package com.github.cybellereaper.particleengine.api;

public final class ParticleEngineApiProvider {
    private static volatile ParticleEngineApi api = ParticleEngineApi.noop();

    private ParticleEngineApiProvider() {
    }

    public static ParticleEngineApi get() {
        return api;
    }

    public static void set(ParticleEngineApi replacement) {
        api = replacement;
    }
}
