package net.openid.conformance.testmodule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a method as 'user facing'.
 *
 * This only makes a difference when handling exceptions or test errors thrown from incoming http to test modules; if
 * any method (from the test module) in the call stack is marked as user facing the suite will redirect the user to
 * log-detail.html where they should be able to see the failure.
 *
 * This is relatively rare and is only the case where the test module is returning html that might be displayed in the
 * user's web browser.
 *
 * If the method is not user facing, a HTTP 500 error containing a payload detailing the exception will be returned.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserFacing {

}
