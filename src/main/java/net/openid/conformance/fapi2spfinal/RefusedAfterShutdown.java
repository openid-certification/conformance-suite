package net.openid.conformance.fapi2spfinal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint handler that the client must not call once the test has sent a response that
 * must cause it to stop interacting with the server. {@link AbstractFAPI2SPFinalClientTest} refuses
 * such requests before the handler runs.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RefusedAfterShutdown {
}
