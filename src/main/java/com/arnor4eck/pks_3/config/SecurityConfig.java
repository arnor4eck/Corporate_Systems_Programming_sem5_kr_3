package com.arnor4eck.pks_3.config;

import com.arnor4eck.pks_3.repository.RefreshSessionRepository;
import com.arnor4eck.pks_3.security.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.core.annotation.Order;
import com.arnor4eck.pks_3.dto.AuthDtos;
import com.arnor4eck.pks_3.service.AuthService;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    @Order(1)
    SecurityFilterChain documentationSecurity(HttpSecurity http, AuthService authService) throws Exception {
        // Browser-native login is scoped to documentation. API calls still require a JWT.
        AuthenticationProvider provider = new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) {
                String login = authentication.getName();
                String password = authentication.getCredentials().toString();
                if (login.isBlank() || login.length() > 50 || password.isBlank() || password.length() > 72)
                    throw new BadCredentialsException("Invalid credentials");
                try {
                    var employee = authService.verifyCredentials(new AuthDtos.Login(login, password));
                    if (employee.isPasswordChangeRequired()) throw new BadCredentialsException("Password change required");
                    return UsernamePasswordAuthenticationToken.authenticated(employee, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name())));
                } catch (org.springframework.web.server.ResponseStatusException exception) {
                    throw new BadCredentialsException("Invalid credentials");
                }
            }
            @Override
            public boolean supports(Class<?> type) { return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type); }
        };
        return http.securityMatcher("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml")
                .authenticationProvider(provider)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
                .httpBasic(basic -> basic.realmName("Cemetery API documentation").authenticationEntryPoint((req, res, ex) -> {
                    res.setHeader("WWW-Authenticate", "Basic realm=\"Cemetery API documentation\"");
                    BearerTokenFilter.error(req, res, 401, "Administrator authentication required");
                }))
                .exceptionHandling(errors -> errors.accessDeniedHandler((req, res, ex) -> BearerTokenFilter.error(req, res, 403, "Insufficient permissions")))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http, TokenService tokens, RefreshSessionRepository sessions) throws Exception {
        return http
                // Credentials are supplied only in Authorization/JSON, never in browser cookies.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/change-password").authenticated()
                        .requestMatchers("/api/employees", "/api/employees/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/*/*/permanent").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("WORKER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/sectors", "/api/sectors/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").hasAnyRole("MANAGER", "ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, ex) -> BearerTokenFilter.error(req, res, 401, "Authentication required"))
                        .accessDeniedHandler((req, res, ex) -> BearerTokenFilter.error(req, res, 403, "Insufficient permissions")))
                .addFilterBefore(new BearerTokenFilter(tokens, sessions), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
