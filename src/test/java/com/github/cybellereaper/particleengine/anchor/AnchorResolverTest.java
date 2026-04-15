package com.github.cybellereaper.particleengine.anchor;

import com.github.cybellereaper.particleengine.effect.*;
import com.github.cybellereaper.particleengine.runtime.EffectRuntimeContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnchorResolverTest {

    @Test
    void resolveReturnsAnchorLocationForCoordinateTemplates() {
        AnchorResolver resolver = new AnchorResolver(new NoopLocator());
        Location anchorLocation = new Location(null, 1, 2, 3);
        EffectRuntimeContext context = new EffectRuntimeContext(UUID.randomUUID(), anchorLocation, UUID.randomUUID(), Set.of(), Map.of());

        Optional<Location> resolved = resolver.resolve(templateWith(AnchorType.COORDINATE, "feet"), context);

        assertTrue(resolved.isPresent());
        assertNotSame(anchorLocation, resolved.get());
        assertEquals(anchorLocation, resolved.get());
    }

    @Test
    void resolvePlayerPrefersEntityAnchorOverFallbackLocation() {
        UUID playerId = UUID.randomUUID();
        Location fallbackLocation = new Location(null, 100, 100, 100);
        Player player = fakePlayer(new Location(null, 10, 64, 10), 1.8D, true);
        AnchorResolver resolver = new AnchorResolver(new Locator(Map.of(), Map.of(playerId, player)));

        EffectRuntimeContext context = new EffectRuntimeContext(playerId, fallbackLocation, null, Set.of(), Map.of());

        Location resolved = resolver.resolve(templateWith(AnchorType.PLAYER, "head"), context).orElseThrow();

        assertEquals(10D, resolved.getX(), 1e-6);
        assertEquals(65.8D, resolved.getY(), 1e-6);
        assertEquals(10D, resolved.getZ(), 1e-6);
    }

    @Test
    void resolvePlayerFallsBackToAnchorLocationWhenPlayerMissing() {
        AnchorResolver resolver = new AnchorResolver(new NoopLocator());
        Location fallbackLocation = new Location(null, 3, 4, 5);
        EffectRuntimeContext context = new EffectRuntimeContext(UUID.randomUUID(), fallbackLocation, null, Set.of(), Map.of());

        Location resolved = resolver.resolve(templateWith(AnchorType.PLAYER, "head"), context).orElseThrow();

        assertEquals(fallbackLocation, resolved);
    }

    @Test
    void resolveEntityBackAnchorUsesEntityDirection() {
        UUID entityId = UUID.randomUUID();
        Entity entity = fakeEntity(new Location(null, 4, 70, 8, 0, 0), 2.0D, true);
        AnchorResolver resolver = new AnchorResolver(new Locator(Map.of(entityId, entity), Map.of()));

        EffectRuntimeContext context = new EffectRuntimeContext(null, null, entityId, Set.of(), Map.of());

        Location resolved = resolver.resolve(templateWith(AnchorType.ENTITY, "back"), context).orElseThrow();

        assertEquals(4D, resolved.getX(), 1e-6);
        assertEquals(71.1D, resolved.getY(), 1e-6);
        assertEquals(7.65D, resolved.getZ(), 1e-6);
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

    private Entity fakeEntity(Location location, double height, boolean valid) {
        return (Entity) Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                new Class[]{Entity.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getLocation" -> location.clone();
                    case "getHeight" -> height;
                    case "isValid" -> valid;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private Player fakePlayer(Location location, double height, boolean valid) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getLocation" -> location.clone();
                    case "getHeight" -> height;
                    case "isValid" -> valid;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
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

    private record Locator(Map<UUID, Entity> entities, Map<UUID, Player> players) implements AnchorResolver.EntityLocator {
        @Override
        public Optional<Entity> findEntity(UUID id) {
            return Optional.ofNullable(entities.get(id));
        }

        @Override
        public Optional<Player> findPlayer(UUID id) {
            return Optional.ofNullable(players.get(id));
        }
    }
}
