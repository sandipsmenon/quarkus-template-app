package com.template.quarkus.interceptor;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * FEATURE: CDI Interceptors — @Timed interceptor binding annotation.
 *
 * Applying @Timed to a CDI bean class or method automatically measures
 * and logs the execution time of that method via TimedInterceptor.
 *
 * Usage:
 *   @Timed
 *   public List<ProductResponse> getByCategory(String category) { ... }
 *
 * Note: Quarkus Micrometer (@Timed from micrometer) is the production way
 * to record metrics. This custom @Timed is here to demonstrate how to build
 * your own interceptors from scratch. In real projects, prefer:
 *   @io.micrometer.core.annotation.Timed
 */
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Timed {
}
