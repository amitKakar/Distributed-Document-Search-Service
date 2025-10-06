package com.example.multitenantdocumentsearch.service;

import com.example.multitenantdocumentsearch.config.TenantContext;
import com.example.multitenantdocumentsearch.entity.Document;
import com.example.multitenantdocumentsearch.repository.DocumentRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service layer for document operations, multi-tenancy enforcement, and caching.
 */
@Service
public class DocumentService {
    @Autowired
    private DocumentRepository documentRepository;

    // In-memory cache for search results: key = tenantId:query:page:size
    private final Cache<String, Page<Document>> searchCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    /**
     * Creates a new document for the current tenant.
     * Clears the search cache for this tenant.
     */
    @Transactional
    public Document createDocument(String title, String content) {
        String tenantId = TenantContext.getTenantId();
        Document doc = Document.builder()
                .tenantId(tenantId)
                .title(title)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        Document saved = documentRepository.save(doc);
        clearTenantCache(tenantId);
        return saved;
    }

    /**
     * Searches documents for the current tenant with caching.
     */
    public Page<Document> searchDocuments(String query, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        String cacheKey = buildCacheKey(tenantId, query, page, size);
        return searchCache.get(cacheKey, k -> {
            Pageable pageable = PageRequest.of(page, size);
            return documentRepository.searchByTenantAndText(tenantId, query, pageable);
        });
    }

    /**
     * Retrieves a document by id for the current tenant.
     */
    public Optional<Document> getDocument(Long id) {
        String tenantId = TenantContext.getTenantId();
        return Optional.ofNullable(documentRepository.findByIdAndTenantId(id, tenantId));
    }

    /**
     * Deletes a document by id for the current tenant and clears cache.
     */
    @Transactional
    public void deleteDocument(Long id) {
        String tenantId = TenantContext.getTenantId();
        documentRepository.deleteByIdAndTenantId(id, tenantId);
        clearTenantCache(tenantId);
    }

    /**
     * Builds a cache key for search results.
     */
    private String buildCacheKey(String tenantId, String query, int page, int size) {
        return tenantId + ":" + query + ":" + page + ":" + size;
    }

    /**
     * Clears all cached search results for a tenant.
     */
    private void clearTenantCache(String tenantId) {
        searchCache.asMap().keySet().removeIf(key -> key.startsWith(tenantId + ":"));
    }
}

