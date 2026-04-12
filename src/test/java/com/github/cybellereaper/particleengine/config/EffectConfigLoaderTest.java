package com.github.cybellereaper.particleengine.config;

import com.github.cybellereaper.particleengine.effect.ShapeType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class EffectConfigLoaderTest {

    @Test
    void validatesAndParsesSingleEffect() throws IOException {
        File dataDir = Files.createTempDirectory("pe-config-test").toFile();
        File effects = new File(dataDir, "effects");
        assertTrue(effects.mkdirs());
        Files.writeString(new File(effects, "test.yml").toPath(), """
            effects:
              demo:
                family: CIRCLE
                particle: FLAME
                count: 2
                lifetimeTicks: 30
                emitters:
                  - shape: CIRCLE
                    rate: 1
                    points: 6
                    radius: 1.0
            """);

        Plugin plugin = org.mockito.Mockito.mock(Plugin.class);
        org.mockito.Mockito.when(plugin.getDataFolder()).thenReturn(dataDir);

        EffectConfigLoader loader = new EffectConfigLoader(plugin);
        LoadResult result = loader.loadAll();

        assertEquals(1, result.templates().size());
        assertEquals(ShapeType.CIRCLE, result.templates().getFirst().emitters().getFirst().shape());
        assertFalse(result.hasErrors());
    }
}
