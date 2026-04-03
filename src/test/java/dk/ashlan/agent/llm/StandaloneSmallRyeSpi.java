package dk.ashlan.agent.llm;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.api.Guard;
import io.smallrye.faulttolerance.api.Spi;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.apiimpl.BasicCircuitBreakerMaintenanceImpl;
import io.smallrye.faulttolerance.apiimpl.BuilderEagerDependencies;
import io.smallrye.faulttolerance.apiimpl.BuilderLazyDependencies;
import io.smallrye.faulttolerance.apiimpl.GuardImpl;
import io.smallrye.faulttolerance.core.event.loop.EventLoop;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;
import io.smallrye.faulttolerance.core.timer.Timer;
import io.smallrye.faulttolerance.core.timer.ThreadTimer;
import io.smallrye.faulttolerance.vertx.VertxEventLoop;

import java.lang.reflect.Type;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"deprecation", "removal"})
public final class StandaloneSmallRyeSpi implements Spi {
    private static final CircuitBreakerMaintenance CIRCUIT_BREAKER_MAINTENANCE = new BasicCircuitBreakerMaintenanceImpl();
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "smallrye-ft-test-async");
        thread.setDaemon(true);
        return thread;
    });
    private static final BuilderEagerDependencies EAGER_DEPENDENCIES = () -> (BasicCircuitBreakerMaintenanceImpl) CIRCUIT_BREAKER_MAINTENANCE;
    private static final Supplier<BuilderLazyDependencies> LAZY_DEPENDENCIES = StandaloneSmallRyeSpi::lazyDependencies;

    @Override
    public boolean applies() {
        return true;
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Guard.Builder newGuardBuilder() {
        return new GuardImpl.BuilderImpl(EAGER_DEPENDENCIES, LAZY_DEPENDENCIES);
    }

    @Override
    public <T> TypedGuard.Builder<T> newTypedGuardBuilder(Type type) {
        throw new UnsupportedOperationException("Typed guards are not needed in tests");
    }

    @Override
    public <T, R> FaultTolerance.Builder<T, R> newBuilder(Function<FaultTolerance<T>, R> function) {
        throw new UnsupportedOperationException("FaultTolerance builder is not needed in tests");
    }

    @Override
    public <T, R> FaultTolerance.Builder<T, R> newAsyncBuilder(Class<?> asyncType, Function<FaultTolerance<T>, R> function) {
        throw new UnsupportedOperationException("Async FaultTolerance builder is not needed in tests");
    }

    @Override
    public CircuitBreakerMaintenance circuitBreakerMaintenance() {
        return CIRCUIT_BREAKER_MAINTENANCE;
    }

    private static BuilderLazyDependencies lazyDependencies() {
        return new BuilderLazyDependencies() {
            @Override
            public boolean ftEnabled() {
                return true;
            }

            @Override
            public ExecutorService asyncExecutor() {
                return ASYNC_EXECUTOR;
            }

            @Override
            public EventLoop eventLoop() {
                return new VertxEventLoop();
            }

            @Override
            public Timer timer() {
                return new ThreadTimer(ASYNC_EXECUTOR);
            }

            @Override
            public MetricsProvider metricsProvider() {
                return new MetricsProvider() {
                    @Override
                    public boolean isEnabled() {
                        return false;
                    }

                    @Override
                    public MetricsRecorder create(io.smallrye.faulttolerance.core.metrics.MeteredOperation operation) {
                        return MetricsRecorder.NOOP;
                    }
                };
            }
        };
    }
}
