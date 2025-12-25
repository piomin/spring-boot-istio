package com.github.piomin.springboot.istio.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Match {

    boolean ignoreUriCase() default false;

    MatchType type() default MatchType.URI;

    MatchMode mode() default MatchMode.PREFIX;

    String value() default "";
    String key() default "";
}
