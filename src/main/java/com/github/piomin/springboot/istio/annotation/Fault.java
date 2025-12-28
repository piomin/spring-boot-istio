package com.github.piomin.springboot.istio.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Fault {
    FaultType type() default FaultType.ABORT;
    int percentage() default 100;
    int httpStatus() default 500;
    long delay() default 0;
}
