package com.github.cybellereaper.particleengine.script;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ScriptRuntimeTest {

    @Test
    void runsCompletingScript() throws Exception {
        ScriptRuntime runtime = new ScriptRuntime(null);
        try {
            TestScriptHost host = new TestScriptHost();
            ScriptRuntime.RunningScript handle = runtime.run("ok", host, "log(\"hello\");");
            assertNotNull(handle.id());
            waitFor(() -> host.logs.size() == 1, 1000);
            // ScriptRuntime prefixes log output with the script name.
            assertEquals("[ok] hello", host.logs.get(0));
        } finally {
            runtime.shutdown();
        }
    }

    @Test
    void waitSuspendsUntilTickAdvances() throws Exception {
        ScriptRuntime runtime = new ScriptRuntime(null);
        try {
            TestScriptHost host = new TestScriptHost();
            String src = "log(\"before\"); wait 5; log(\"after\");";
            runtime.run("waiter", host, src);

            waitFor(() -> host.logs.size() == 1, 1000);

            // Tick repeatedly until the script wakes up. With wait 5 we need
            // at most a handful of ticks; pushing one tick at a time mirrors
            // how the engine scheduler drives the runtime.
            long deadline = System.currentTimeMillis() + 1000;
            while (host.logs.size() < 2 && System.currentTimeMillis() < deadline) {
                runtime.tick();
                Thread.sleep(5);
            }
            assertEquals(2, host.logs.size());
            assertTrue(host.logs.get(1).endsWith("after"));
        } finally {
            runtime.shutdown();
        }
    }

    @Test
    void waitDoesNotElapseInZeroTicks() throws Exception {
        ScriptRuntime runtime = new ScriptRuntime(null);
        try {
            TestScriptHost host = new TestScriptHost();
            String src = "log(\"before\"); wait 100; log(\"after\");";
            runtime.run("waiter", host, src);

            waitFor(() -> host.logs.size() == 1, 1000);
            // No ticks - script must remain suspended.
            Thread.sleep(50);
            assertEquals(1, host.logs.size());
        } finally {
            runtime.shutdown();
        }
    }

    @Test
    void cancelStopsWaitingScript() throws Exception {
        ScriptRuntime runtime = new ScriptRuntime(null);
        try {
            TestScriptHost host = new TestScriptHost();
            ScriptRuntime.RunningScript handle = runtime.run("stuck", host, "log(\"a\"); wait 100000; log(\"b\");");
            waitFor(() -> host.logs.size() == 1, 1000);
            assertTrue(runtime.cancel(handle.id()));
            // Nothing should appear after cancel even after time passes.
            Thread.sleep(50);
            for (int i = 0; i < 10; i++) runtime.tick();
            Thread.sleep(50);
            assertEquals(1, host.logs.size());
        } finally {
            runtime.shutdown();
        }
    }

    @Test
    void parseErrorReportsImmediately() {
        ScriptRuntime runtime = new ScriptRuntime(null);
        try {
            TestScriptHost host = new TestScriptHost();
            ScriptRuntime.RunningScript handle = runtime.run("bad", host, "let x =;");
            assertTrue(handle.isFailed());
            assertNotNull(handle.parseError());
        } finally {
            runtime.shutdown();
        }
    }

    private void waitFor(java.util.function.BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(5);
        }
        fail("Condition not met within " + Duration.ofMillis(timeoutMillis));
    }
}
