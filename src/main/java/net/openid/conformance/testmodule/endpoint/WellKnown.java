package net.openid.conformance.testmodule.endpoint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a handler for a host-level well-known document on the {@link Channel#WELL_KNOWN} channel.
 *
 * <p>{@code @WellKnown("oauth-authorization-server")} matches {@code /.well-known/oauth-authorization-server}
 * and any path below it, since RFC 8414 section 3.1 inserts the issuer's path component after the
 * well-known suffix ({@code /.well-known/oauth-authorization-server/tenant1}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WellKnown {

	/** The well-known suffix, such as {@code "oauth-authorization-server"} or {@code "openid-credential-issuer"}. */
	String value();

	/** Accepted HTTP methods, upper case. Empty accepts any method. */
	String[] methods() default {};
}
