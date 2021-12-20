package com.udacity.webcrawler.profiler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks which methods should have their running time profiled.
 *
 * <p>This annotation does not include the @Qualifier marker because it is
 * not a Guice annotation and there will be other annotations.</p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Profiled {
}
