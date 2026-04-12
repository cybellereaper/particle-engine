package com.github.cybellereaper.particleengine.integration;

public interface IntegrationHook {
    String name();
    boolean isAvailable();
    void enable();
}
