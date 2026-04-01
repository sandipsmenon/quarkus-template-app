package com.template.quarkus.interceptor;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * FEATURE: CDI Interceptors — @Logged interceptor binding annotation.
 *
 * This is an interceptor BINDING: a custom annotation that acts as a marker
 * to say "apply the LoggingInterceptor to this class or method".
 *
 * How CDI interceptors work:
 *   1. You declare an interceptor binding annotation (@InterceptorBinding)
 *   2. You implement an interceptor class (@Interceptor) that references the binding
 *   3. You apply the binding annotation to any CDI bean class or method
 *   4. CDI automatically wraps those calls with the interceptor logic
 *
 * This enables cross-cutting concerns (AOP-style) without modifying business logic:
 *   @Logged on a method → every call is automatically logged
 *   @Timed on a method  → every call is automatically timed
 *   @Audited on a method → every call is automatically audited
 *
 * Usage:
 *   @Logged                           // apply to whole class
 *   public class ProductService { }
 *
 *   @Logged                           // apply to a single method
 *   public ProductResponse create(ProductRequest req) { ... }
 */
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Logged {
}
