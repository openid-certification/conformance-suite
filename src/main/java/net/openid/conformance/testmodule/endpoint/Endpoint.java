package net.openid.conformance.testmodule.endpoint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a method as the handler for an emulated endpoint. The annotation is interpreted by
 * {@link EndpointRegistry}, not by Spring MVC: requests still reach the module through
 * {@code TestDispatcher}, which keeps the test lock and status handling in place.
 *
 * <p>A handler is a public method of a public class (the registry invokes it without changing its
 * accessibility). It takes either no parameters or a single {@link EndpointRequest}, and returns
 * what the module's {@code handleHttp} would return (a {@code ResponseEntity}, a view, ...).
 *
 * <p>The annotation is found on the most-derived declaration in the class hierarchy that carries it,
 * and the method is invoked virtually, so a subclass can override a handler without repeating it.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Endpoint {

	/**
	 * Path relative to the channel's base URL, with or without a leading slash, such as {@code "token"}.
	 */
	String path();

	/**
	 * Accepted HTTP methods, upper case. Empty accepts any method.
	 */
	String[] methods() default {};

	/**
	 * Channels the endpoint is reachable on. A request to the same path on another channel is unmatched.
	 */
	Channel[] channels() default { Channel.HTTP };

	/**
	 * Media types the endpoint expects in the request body, for interceptors that check Content-Type.
	 * The dispatcher itself does not enforce them.
	 */
	String[] consumes() default {};
}
