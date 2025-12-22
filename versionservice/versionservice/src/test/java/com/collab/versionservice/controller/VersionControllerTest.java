package com.collab.versionservice.controller;

import com.collab.versionservice.model.Version;
import com.collab.versionservice.repo.VersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VersionController.class)
@AutoConfigureMockMvc(addFilters = false)
class VersionControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    VersionRepository repo;

    @Test
    void save_setsCreatedAt_andConvertsNullContentToEmptyString() throws Exception {
        UUID docId = UUID.randomUUID();

        Version saved = new Version();
        saved.setId(1L);
        saved.setDocumentId(docId);
        saved.setAuthorUsername("z");
        saved.setContent(""); // should become ""
        saved.setCreatedAt(LocalDateTime.now());

        when(repo.save(any(Version.class))).thenReturn(saved);

        mvc.perform(post("/api/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"" + docId + "\",\"authorUsername\":\"z\",\"content\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.documentId").value(docId.toString()))
                .andExpect(jsonPath("$.authorUsername").value("z"))
                .andExpect(jsonPath("$.content").value(""));

        verify(repo).save(any(Version.class));
    }

    @Test
    void history_returnsList() throws Exception {
        UUID docId = UUID.randomUUID();

        Version v1 = new Version();
        v1.setId(1L);
        v1.setDocumentId(docId);
        v1.setAuthorUsername("a");
        v1.setContent("c1");
        v1.setCreatedAt(LocalDateTime.now());

        when(repo.findByDocumentIdOrderByCreatedAtDesc(docId)).thenReturn(List.of(v1));

        mvc.perform(get("/api/versions/doc/" + docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].documentId").value(docId.toString()));
    }

    @Test
    void getOne_returnsVersion() throws Exception {
        UUID docId = UUID.randomUUID();

        Version v = new Version();
        v.setId(10L);
        v.setDocumentId(docId);
        v.setAuthorUsername("z");
        v.setContent("hello");
        v.setCreatedAt(LocalDateTime.now());

        when(repo.findById(10L)).thenReturn(Optional.of(v));

        mvc.perform(get("/api/versions/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.content").value("hello"));
    }

    @Test
    void deleteHistory_callsRepoDelete() throws Exception {
        UUID docId = UUID.randomUUID();
        doNothing().when(repo).deleteByDocumentId(docId);

        mvc.perform(delete("/api/versions/doc/" + docId))
                .andExpect(status().isOk());

        verify(repo).deleteByDocumentId(docId);
    }
}
