package com.collab.docservice.repo;

import com.collab.docservice.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Query("SELECT d FROM Document d LEFT JOIN d.collaborators c " +
            "WHERE d.ownerUsername = :username OR c = :username " +
            "ORDER BY d.updatedAt DESC")
    List<Document> findVisibleDocuments(@Param("username") String username);

    List<Document> findByOwnerUsername(String ownerUsername);
}
