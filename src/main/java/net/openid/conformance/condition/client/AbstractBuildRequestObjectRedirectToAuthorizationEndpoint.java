package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractBuildRequestObjectRedirectToAuthorizationEndpoint extends AbstractCondition {
	/**
	 * A list of parameters that must also be included in the url query, even when they are already in the request
	 * object.
	 *
	 * response_type, client_id, scope are required by these clauses from
	 * https://openid.net/specs/openid-connect-core-1_0.html#RequestObject :
	 *
	 *    So that the request is a valid OAuth 2.0 Authorization Request, values for the response_type and client_id
	 *    parameters MUST be included using the OAuth 2.0 request syntax, since they are REQUIRED by OAuth 2.0. The
	 *    values for these parameters MUST match those in the Request Object, if present.
	 *
	 *    Even if a scope parameter is present in the Request Object value, a scope parameter MUST always be passed
	 *    using the OAuth 2.0 request syntax containing the openid scope value to indicate to the underlying OAuth
	 *    2.0 logic that this is an OpenID Connect request.
	 *
	 * redirect_uri is required because of this clause from https://tools.ietf.org/html/rfc6749#section-3.1.2.3 :
	 *
	 *    If multiple redirection URIs have been registered, if only part of
	 *    the redirection URI has been registered, or if no redirection URI has
	 *    been registered, the client MUST include a redirection URI with the
	 *    authorization request using the "redirect_uri" request parameter.
	 */
	private static final List<String> REQUIRED_DUPLICATES = Arrays.asList(new String[] {
		"response_type",
		"client_id",
		"scope",
		"redirect_uri"
	});

	/**
	 * @param includeDuplicates If true include the duplicate parameters required by RFC6749/OIDC - if false,
	 *                          skip the duplicates as permitted by JAR / PAR.
	 */
	protected Environment buildRedirect(Environment env, String paramName, String paramValue, boolean includeDuplicates) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		String authorizationEndpoint = env.getString("authorization_endpoint") != null ? env.getString("authorization_endpoint") : env.getString("server", "authorization_endpoint");
		if (Strings.isNullOrEmpty(authorizationEndpoint)) {
			throw error("Couldn't find authorization endpoint");
		}

		// send a front channel request to start things off
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(authorizationEndpoint);

		builder.queryParam(paramName, paramValue);

		for (String key : authorizationEndpointRequest.keySet()) {

			JsonElement requestObjectElement = requestObjectClaims.get(key);
			JsonElement requestParameterElement = authorizationEndpointRequest.get(key);
			if (requestObjectElement != null && !(requestObjectElement instanceof JsonPrimitive)
				|| !(requestParameterElement instanceof JsonPrimitive)) {
				// only handle stringable values for now (as BuildPlainRedirectToAuthorizationEndpoint)
				continue;
			}

			String requestObjectValue = null;
			if (requestObjectElement != null) {
				requestObjectValue = OIDFJSON.forceConversionToString(requestObjectElement);
			}
			String requestParameterValue = OIDFJSON.forceConversionToString(requestParameterElement);

			if (key.equals("state")) {
				Boolean exposeState = env.getBoolean("expose_state_in_authorization_endpoint_request");
				if (exposeState != null && exposeState.equals(true) ) {
					builder.queryParam("state", env.getString("state"));
				}
			}

			if (includeDuplicates) {
				if (REQUIRED_DUPLICATES.contains(key)
					|| requestObjectValue == null
					|| !requestParameterValue.equals(requestObjectValue)) {
					builder.queryParam(key, requestParameterValue);
				}
			} else {
				if (key.equals("client_id")) {
					builder.queryParam(key, requestParameterValue);
				}
			}
		}

		String redirectTo = builder.toUriString();

		logSuccess("Sending to authorization endpoint", args("redirect_to_authorization_endpoint", redirectTo));

		env.putString("redirect_to_authorization_endpoint", redirectTo);

		return env;
	}
}
