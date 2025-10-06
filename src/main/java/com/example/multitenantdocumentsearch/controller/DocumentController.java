package com.example.multitenantdocumentsearch.controller;

import com.example.multitenantdocumentsearch.config.TenantContext;
import com.example.multitenantdocumentsearch.entity.Document;
import com.example.multitenantdocumentsearch.service.DocumentService;
import com.example.multitenantdocumentsearch.service.RateLimiterService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Optional;

/**
 * REST API controller for document CRUD and search operations.
 * Enforces multi-tenancy, rate limiting, and structured error responses.
 */
@RestController
@RequestMapping
@Validated
public class DocumentController {
    @Autowired
    private DocumentService documentService;
    @Autowired
    private RateLimiterService rateLimiterService;

    /**
     * Creates a new document for the current tenant.
     * Requires X-Tenant-Id header.
     */
    @PostMapping("/documents")
    public ResponseEntity<?> createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        enforceRateLimit();
        Document doc = documentService.createDocument(request.getTitle(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    /**
     * Searches documents for the current tenant with paging and caching.
     * Example: GET /search?q=foo&page=0&size=10&tenant=tenant1
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchDocuments(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        enforceRateLimit();
        Page<Document> results = documentService.searchDocuments(query, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Retrieves a document by id for the current tenant.
     */
    @GetMapping("/documents/{id}")
    public ResponseEntity<?> getDocument(@PathVariable Long id) {
        enforceRateLimit();
        Optional<Document> doc = documentService.getDocument(id);
        return doc.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Document not found for tenant")));
    }

    /**
     * Deletes a document by id for the current tenant.
     */
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        enforceRateLimit();
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Enforces per-tenant rate limiting. Returns 429 if exceeded.
     */
    private void enforceRateLimit() {
        String tenantId = TenantContext.getTenantId();
        if (!rateLimiterService.isAllowed(tenantId)) {
            throw new RateLimitExceededException("Rate limit exceeded for tenant: " + tenantId);
        }
    }

    /**
     * Request body for creating a document.
     */
    @Data
    public static class CreateDocumentRequest {
        @NotBlank
        @Size(max = 255)
        private String title;
        @NotBlank
        private String content;
    }

    /**
     * Error response structure.
     */
    @Data
    public static class ErrorResponse {
        private final String error;
    }

    /**
     * Exception for rate limit exceeded.
     */
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) { super(message); }
    }
}

