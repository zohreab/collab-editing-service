package com.collab.docservice.controller;

import com.collab.docservice.dto.CreateDocRequest;
import com.collab.docservice.dto.ShareRequest;
import com.collab.docservice.model.Document;
import com.collab.docservice.repo.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.mockito.ArgumentCaptor;
import com.collab.docservice.dto.VersionResponse;
import org.springframework.core.ParameterizedTypeReference;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;


import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DocController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    DocumentRepository repo;

    @MockBean
    RestTemplate restTemplate;

    // ---------- Helpers ----------
    private static Document doc(UUID id, String owner, String title, String content, String... collaborators) {
        Document d = new Document();
        d.setId(id);
        d.setOwnerUsername(owner);
        d.setTitle(title);
        d.setContent(content);
        d.setCollaborators(new HashSet<>(Arrays.asList(collaborators)));
        // createdAt/updatedAt are set by JPA callbacks normally; not required for these tests
        return d;
    }

    // ---------- Create ----------

    @Test
    void create_missingXUser_returns401() throws Exception {
        mvc.perform(post("/docs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withXUser_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        Document saved = doc(id, "z", "t", "c");

        when(repo.save(any(Document.class))).thenReturn(saved);

        mvc.perform(post("/docs")
                        .header("X-User", "z")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.ownerUsername").value("z"))
                .andExpect(jsonPath("$.title").value("t"))
                .andExpect(jsonPath("$.content").value("c"));
    }

    // ---------- Get One + Permission ----------

    @Test
    void getOne_ownerCanRead_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Document d = doc(id, "owner", "t", "hello", "collab1");

        when(repo.findById(id)).thenReturn(Optional.of(d));

        mvc.perform(get("/docs/" + id)
                        .header("X-User", "owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerUsername").value("owner"))
                .andExpect(jsonPath("$.content").value("hello"));
    }

    @Test
    void getOne_collaboratorCanRead_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Document d = doc(id, "owner", "t", "hello", "alice", "bob");

        when(repo.findById(id)).thenReturn(Optional.of(d));

        mvc.perform(get("/docs/" + id)
                        .header("X-User", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerUsername").value("owner"));
    }

    @Test
    void getOne_notOwnerNorCollaborator_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        Document d = doc(id, "owner", "t", "hello", "alice");

        when(repo.findById(id)).thenReturn(Optional.of(d));

        mvc.perform(get("/docs/" + id)
                        .header("X-User", "mallory"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOne_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        mvc.perform(get("/docs/" + id)
                        .header("X-User", "owner"))
                .andExpect(status().isNotFound());
    }

    // ---------- List Mine ----------

    @Test
    void listMine_requiresXUser() throws Exception {
        mvc.perform(get("/docs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listMine_returnsDocsVisibleToUser() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Document d1 = doc(id1, "z", "doc1", "content1");
        Document d2 = doc(id2, "other", "doc2", "content2", "z");

        when(repo.findVisibleDocuments("z")).thenReturn(List.of(d1, d2));

        mvc.perform(get("/docs").header("X-User", "z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(id1.toString()))
                .andExpect(jsonPath("$[1].id").value(id2.toString()))
                // listMine intentionally returns content="" per your code
                .andExpect(jsonPath("$[0].content").value(""));
    }

    // ---------- Update ----------

    @Test
    void update_requiresPermission_ownerCanUpdate() throws Exception {
        UUID id = UUID.randomUUID();
        Document existing = doc(id, "owner", "old", "oldcontent");
        Document saved = doc(id, "owner", "new", "newcontent");

        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(Document.class))).thenReturn(saved);

        mvc.perform(put("/docs/" + id)
                        .header("X-User", "owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"new\",\"content\":\"newcontent\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("new"))
                .andExpect(jsonPath("$.content").value("newcontent"));
    }

    @Test
    void update_notAllowed_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        Document existing = doc(id, "owner", "old", "oldcontent");

        when(repo.findById(id)).thenReturn(Optional.of(existing));

        mvc.perform(put("/docs/" + id)
                        .header("X-User", "intruder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"new\",\"content\":\"newcontent\"}"))
                .andExpect(status().isForbidden());

        verify(repo, never()).save(any());
    }

    // ---------- Share ----------

    @Test
    void share_onlyOwnerCanShare_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        Document existing = doc(id, "owner", "t", "c", "alice");

        when(repo.findById(id)).thenReturn(Optional.of(existing));

        mvc.perform(post("/docs/" + id + "/share")
                        .header("X-User", "alice") // collaborator tries to share
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collaboratorUsername\":\"bob\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void share_cannotShareWithSelf_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        Document existing = doc(id, "owner", "t", "c");

        when(repo.findById(id)).thenReturn(Optional.of(existing));

        mvc.perform(post("/docs/" + id + "/share")
                        .header("X-User", "owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collaboratorUsername\":\"owner\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void share_userDoesNotExist_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        Document existing = doc(id, "owner", "t", "c");

        when(repo.findById(id)).thenReturn(Optional.of(existing));

        when(restTemplate.getForEntity(contains("/users/exists/bob"), eq(Boolean.class)))
                .thenReturn(new ResponseEntity<>(false, HttpStatus.OK));

        mvc.perform(post("/docs/" + id + "/share")
                        .header("X-User", "owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collaboratorUsername\":\"bob\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void share_userExists_addsCollaborator_andReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        Document existing = doc(id, "owner", "t", "c"); // no collaborators yet

        when(repo.findById(id)).thenReturn(Optional.of(existing));

        when(restTemplate.getForEntity(contains("/users/exists/alice"), eq(Boolean.class)))
                .thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        when(repo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/docs/" + id + "/share")
                        .header("X-User", "owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collaboratorUsername\":\"alice\"}"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(repo).save(captor.capture());

        Document savedDoc = captor.getValue();
        assertTrue(savedDoc.getCollaborators().contains("alice"));

    }

    // ---------- Revoke Access ----------

    @Test
    void revoke_onlyOwner_canRemoveCollaborator() throws Exception {
        UUID id = UUID.randomUUID();
        Document existing = doc(id, "owner", "t", "c", "alice");

        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(delete("/docs/" + id + "/share/alice")
                        .header("X-User", "owner"))
                .andExpect(status().isNoContent());

        assertFalse(existing.getCollaborators().contains("alice"));
        verify(repo).save(existing);
    }

    @Test
    void revoke_userNotCollaborator_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        Document existing = doc(id, "owner", "t", "c"); // no collaborators

        when(repo.findById(id)).thenReturn(Optional.of(existing));

        mvc.perform(delete("/docs/" + id + "/share/alice")
                        .header("X-User", "owner"))
                .andExpect(status().isNotFound());
    }

    // ---------- Delete ----------

    @Test
    void delete_onlyOwner_canDelete_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        Document existing = doc(id, "owner", "t", "c");

        when(repo.findById(id)).thenReturn(Optional.of(existing));
        doNothing().when(repo).delete(existing);
        doNothing().when(restTemplate).delete(anyString());

        mvc.perform(delete("/docs/" + id)
                        .header("X-User", "owner"))
                .andExpect(status().isNoContent());

        verify(repo).delete(existing);
        verify(restTemplate).delete(contains("/api/versions/doc/" + id));
    }

    @Test
    void delete_notOwner_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        Document existing = doc(id, "owner", "t", "c", "alice");

        when(repo.findById(id)).thenReturn(Optional.of(existing));

        mvc.perform(delete("/docs/" + id)
                        .header("X-User", "alice"))
                .andExpect(status().isForbidden());

        verify(repo, never()).delete(any());
    }

    // ---------- Version Service Endpoints ----------

    @Test
    void versions_returnsListFromVersionService() throws Exception {
        UUID id = UUID.randomUUID();
        Document d = doc(id, "owner", "title", "content");
        when(repo.findById(id)).thenReturn(Optional.of(d));

        VersionResponse v1 = new VersionResponse();
        v1.id = 1L;
        v1.content = "v1 content";

        // Fix: Explicitly define the ResponseEntity to help Mockito with types
        ResponseEntity<List<VersionResponse>> responseEntity = new ResponseEntity<>(List.of(v1), HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        mvc.perform(get("/docs/" + id + "/versions").header("X-User", "owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("v1 content"));
    }

    @Test
    void versionById_returnsSingleVersion() throws Exception {
        UUID id = UUID.randomUUID();
        Document d = doc(id, "owner", "title", "content");
        when(repo.findById(id)).thenReturn(Optional.of(d));

        VersionResponse v = new VersionResponse();
        v.id = 100L;
        v.content = "specific version";

        when(restTemplate.getForObject(contains("/api/versions/100"), eq(VersionResponse.class)))
                .thenReturn(v);

        mvc.perform(get("/docs/" + id + "/versions/100").header("X-User", "owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.content").value("specific version"));
    }

    @Test
    void share_userServiceDown_returns503() throws Exception {
        UUID id = UUID.randomUUID();
        Document existing = doc(id, "owner", "t", "c");
        when(repo.findById(id)).thenReturn(Optional.of(existing));

        // Simulate service connection failure
        when(restTemplate.getForEntity(anyString(), eq(Boolean.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        mvc.perform(post("/docs/" + id + "/share")
                        .header("X-User", "owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collaboratorUsername\":\"alice\"}"))
                .andExpect(status().isServiceUnavailable());
    }
}
