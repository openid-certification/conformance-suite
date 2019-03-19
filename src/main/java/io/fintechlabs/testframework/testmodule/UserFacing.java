package io.fintechlabs.testframework.testmodule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is placed on an HTTP handler method to indicate to the TestRunner that the page
 * is intended to face an end user, as opposed to being consumed by an API client. Specifically, any errors
 * thrown from within should be handled by rendering an error page instead of a JSON object.
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserFacing {

}
