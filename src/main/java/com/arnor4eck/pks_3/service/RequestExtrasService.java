package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.domain.*;
import com.arnor4eck.pks_3.repository.*;
import com.arnor4eck.pks_3.dto.*;
import com.arnor4eck.pks_3.dto.RequestExtras.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class RequestExtrasService {
    private final RequestRepository requests;
    private final TagRepository tags;
    private final RequestCommentRepository comments;
    private final EmployeeRepository employees;
    private final RequestService requestService;
    private final AuditService audit;

    public PageResponse<TagResponse> tags(int page, int size) {
        var result = tags.findAll(PageRequest.of(page, size, Sort.by("name").and(Sort.by("id"))));
        return new PageResponse<>(result.stream().map(t -> new TagResponse(t.getId(), t.getName())).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }
    @Transactional public TagResponse createTag(TagInput input) {
        var tag = new Tag(); tag.setName(input.name().strip()); tags.saveAndFlush(tag);
        audit.record("CREATE", "tags", tag.getId(), "Request label created");
        return new TagResponse(tag.getId(), tag.getName());
    }
    @Transactional public RequestResponse changeTag(UUID requestId, TagChange input, boolean add) {
        var request = request(requestId); Versions.require(request.getVersion(), input.version());
        var tag = tags.findById(input.tagId()).orElseThrow(() -> missing("Tag"));
        if (add) request.getTags().add(tag); else request.getTags().removeIf(t -> t.getId().equals(tag.getId()));
        requests.flush(); audit.record(add ? "ADD_TAG" : "REMOVE_TAG", "requests", requestId, "Request label " + tag.getId());
        return requestService.response(request);
    }
    public PageResponse<Comment> comments(UUID requestId, int page, int size) {
        request(requestId);
        var result = comments.findByRequestId(requestId, PageRequest.of(page, size, Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdAt"), Sort.Order.asc("id"))));
        return new PageResponse<>(result.stream().map(this::response).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }
    @Transactional public Comment createComment(UUID id, CommentInput input) {
        var comment = new RequestComment(); comment.setRequest(request(id)); comment.setAuthor(actor());
        comment.setContent(input.content()); comment.setPinned(Boolean.TRUE.equals(input.pinned())); comments.saveAndFlush(comment);
        audit.record("COMMENT_CREATE", "requests", id, "Service comment " + comment.getId());
        return response(comment);
    }
    @Transactional public Comment updateComment(UUID requestId, UUID id, CommentInput input) {
        var comment = comment(requestId, id); authorize(comment); Versions.require(comment.getVersion(), input.version());
        comment.setContent(input.content()); comment.setPinned(Boolean.TRUE.equals(input.pinned())); comments.flush();
        audit.record("COMMENT_UPDATE", "requests", requestId, "Service comment " + id);
        return response(comment);
    }
    @Transactional public void deleteComment(UUID requestId, UUID id, Long version) {
        var comment = comment(requestId, id); authorize(comment); Versions.require(comment.getVersion(), version);
        comments.delete(comment); comments.flush(); audit.record("COMMENT_DELETE", "requests", requestId, "Service comment " + id);
    }
    private Employee actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof Employee e) return employees.findById(e.getId()).orElseThrow(() -> missing("Employee"));
        return employees.findByLogin(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "An existing employee account is required"));
    }
    private void authorize(RequestComment comment) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !comment.getAuthor().getId().equals(actor().getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author or administrator can change a comment");
    }
    private RequestComment comment(UUID requestId, UUID id) {
        return comments.findById(id).filter(c -> c.getRequest().getId().equals(requestId)).orElseThrow(() -> missing("Comment"));
    }
    private Request request(UUID id) { return requests.findById(id).orElseThrow(() -> missing("Request")); }
    private ResponseStatusException missing(String entity) { return new ResponseStatusException(HttpStatus.NOT_FOUND, entity + " not found"); }
    private Comment response(RequestComment c) { return new Comment(c.getId(), c.getRequest().getId(), c.getAuthor().getId(), c.getAuthor().getFullName(), c.getContent(), c.isPinned(), c.getCreatedAt(), c.getUpdatedAt(), c.getVersion()); }
}
