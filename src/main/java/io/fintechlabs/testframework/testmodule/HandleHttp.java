package io.fintechlabs.testframework.testmodule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used by subclasses of AbstractTest module to mark a public
 * method as a handler for HTTP calls sent from the TestDispatcher. The value of the
 * annotation is the path used by the dispatcher to match the incoming request. The single
 * most specific match will be executed, all others will be ignored. The handler method
 * may have any name and must have a method signature of:
 *
 * 	public Object methodName(
 * 		HttpServletRequest request,
 * 		HttpServletResponse response,
 * 		HttpSession session,
 * 		JsonObject requestParts){
 *
 * The requestParts object contains the following elements from the TestDispatcher.handle method:
 *
 *   params: the incoming query or form parameters, if present
 *   headers: the HTTP headers, with header names lowercased
 *   method: the HTTP method
 *   body: the request body, if present
 *   body_json: the request body parsed as a JSON object, if present and the request was in JSON
 *
 * The annotated method must return a value suitable for processing by the Spring MVC framework.
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HandleHttp {

	/**
	 * The ant-style path used for matching incoming requests.
	 */
	String value();
}
