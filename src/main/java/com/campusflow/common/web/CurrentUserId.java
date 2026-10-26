package com.campusflow.common.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the id of the logged-in user (the JWT subject) into a controller parameter of type {@code Long}.
 * With {@code required = false} the value is {@code null} for anonymous requests on public endpoints.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {

    boolean required() default true;
}
