package com.template.quarkus.interceptor;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;

import java.util.concurrent.TimeUnit;

/**
 * FEATURE: CDI Interceptors — Automatic method execution timing.
 *
 * Runs at Priority APPLICATION+1 (after LoggingInterceptor at APPLICATION),
 * so logging wraps timing (logging sees the full round-trip including timing overhead).
 *
 * The @AroundInvoke method captures System.nanoTime() before and after
 * ctx.proceed(), then logs the elapsed time in milliseconds.
 *
 * In production, replace the log statement with a Micrometer timer:
 *   registry.timer("method.execution", "class", className, "method", methodName)
 *            .record(elapsed, TimeUnit.NANOSECONDS);
 */
@Timed
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 1)
public class TimedInterceptor {

    private static final Logger LOG = Logger.getLogger(TimedInterceptor.class);

    @AroundInvoke
    public Object timeMethod(InvocationContext ctx) throws Exception {
        long startNs = System.nanoTime();
        try {
            return ctx.proceed();
        } finally {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
            String className  = ctx.getMethod().getDeclaringClass().getSimpleName();
            String methodName = ctx.getMethod().getName();
            LOG.debugf("[TIMED] %s.%s took %dms", className, methodName, elapsedMs);
        }
    }
}
