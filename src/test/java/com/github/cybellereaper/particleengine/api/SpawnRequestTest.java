package com.github.cybellereaper.particleengine.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SpawnRequestTest {
    @Test
    void forEntityUsesEntityAsInitiatorAndAnchor() {
        UUID entityId = UUID.randomUUID();

        SpawnRequest request = SpawnRequest.forEntity(entityId);

        assertEquals(entityId, request.initiator());
        assertEquals(entityId, request.anchorEntityId());
        assertNull(request.anchorLocation());
        assertTrue(request.tags().isEmpty());
        assertTrue(request.overrides().isEmpty());
    }
}
