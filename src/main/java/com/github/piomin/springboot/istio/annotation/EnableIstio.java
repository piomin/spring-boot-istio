package com.github.piomin.springboot.istio.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableIstio {
    /**
     * Timeout in seconds
     * @return timeout value
     */
    int timeout() default 0;

    /**
     * Version of the service set in the destination rule
     * @return version value
     */
    String version() default "";

    /**
     * Weight of the service (0-100) set in the destination rule
     * @return weight value
     */
    int weight() default 0;

    /**
     * Number of retries
     * @return number of retries value
     */
    int numberOfRetries() default 0;

    int circuitBreakerErrors() default 0;

    Match[] matches() default {};
  
    Fault fault() default @Fault(percentage = 0);

    boolean enableGateway() default false;
}
