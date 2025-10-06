package com.example.multitenantdocumentsearch.repository;

import com.example.multitenantdocumentsearch.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for Document entity with multi-tenant and text search support.
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {
    /**
     * Case-insensitive search for documents by tenant and query string in title or content.
     * Uses case-insensitive LIKE for full-text search (portable JPQL).
     */
    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(d.content) LIKE LOWER(CONCAT('%', :query, '%')))" )
    List<Document> searchDocuments(@Param("tenantId") String tenantId, @Param("query") String query);

    /**
     * Find a document by id and tenantId (multi-tenancy enforcement).
     */
    Document findByIdAndTenantId(Long id, String tenantId);

    /**
     * Delete a document by id and tenantId (multi-tenancy enforcement).
     */
    void deleteByIdAndTenantId(Long id, String tenantId);
}
