package com.github.cybellereaper.particleengine.script;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates execution of PEScript programs. Each script run executes on a
 * dedicated daemon worker thread so {@code wait N} statements can suspend
 * without blocking the server tick. {@link #tick()} should be invoked by the
 * engine scheduler so suspended scripts are resumed at the right moment.
 */
public final class ScriptRuntime {
    private final Logger logger;
    private final AtomicLong tickCounter = new AtomicLong();
    private final ConcurrentHashMap<UUID, RunningScript> running = new ConcurrentHashMap<>();
    private final List<Waiter> waiters = new ArrayList<>();
    private final Object waitersLock = new Object();
    private final ExecutorService executor;
    private long defaultInstructionBudget = 1_000_000L;

    public ScriptRuntime(Logger logger) {
        this.logger = logger;
        this.executor = Executors.newCachedThreadPool(threadFactory());
    }

    public void setDefaultInstructionBudget(long budget) {
        this.defaultInstructionBudget = budget;
    }

    public void shutdown() {
        for (RunningScript script : running.values()) {
            script.cancel();
        }
        executor.shutdownNow();
    }

    /**
     * Resume any waiters whose deadline has elapsed. Should be invoked once
     * per server tick.
     */
    public void tick() {
        long now = tickCounter.incrementAndGet();
        List<Waiter> toResume = new ArrayList<>();
        synchronized (waitersLock) {
            for (int i = waiters.size() - 1; i >= 0; i--) {
                Waiter waiter = waiters.get(i);
                if (waiter.cancelled || now >= waiter.deadline) {
                    toResume.add(waiter);
                    waiters.remove(i);
                }
            }
        }
        for (Waiter waiter : toResume) {
            waiter.release();
        }
    }

    public RunningScript run(String name, ScriptHost host, String source) {
        return run(name, host, source, null);
    }

    public RunningScript run(String name, ScriptHost host, String source, Consumer<Throwable> onComplete) {
        ScriptNode.Program program;
        try {
            List<ScriptToken> tokens = new ScriptLexer(source).scanTokens();
            program = new ScriptParser(tokens).parse();
        } catch (ScriptError ex) {
            log(Level.WARNING, "Script '" + name + "' failed to parse: " + ex.getMessage());
            if (onComplete != null) onComplete.accept(ex);
            return RunningScript.failed(name, ex);
        }

        UUID id = UUID.randomUUID();
        RunningScript handle = new RunningScript(id, name);
        running.put(id, handle);

        ScriptInterpreter interpreter = new ScriptInterpreter(new SuspendingHostWrapper(host, handle));
        interpreter.setInstructionBudget(defaultInstructionBudget);

        Future<?> future = executor.submit(() -> {
            try {
                interpreter.execute(program);
                if (onComplete != null) onComplete.accept(null);
            } catch (ScriptError ex) {
                log(Level.WARNING, "Script '" + name + "' failed: " + ex.getMessage());
                if (onComplete != null) onComplete.accept(ex);
            } catch (Throwable t) {
                log(Level.SEVERE, "Unexpected error in script '" + name + "': " + t);
                if (onComplete != null) onComplete.accept(t);
            } finally {
                running.remove(id);
            }
        });
        handle.bindFuture(future);
        return handle;
    }

    public boolean cancel(UUID id) {
        RunningScript script = running.get(id);
        if (script == null) return false;
        script.cancel();
        return true;
    }

    public int cancelAll() {
        int count = 0;
        for (RunningScript script : new ArrayList<>(running.values())) {
            script.cancel();
            count++;
        }
        return count;
    }

    public List<RunningScript> snapshot() {
        return List.copyOf(running.values());
    }

    private void log(Level level, String message) {
        if (logger != null) logger.log(level, message);
    }

    private ThreadFactory threadFactory() {
        return r -> {
            Thread t = new Thread(r, "PEScript-Worker");
            t.setDaemon(true);
            return t;
        };
    }

    public static final class RunningScript {
        private final UUID id;
        private final String name;
        private volatile Future<?> future;
        private volatile boolean cancelled;
        private final Throwable parseError;
        private volatile WaitCancellable wrapper;

        private RunningScript(UUID id, String name) {
            this.id = id;
            this.name = name;
            this.parseError = null;
        }

        private RunningScript(UUID id, String name, Throwable parseError) {
            this.id = id;
            this.name = name;
            this.parseError = parseError;
        }

        static RunningScript failed(String name, Throwable error) {
            return new RunningScript(UUID.randomUUID(), name, error);
        }

        public UUID id() { return id; }
        public String name() { return name; }
        public boolean isCancelled() { return cancelled; }
        public boolean isFailed() { return parseError != null; }
        public Throwable parseError() { return parseError; }

        void bindFuture(Future<?> future) { this.future = future; }
        void attachWrapper(WaitCancellable wrapper) { this.wrapper = wrapper; }

        void cancel() {
            cancelled = true;
            WaitCancellable local = wrapper;
            if (local != null) local.cancelInFlightWait();
            if (future != null) future.cancel(true);
        }
    }

    private interface WaitCancellable {
        void cancelInFlightWait();
    }

    private final class SuspendingHostWrapper implements ScriptHost, WaitCancellable {
        private final ScriptHost delegate;
        private final RunningScript handle;
        private volatile Waiter currentWaiter;

        SuspendingHostWrapper(ScriptHost delegate, RunningScript handle) {
            this.delegate = delegate;
            this.handle = handle;
            handle.attachWrapper(this);
        }

        @Override
        public void cancelInFlightWait() {
            Waiter waiter = currentWaiter;
            if (waiter != null) {
                waiter.cancelled = true;
                synchronized (waitersLock) {
                    waiters.remove(waiter);
                }
                waiter.release();
            }
        }

        @Override public UUID spawn(String templateId, ScriptLocation at, java.util.Set<String> tags, java.util.Map<String, Object> overrides) {
            return delegate.spawn(templateId, at, tags, overrides);
        }
        @Override public boolean stop(UUID runtimeId) { return delegate.stop(runtimeId); }
        @Override public int stopByTag(String tag) { return delegate.stopByTag(tag); }
        @Override public int stopAll() { return delegate.stopAll(); }
        @Override public boolean pause(UUID runtimeId) { return delegate.pause(runtimeId); }
        @Override public int pauseByTag(String tag) { return delegate.pauseByTag(tag); }
        @Override public boolean resume(UUID runtimeId) { return delegate.resume(runtimeId); }
        @Override public int resumeByTag(String tag) { return delegate.resumeByTag(tag); }
        @Override public boolean templateExists(String templateId) { return delegate.templateExists(templateId); }
        @Override public void log(String message) { delegate.log("[" + handle.name() + "] " + message); }

        @Override
        public void waitTicks(int ticks) throws InterruptedException {
            if (handle.cancelled) {
                throw new InterruptedException("Script cancelled.");
            }
            Waiter waiter = new Waiter(tickCounter.get() + Math.max(1, ticks));
            currentWaiter = waiter;
            synchronized (waitersLock) {
                if (handle.cancelled) {
                    throw new InterruptedException("Script cancelled.");
                }
                waiters.add(waiter);
            }
            try {
                waiter.await();
            } finally {
                currentWaiter = null;
            }
            if (handle.cancelled) {
                throw new InterruptedException("Script cancelled.");
            }
        }
    }

    private static final class Waiter {
        final long deadline;
        private volatile boolean cancelled;
        private final java.util.concurrent.Semaphore gate = new java.util.concurrent.Semaphore(0);

        Waiter(long deadline) {
            this.deadline = deadline;
        }

        void await() throws InterruptedException {
            gate.acquire();
        }

        void release() {
            gate.release();
        }
    }
}
