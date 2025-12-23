package com.collab.docservice.controller;

import com.collab.docservice.dto.DocEditMessage;
import com.collab.docservice.model.Document;
import com.collab.docservice.repo.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocRealtimeControllerTest {

    @Test
    void join_returnsListOfUsersInContent() {
        DocumentRepository repo = mock(DocumentRepository.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        DocRealtimeController controller = new DocRealtimeController(repo, restTemplate);

        UUID docId = UUID.randomUUID();

        DocEditMessage join1 = new DocEditMessage();
        join1.type = "JOIN";
        join1.sender = "alice";

        DocEditMessage out1 = controller.streamEdit(docId, join1);
        assertTrue(out1.content.contains("alice"));

        DocEditMessage join2 = new DocEditMessage();
        join2.type = "JOIN";
        join2.sender = "bob";

        DocEditMessage out2 = controller.streamEdit(docId, join2);
        assertTrue(out2.content.contains("alice"));
        assertTrue(out2.content.contains("bob"));

        verifyNoInteractions(repo);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void edit_savesContentToDatabase_only() {
        DocumentRepository repo = mock(DocumentRepository.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        DocRealtimeController controller = new DocRealtimeController(repo, restTemplate);

        UUID docId = UUID.randomUUID();
        Document d = new Document();
        d.setId(docId);
        d.setOwnerUsername("owner");
        d.setTitle("t");
        d.setContent("old");

        when(repo.findById(docId)).thenReturn(Optional.of(d));
        when(repo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocEditMessage edit = new DocEditMessage();
        edit.type = "EDIT";
        edit.sender = "alice";
        edit.content = "new content";

        controller.streamEdit(docId, edit);

        assertEquals("new content", d.getContent());
        verify(repo).save(d);


        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(Object.class));
        verifyNoMoreInteractions(restTemplate);
    }

    @Test
    void cursor_doesNotSaveToDatabase_orVersionService() {
        DocumentRepository repo = mock(DocumentRepository.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        DocRealtimeController controller = new DocRealtimeController(repo, restTemplate);

        UUID docId = UUID.randomUUID();

        DocEditMessage cursor = new DocEditMessage();
        cursor.type = "CURSOR";
        cursor.sender = "alice";
        cursor.cursorPosition = 5;

        controller.streamEdit(docId, cursor);

        verifyNoInteractions(repo);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void leave_removesUserFromActiveList() {
        DocumentRepository repo = mock(DocumentRepository.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        DocRealtimeController controller = new DocRealtimeController(repo, restTemplate);
        UUID docId = UUID.randomUUID();

        // 1. Join
        DocEditMessage join = new DocEditMessage();
        join.type = "JOIN";
        join.sender = "alice";
        controller.streamEdit(docId, join);

        // 2. Leave
        DocEditMessage leave = new DocEditMessage();
        leave.type = "LEAVE";
        leave.sender = "alice";

        DocEditMessage out = controller.streamEdit(docId, leave);

        // Content should now be empty because the only user left
        assertEquals("", out.content);

        verifyNoInteractions(repo);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void edit_handlesNullContentBySavingEmptyString() {
        DocumentRepository repo = mock(DocumentRepository.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        DocRealtimeController controller = new DocRealtimeController(repo, restTemplate);

        UUID docId = UUID.randomUUID();
        Document d = new Document();
        d.setContent("previous");

        when(repo.findById(docId)).thenReturn(Optional.of(d));
        when(repo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocEditMessage edit = new DocEditMessage();
        edit.type = "EDIT";
        edit.sender = "alice";
        edit.content = null; // Test the null check branch

        controller.streamEdit(docId, edit);

        assertEquals("", d.getContent());
        verify(repo).save(d);


        verifyNoInteractions(restTemplate);
    }
}
