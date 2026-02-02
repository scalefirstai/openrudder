package io.openrudder.core.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChangeEventTest {

    @Test
    void shouldCreateInsertEvent() {
        Map<String, Object> after = Map.of("id", 1, "name", "Test");
        
        ChangeEvent event = ChangeEvent.builder()
            .id("evt-1")
            .type(ChangeEvent.ChangeType.INSERT)
            .entityType("Order")
            .entityId(1)
            .after(after)
            .timestamp(Instant.now())
            .sourceId("source-1")
            .build();

        assertTrue(event.isInsert());
        assertFalse(event.isUpdate());
        assertFalse(event.isDelete());
        assertFalse(event.isSnapshot());
        assertEquals("Order", event.getEntityType());
        assertEquals(after, event.getData());
    }

    @Test
    void shouldCreateUpdateEvent() {
        Map<String, Object> before = Map.of("id", 1, "status", "PENDING");
        Map<String, Object> after = Map.of("id", 1, "status", "COMPLETED");
        
        ChangeEvent event = ChangeEvent.builder()
            .id("evt-2")
            .type(ChangeEvent.ChangeType.UPDATE)
            .entityType("Order")
            .entityId(1)
            .before(before)
            .after(after)
            .timestamp(Instant.now())
            .sourceId("source-1")
            .build();

        assertTrue(event.isUpdate());
        assertFalse(event.isInsert());
        assertEquals(after, event.getData());
    }

    @Test
    void shouldCreateDeleteEvent() {
        Map<String, Object> before = Map.of("id", 1, "name", "Test");
        
        ChangeEvent event = ChangeEvent.builder()
            .id("evt-3")
            .type(ChangeEvent.ChangeType.DELETE)
            .entityType("Order")
            .entityId(1)
            .before(before)
            .timestamp(Instant.now())
            .sourceId("source-1")
            .build();

        assertTrue(event.isDelete());
        assertFalse(event.isInsert());
        assertEquals(before, event.getData());
    }

    @Test
    void shouldGetFieldValue() {
        Map<String, Object> after = Map.of("id", 1, "name", "Test", "status", "ACTIVE");
        
        ChangeEvent event = ChangeEvent.builder()
            .id("evt-4")
            .type(ChangeEvent.ChangeType.INSERT)
            .entityType("Order")
            .after(after)
            .build();

        assertEquals(1, event.getFieldValue("id"));
        assertEquals("Test", event.getFieldValue("name"));
        assertEquals("ACTIVE", event.getFieldValue("status"));
        assertNull(event.getFieldValue("nonexistent"));
    }

    @Test
    void shouldDetectFieldChanges() {
        Map<String, Object> before = Map.of("id", 1, "status", "PENDING", "amount", 100);
        Map<String, Object> after = Map.of("id", 1, "status", "COMPLETED", "amount", 100);
        
        ChangeEvent event = ChangeEvent.builder()
            .id("evt-5")
            .type(ChangeEvent.ChangeType.UPDATE)
            .entityType("Order")
            .before(before)
            .after(after)
            .build();

        assertTrue(event.hasFieldChanged("status"));
        assertFalse(event.hasFieldChanged("amount"));
        assertFalse(event.hasFieldChanged("id"));
    }

    @Test
    void shouldHandleNullFieldChanges() {
        Map<String, Object> before = new HashMap<>();
        before.put("id", 1);
        before.put("status", null);
        
        Map<String, Object> after = new HashMap<>();
        after.put("id", 1);
        after.put("status", "ACTIVE");
        
        ChangeEvent event = ChangeEvent.builder()
            .id("evt-6")
            .type(ChangeEvent.ChangeType.UPDATE)
            .entityType("Order")
            .before(before)
            .after(after)
            .build();

        assertTrue(event.hasFieldChanged("status"));
        assertFalse(event.hasFieldChanged("id"));
    }

    @Test
    void shouldReturnFalseForFieldChangesOnNonUpdateEvents() {
        Map<String, Object> after = Map.of("id", 1, "status", "ACTIVE");
        
        ChangeEvent event = ChangeEvent.builder()
            .id("evt-7")
            .type(ChangeEvent.ChangeType.INSERT)
            .entityType("Order")
            .after(after)
            .build();

        assertFalse(event.hasFieldChanged("status"));
    }

    @Test
    void shouldGetBeforeAndAfterValues() {
        Map<String, Object> before = Map.of("id", 1, "status", "PENDING");
        Map<String, Object> after = Map.of("id", 1, "status", "COMPLETED");
        
        ChangeEvent event = ChangeEvent.builder()
            .id("evt-8")
            .type(ChangeEvent.ChangeType.UPDATE)
            .entityType("Order")
            .before(before)
            .after(after)
            .build();

        assertEquals("PENDING", event.getBeforeValue("status"));
        assertEquals("COMPLETED", event.getAfterValue("status"));
        assertNull(event.getBeforeValue("nonexistent"));
        assertNull(event.getAfterValue("nonexistent"));
    }

    @Test
    void shouldHandleSnapshotEvent() {
        Map<String, Object> after = Map.of("id", 1, "name", "Test");
        
        ChangeEvent event = ChangeEvent.builder()
            .id("evt-9")
            .type(ChangeEvent.ChangeType.SNAPSHOT)
            .entityType("Order")
            .after(after)
            .build();

        assertTrue(event.isSnapshot());
        assertFalse(event.isInsert());
        assertFalse(event.isUpdate());
        assertFalse(event.isDelete());
    }

    @Test
    void shouldIncludeMetadata() {
        Map<String, Object> metadata = Map.of("source", "postgres", "table", "orders");
        
        ChangeEvent event = ChangeEvent.builder()
            .id("evt-10")
            .type(ChangeEvent.ChangeType.INSERT)
            .entityType("Order")
            .metadata(metadata)
            .build();

        assertEquals(metadata, event.getMetadata());
        assertEquals("postgres", event.getMetadata().get("source"));
    }
}
