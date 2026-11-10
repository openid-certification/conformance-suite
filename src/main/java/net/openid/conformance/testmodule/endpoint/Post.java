package net.openid.conformance.testmodule.endpoint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shorthand for an {@link Endpoint} that only accepts POST.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Post {

	/** See {@link Endpoint#path()}. */
	String value();

	/** See {@link Endpoint#channels()}. */
	Channel[] channels() default { Channel.HTTP };

	/** See {@link Endpoint#consumes()}. */
	String[] consumes() default {};
}
