package com.template.quarkus.interceptor;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;

import java.util.Arrays;

/**
 * FEATURE: CDI Interceptors — Automatic method logging.
 *
 * The @Interceptor annotation marks this class as a CDI interceptor.
 * @Logged is the binding — this interceptor runs whenever @Logged is on a method/class.
 * @Priority controls execution order when multiple interceptors apply to the same method.
 *
 * @AroundInvoke wraps the actual method call:
 *   - Code before ctx.proceed() runs BEFORE the target method
 *   - Code after ctx.proceed() runs AFTER the target method returns
 *   - Catching exceptions lets you handle or re-throw them
 *
 * Priority constants (lower number = runs first):
 *   Interceptor.Priority.PLATFORM_BEFORE (< 1000) — platform interceptors
 *   Interceptor.Priority.LIBRARY_BEFORE  (< 2000) — library interceptors
 *   Interceptor.Priority.APPLICATION     (= 2000) — application interceptors
 *   Interceptor.Priority.LIBRARY_AFTER   (< 3000) — library post-processing
 *   Interceptor.Priority.PLATFORM_AFTER  (< 4000) — platform post-processing
 */
@Logged
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class LoggingInterceptor {

    private static final Logger LOG = Logger.getLogger(LoggingInterceptor.class);

    @AroundInvoke
    public Object logMethodCall(InvocationContext ctx) throws Exception {
        String className  = ctx.getMethod().getDeclaringClass().getSimpleName();
        String methodName = ctx.getMethod().getName();
        Object[] params   = ctx.getParameters();

        LOG.debugf("[INTERCEPTOR] → %s.%s(%s)", className, methodName,
                params != null ? Arrays.toString(params) : "");

        try {
            Object result = ctx.proceed();  // invoke the actual method

            LOG.debugf("[INTERCEPTOR] ← %s.%s returned: %s", className, methodName,
                    result != null ? result.getClass().getSimpleName() : "void");
            return result;

        } catch (Exception e) {
            LOG.errorf("[INTERCEPTOR] ✗ %s.%s threw %s: %s",
                    className, methodName, e.getClass().getSimpleName(), e.getMessage());
            throw e;  // always re-throw to preserve original behaviour
        }
    }
}
