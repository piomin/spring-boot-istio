package com.github.piomin.springboot.istio.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableIstio {
    int timeout() default 0;

    String version() default "";

    int weight() default 0;

    int numberOfRetries() default 0;

    int circuitBreakerErrors() default 0;
}
