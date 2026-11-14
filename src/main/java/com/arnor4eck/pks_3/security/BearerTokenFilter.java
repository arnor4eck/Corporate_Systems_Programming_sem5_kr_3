package com.arnor4eck.pks_3.security;

import com.arnor4eck.pks_3.domain.Employee;
import com.arnor4eck.pks_3.repository.RefreshSessionRepository;
import com.arnor4eck.pks_3.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class BearerTokenFilter extends OncePerRequestFilter {
    private final TokenService tokens;
    private final RefreshSessionRepository sessions;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/api/auth/")) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
        }
        String header = request.getHeader("Authorization");
        // Browser credentials for Swagger must not replace JSON login or become API authentication.
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var jwt = tokens.decode(header.substring(7));
                var session = sessions.findById(UUID.fromString(jwt.getClaimAsString("sid"))).orElseThrow();
                if (!AuthService.valid(session) || !session.getEmployee().getId().toString().equals(jwt.getSubject()))
                    throw new IllegalArgumentException();
                var employee = session.getEmployee();
                var auth = UsernamePasswordAuthenticationToken.authenticated(employee, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (org.springframework.security.oauth2.jwt.JwtException | IllegalArgumentException | java.util.NoSuchElementException exception) {
                SecurityContextHolder.clearContext();
                error(request, response, 401, "Invalid or expired access token");
                return;
            }
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Employee employee && employee.isPasswordChangeRequired()
                && !List.of("/api/auth/me", "/api/auth/change-password", "/api/auth/logout").contains(path)) {
            error(request, response, 403, "Password change required");
            return;
        }
        chain.doFilter(request, response);
    }

    public static void error(HttpServletRequest request, HttpServletResponse response, int status, String detail) throws IOException {
        com.arnor4eck.pks_3.error.ApiProblems.write(request, response, status, detail);
    }
}
