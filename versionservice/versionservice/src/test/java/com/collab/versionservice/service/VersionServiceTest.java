package com.collab.versionservice.service;

import com.collab.versionservice.model.Version;
import com.collab.versionservice.repo.VersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VersionServiceTest {

    private VersionRepository repo;
    private VersionService service;

    @BeforeEach
    void setup() {
        repo = mock(VersionRepository.class);
        service = new VersionService();

        // inject mock repo (field injection in your service)
        try {
            var f = VersionService.class.getDeclaredField("repository");
            f.setAccessible(true);
            f.set(service, repo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void saveSnapshot_setsCreatedAt_andSaves() {
        Version v = new Version();
        v.setDocumentId(UUID.randomUUID());
        v.setAuthorUsername("z");
        v.setContent("hello");

        when(repo.save(any(Version.class))).thenAnswer(inv -> inv.getArgument(0));

        Version out = service.saveSnapshot(v);

        assertNotNull(out.getCreatedAt());
        verify(repo).save(v);
    }

    @Test
    void getHistory_delegatesToRepo() {
        UUID docId = UUID.randomUUID();
        when(repo.findByDocumentIdOrderByCreatedAtDesc(docId)).thenReturn(List.of());

        service.getHistory(docId);

        verify(repo).findByDocumentIdOrderByCreatedAtDesc(docId);
    }

    @Test
    void getVersion_found_returnsVersion() {
        Version v = new Version();
        v.setId(10L);

        when(repo.findById(10L)).thenReturn(Optional.of(v));

        Version out = service.getVersion(10L);

        assertEquals(10L, out.getId());
    }

    @Test
    void getVersion_notFound_throws() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getVersion(99L));
        assertTrue(ex.getMessage().contains("Version history not found"));
    }
}
