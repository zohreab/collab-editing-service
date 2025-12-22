package com.collab.docservice.controller;

import com.collab.docservice.dto.*;
import com.collab.docservice.model.Document;
import com.collab.docservice.repo.DocumentRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.collab.docservice.dto.VersionResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import java.util.*;

@RestController
@RequestMapping("/docs")
public class DocController {

    private final DocumentRepository repo;
    private final RestTemplate restTemplate;

    @Value("${services.userservice.baseUrl:http://localhost:8081}")
    private String userserviceBaseUrl;

    @Value("${services.versionservice.baseUrl:http://localhost:8083}")
    private String versionserviceBaseUrl;

    public DocController(DocumentRepository repo, RestTemplate restTemplate) {
        this.repo = repo;
        this.restTemplate = restTemplate;
    }

    private String requireUser(HttpServletRequest request) {
        String user = request.getHeader("X-User");
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User");
        }
        return user;
    }

    private Document getDocWithPermission(UUID id, String username) {
        Document doc = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doc not found"));

        boolean isOwner = doc.getOwnerUsername().equals(username);
        boolean isCollaborator = doc.getCollaborators().contains(username);

        if (!isOwner && !isCollaborator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return doc;
    }

    @GetMapping("/{id}")
    public DocResponse getOne(@PathVariable UUID id, HttpServletRequest request) {
        String username = requireUser(request);
        Document d = getDocWithPermission(id, username);
        return new DocResponse(d.getId(), d.getTitle(), d.getContent(), d.getOwnerUsername(),
                new ArrayList<>(d.getCollaborators()), d.getCreatedAt(), d.getUpdatedAt());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocResponse create(@Valid @RequestBody CreateDocRequest req, HttpServletRequest request) {
        String username = requireUser(request);

        Document d = new Document();
        d.setTitle(req.title);
        d.setContent(req.content == null ? "" : req.content);
        d.setOwnerUsername(username);

        d = repo.save(d);

        // Create initial snapshot in version history
        saveVersionSnapshotSafe(d.getId(), username, d.getContent());

        return new DocResponse(d.getId(), d.getTitle(), d.getContent(), d.getOwnerUsername(),
                new ArrayList<>(d.getCollaborators()), d.getCreatedAt(), d.getUpdatedAt());
    }

    @GetMapping
    public List<DocResponse> listMine(HttpServletRequest request) {
        String username = requireUser(request);
        return repo.findVisibleDocuments(username).stream()
                .map(d -> new DocResponse(d.getId(), d.getTitle(), "", d.getOwnerUsername(),
                        new ArrayList<>(d.getCollaborators()), d.getCreatedAt(), d.getUpdatedAt()))
                .toList();
    }

    @PutMapping("/{id}")
    public DocResponse update(@PathVariable UUID id, @RequestBody CreateDocRequest req, HttpServletRequest request) {
        String username = requireUser(request);

        Document doc = getDocWithPermission(id, username);
        doc.setTitle(req.title);
        doc.setContent(req.content == null ? "" : req.content);

        doc = repo.save(doc);


        saveVersionSnapshotSafe(doc.getId(), username, doc.getContent());

        return new DocResponse(doc.getId(), doc.getTitle(), doc.getContent(), doc.getOwnerUsername(),
                new ArrayList<>(doc.getCollaborators()), doc.getCreatedAt(), doc.getUpdatedAt());
    }

    @PostMapping("/{id}/share")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void share(@PathVariable UUID id, @Valid @RequestBody ShareRequest req, HttpServletRequest request) {
        String username = requireUser(request);
        Document doc = getDocWithPermission(id, username);

        if (!doc.getOwnerUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can share this document");
        }

        if (req.collaboratorUsername.equals(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot share a document with yourself");
        }

        try {
            ResponseEntity<Boolean> response = restTemplate.getForEntity(
                    userserviceBaseUrl + "/users/exists/" + req.collaboratorUsername,
                    Boolean.class
            );

            if (response.getBody() == null || !response.getBody()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User '" + req.collaboratorUsername + "' does not exist");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "User service check failed");
        }

        if (!doc.getCollaborators().contains(req.collaboratorUsername)) {
            doc.getCollaborators().add(req.collaboratorUsername);
            repo.save(doc);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, HttpServletRequest request) {
        String username = requireUser(request);
        Document doc = getDocWithPermission(id, username);

        if (!doc.getOwnerUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can delete");
        }

        // Cleanup versions (best-effort)
        try {
            restTemplate.delete(versionserviceBaseUrl + "/api/versions/doc/" + id);
        } catch (Exception e) {
            System.err.println("Version cleanup failed: " + e.getMessage());
        }

        repo.delete(doc);
    }

    @DeleteMapping("/{id}/share/{collaborator}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeAccess(@PathVariable UUID id, @PathVariable String collaborator, HttpServletRequest request) {
        String username = requireUser(request);
        Document doc = getDocWithPermission(id, username);

        if (!doc.getOwnerUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can revoke access");
        }

        if (doc.getCollaborators().contains(collaborator)) {
            doc.getCollaborators().remove(collaborator);
            repo.save(doc);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a collaborator");
        }
    }

    @GetMapping("/{id}/versions")
    public List<VersionResponse> versions(@PathVariable UUID id, HttpServletRequest request) {
        String username = requireUser(request);

        // Permission check: owner or collaborator
        getDocWithPermission(id, username);

        try {
            ResponseEntity<List<VersionResponse>> response =
                    restTemplate.exchange(
                            versionserviceBaseUrl + "/api/versions/doc/" + id,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<List<VersionResponse>>() {}
                    );

            return response.getBody() == null ? List.of() : response.getBody();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Version service unavailable");
        }
    }

    @GetMapping("/{id}/versions/{versionId}")
    public VersionResponse versionById(
            @PathVariable UUID id,
            @PathVariable Long versionId,
            HttpServletRequest request
    ) {
        String username = requireUser(request);

        // Permission check: owner or collaborator
        getDocWithPermission(id, username);

        try {
            return restTemplate.getForObject(
                    versionserviceBaseUrl + "/api/versions/" + versionId,
                    VersionResponse.class
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Version service unavailable");
        }
    }



    private void saveVersionSnapshotSafe(UUID docId, String authorUsername, String content) {
        try {
            VersionSnapshotRequest payload =
                    new VersionSnapshotRequest(docId, authorUsername, content == null ? "" : content);

            restTemplate.postForEntity(versionserviceBaseUrl + "/api/versions", payload, Object.class);
        } catch (Exception e) {
            // Do not break doc editing if version service is down
            System.err.println("Version snapshot failed: " + e.getMessage());
        }
    }
}
