package com.collab.versionservice;

import com.collab.versionservice.model.Version;
import com.collab.versionservice.repo.VersionRepository;
import com.collab.versionservice.service.VersionService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class VersionServiceTest {

    @Mock
    private VersionRepository repo;

    @InjectMocks
    private VersionService service;

    @Test
    void testOperationSave() {
        Version v = new Version();
        v.setContent("v1");
        when(repo.save(any())).thenReturn(v);
        assertNotNull(service.saveSnapshot(v));
    }

    @Test
    void testOperationGet() {
        Version v = new Version();
        when(repo.findById(1L)).thenReturn(Optional.of(v));
        assertNotNull(service.getVersion(1L));
    }
}