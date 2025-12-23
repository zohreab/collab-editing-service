package com.collab.docservice.controller;

import com.collab.docservice.dto.*;
import com.collab.docservice.model.Document;
import com.collab.docservice.repo.DocumentRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

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

    @Value("${internal.secret}")
    private String internalSecret;

    public DocController(DocumentRepository repo, RestTemplate restTemplate) {
        this.repo = repo;
        this.restTemplate = restTemplate;
    }

    /* -----------------------------
       Auth helper (gateway sets X-User)
    ------------------------------ */
    private String requireUser(HttpServletRequest request) {
        String user = request.getHeader("X-User");
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User");
        }
        return user;
    }

    /* -----------------------------
       Permission helper
    ------------------------------ */
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

    /* -----------------------------
       Get one doc
    ------------------------------ */
    @GetMapping("/{id}")
    public DocResponse getOne(@PathVariable UUID id, HttpServletRequest request) {
        String username = requireUser(request);
        Document d = getDocWithPermission(id, username);

        return new DocResponse(
                d.getId(),
                d.getTitle(),
                d.getContent(),
                d.getOwnerUsername(),
                new ArrayList<>(d.getCollaborators()),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }

    /* -----------------------------
       Create doc
    ------------------------------ */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocResponse create(@Valid @RequestBody CreateDocRequest req, HttpServletRequest request) {
        String username = requireUser(request);

        Document d = new Document();
        d.setTitle(req.title);
        d.setContent(req.content == null ? "" : req.content);
        d.setOwnerUsername(username);

        d = repo.save(d);

        // ✅ Keep or remove this depending on your preference:
        // If you want Version 1 when doc is created, keep it.
        saveVersionSnapshotSafe(d.getId(), username, d.getContent());

        return new DocResponse(
                d.getId(),
                d.getTitle(),
                d.getContent(),
                d.getOwnerUsername(),
                new ArrayList<>(d.getCollaborators()),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }

    /* -----------------------------
       List visible docs (owned OR shared)
    ------------------------------ */
    @GetMapping
    public List<DocResponse> listMine(HttpServletRequest request) {
        String username = requireUser(request);

        return repo.findVisibleDocuments(username).stream()
                .map(d -> new DocResponse(
                        d.getId(),
                        d.getTitle(),
                        "", // do not send content in list
                        d.getOwnerUsername(),
                        new ArrayList<>(d.getCollaborators()),
                        d.getCreatedAt(),
                        d.getUpdatedAt()
                ))
                .toList();
    }

    /* -----------------------------
       Update doc content/title
       IMPORTANT: no snapshots here anymore
    ------------------------------ */
    @PutMapping("/{id}")
    public DocResponse update(@PathVariable UUID id,
                              @RequestBody CreateDocRequest req,
                              HttpServletRequest request) {
        String username = requireUser(request);

        Document doc = getDocWithPermission(id, username);
        doc.setTitle(req.title);
        doc.setContent(req.content == null ? "" : req.content);

        doc = repo.save(doc);

        // ❌ DO NOT snapshot here (prevents version-per-keystroke)
        return new DocResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getContent(),
                doc.getOwnerUsername(),
                new ArrayList<>(doc.getCollaborators()),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }

    /* -----------------------------
       Share doc (owner only)
    ------------------------------ */
    @PostMapping("/{id}/share")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void share(@PathVariable UUID id,
                      @Valid @RequestBody ShareRequest req,
                      HttpServletRequest request) {
        String username = requireUser(request);
        Document doc = getDocWithPermission(id, username);

        if (!doc.getOwnerUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can share this document");
        }

        if (req.collaboratorUsername.equals(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot share a document with yourself");
        }

        // check user exists in userservice
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
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "User service check failed");
        }

        if (!doc.getCollaborators().contains(req.collaboratorUsername)) {
            doc.getCollaborators().add(req.collaboratorUsername);
            repo.save(doc);
        }
    }

    /* -----------------------------
       Delete doc (owner only)
    ------------------------------ */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, HttpServletRequest request) {
        String username = requireUser(request);
        Document doc = getDocWithPermission(id, username);

        if (!doc.getOwnerUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can delete");
        }

        // cleanup versions (best-effort)
        try {
            restTemplate.delete(versionserviceBaseUrl + "/api/versions/doc/" + id);
        } catch (Exception e) {
            System.err.println("Version cleanup failed: " + e.getMessage());
        }

        repo.delete(doc);
    }

    /* -----------------------------
       Revoke access (owner only)
    ------------------------------ */
    @DeleteMapping("/{id}/share/{collaborator}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeAccess(@PathVariable UUID id,
                             @PathVariable String collaborator,
                             HttpServletRequest request) {
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

    /* -----------------------------
       Versions proxy
    ------------------------------ */
    @GetMapping("/{id}/versions")
    public List<VersionResponse> versions(@PathVariable UUID id, HttpServletRequest request) {
        String username = requireUser(request);

        // permission check
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
    public VersionResponse versionById(@PathVariable UUID id,
                                       @PathVariable Long versionId,
                                       HttpServletRequest request) {
        String username = requireUser(request);

        // permission check
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

    /* -----------------------------
       INTERNAL: delete all docs owned by a username
       Used by userservice when deleting account
    ------------------------------ */
    @DeleteMapping("/internal/owner/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllDocsOwnedBy(@PathVariable String username,
                                     @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        if (secret == null || !secret.equals(internalSecret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        List<Document> owned = repo.findByOwnerUsername(username);

        // cleanup versions (best-effort)
        for (Document d : owned) {
            try {
                restTemplate.delete(versionserviceBaseUrl + "/api/versions/doc/" + d.getId());
            } catch (Exception e) {
                System.err.println("Version cleanup failed for doc " + d.getId() + ": " + e.getMessage());
            }
        }

        repo.deleteAll(owned);
    }

    /* -----------------------------
       Manual snapshot (this is what you want!)
    ------------------------------ */
    @PostMapping("/{id}/snapshot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveSnapshot(@PathVariable UUID id, HttpServletRequest request) {
        String username = requireUser(request);

        Document doc = getDocWithPermission(id, username);

        // ✅ Only save versions when user explicitly triggers snapshot
        saveVersionSnapshotSafe(doc.getId(), username, doc.getContent());
    }

    /* -----------------------------
       Snapshot helper
    ------------------------------ */
    private void saveVersionSnapshotSafe(UUID docId, String authorUsername, String content) {

        // DEBUG: tells you WHO called snapshot
        System.out.println("🔥 SNAPSHOT TRIGGERED in DocService (DocController.saveVersionSnapshotSafe)");
        new Exception("Snapshot stacktrace").printStackTrace();

        try {
            VersionSnapshotRequest payload = new VersionSnapshotRequest(
                    docId,
                    authorUsername,
                    content == null ? "" : content
            );

            restTemplate.postForEntity(versionserviceBaseUrl + "/api/versions", payload, Object.class);
        } catch (Exception e) {
            System.err.println("Version snapshot failed: " + e.getMessage());
        }
    }
}
