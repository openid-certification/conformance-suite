package net.openid.conformance.testmodule.endpoint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shorthand for an {@link Endpoint} that only accepts GET.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Get {

	/** See {@link Endpoint#path()}. */
	String value();

	/** See {@link Endpoint#channels()}. */
	Channel[] channels() default { Channel.HTTP };
}
