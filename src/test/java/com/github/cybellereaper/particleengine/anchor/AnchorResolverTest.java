package com.github.cybellereaper.particleengine.anchor;

import com.github.cybellereaper.particleengine.effect.*;
import com.github.cybellereaper.particleengine.runtime.EffectRuntimeContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnchorResolverTest {

    @Test
    void resolveReturnsAnchorLocationWhenProvided() {
        AnchorResolver resolver = new AnchorResolver(new NoopLocator());
        Location anchorLocation = new Location(null, 1, 2, 3);
        EffectRuntimeContext context = new EffectRuntimeContext(null, anchorLocation, null, Set.of(), Map.of());

        Optional<Location> resolved = resolver.resolve(templateWith(AnchorType.COORDINATE, "feet"), context);

        assertTrue(resolved.isPresent());
        assertNotSame(anchorLocation, resolved.get());
        assertEquals(anchorLocation, resolved.get());
    }

    @Test
    void resolveReturnsEmptyWhenNoAnchorCouldBeResolved() {
        AnchorResolver resolver = new AnchorResolver(new NoopLocator());
        EffectRuntimeContext context = new EffectRuntimeContext(null, null, null, Set.of(), Map.of());

        Optional<Location> resolved = resolver.resolve(templateWith(AnchorType.PLAYER, "head"), context);

        assertTrue(resolved.isEmpty());
    }

    private EffectTemplate templateWith(AnchorType type, String selector) {
        return new EffectTemplate(
                "id",
                EffectFamily.TRAIL,
                Particle.END_ROD,
                1,
                20,
                new AnchorDefinition(type, selector, CoordinateSpace.WORLD),
                List.of(),
                List.of(),
                new TimelineDefinition(20, false, 0, List.of()),
                List.of(),
                Map.of()
        );
    }

    private static final class NoopLocator implements AnchorResolver.EntityLocator {
        @Override
        public Optional<Entity> findEntity(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<Player> findPlayer(UUID id) {
            return Optional.empty();
        }
    }
}
