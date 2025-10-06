package com.example.multitenantdocumentsearch.repository;

import com.example.multitenantdocumentsearch.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for Document entity with multi-tenant and text search support.
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {
    /**
     * Case-insensitive search for documents by tenant and query string in title or content.
     * Uses PostgreSQL ILIKE for full-text search.
     */
    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND (d.title ILIKE %:query% OR d.content ILIKE %:query%)")
    Page<Document> searchByTenantAndText(@Param("tenantId") String tenantId, @Param("query") String query, Pageable pageable);

    /**
     * Find a document by id and tenantId (multi-tenancy enforcement).
     */
    Document findByIdAndTenantId(Long id, String tenantId);

    /**
     * Delete a document by id and tenantId (multi-tenancy enforcement).
     */
    void deleteByIdAndTenantId(Long id, String tenantId);
}

