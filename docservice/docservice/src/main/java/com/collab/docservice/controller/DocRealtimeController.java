package com.collab.docservice.controller;

import com.collab.docservice.dto.DocEditMessage;
import com.collab.docservice.dto.VersionSnapshotRequest;
import com.collab.docservice.repo.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class DocRealtimeController {

    private final DocumentRepository repo;
    private final RestTemplate restTemplate;

    @Value("${services.versionservice.baseUrl:http://localhost:8083}")
    private String versionserviceBaseUrl;

    // Tracks active users per document ID
    private final Map<UUID, Set<String>> activeUsers = new ConcurrentHashMap<>();

    public DocRealtimeController(DocumentRepository repo, RestTemplate restTemplate) {
        this.repo = repo;
        this.restTemplate = restTemplate;
    }

    @Transactional
    @MessageMapping("/edit/{docId}")
    @SendTo("/topic/doc/{docId}")
    public DocEditMessage streamEdit(@DestinationVariable UUID docId, @Payload DocEditMessage message) {

        switch (message.type) {
            case "JOIN" -> {
                activeUsers.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet()).add(message.sender);
                message.content = String.join(",", activeUsers.get(docId));
            }

            case "LEAVE" -> {
                if (activeUsers.containsKey(docId)) {
                    activeUsers.get(docId).remove(message.sender);
                    message.content = String.join(",", activeUsers.get(docId));
                }
            }

            case "EDIT" -> {
                repo.findById(docId).ifPresent(doc -> {
                    doc.setContent(message.content == null ? "" : message.content);
                    repo.save(doc);

                    // Save version snapshot (tracks contributions)
                    saveVersionSnapshotSafe(docId, message.sender, doc.getContent());
                });
            }

            case "CURSOR" -> {
                // Cursor updates are broadcast only, not persisted
            }
        }

        return message;
    }

    private void saveVersionSnapshotSafe(UUID docId, String authorUsername, String content) {
        try {
            VersionSnapshotRequest payload =
                    new VersionSnapshotRequest(docId, authorUsername, content == null ? "" : content);

            restTemplate.postForEntity(versionserviceBaseUrl + "/api/versions", payload, Object.class);
        } catch (Exception e) {
            System.err.println("Version snapshot failed: " + e.getMessage());
        }
    }
}
