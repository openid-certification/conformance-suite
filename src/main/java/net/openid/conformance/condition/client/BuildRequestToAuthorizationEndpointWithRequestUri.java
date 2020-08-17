package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class builds a redirect request to AS with the request_uri
 *
 * This is very similar to BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint, but differs in how
 * it handles client_id - here we read the client_id to go outside the request object from the 'client' object
 * in the environment, rather than from the request object claims.
 */
public class BuildRequestToAuthorizationEndpointWithRequestUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "request_uri", required = {"client", "request_object_claims"})
	@PostEnvironment(strings = "redirect_to_authorization_endpoint")
	public Environment evaluate(Environment env) {

		UriComponentsBuilder builder = buildRequestUri(env);

		String redirectTo = builder.toUriString();

		logSuccess("Sending to authorization endpoint", args("redirect_to_authorization_endpoint", redirectTo));

		env.putString("redirect_to_authorization_endpoint", redirectTo);

		return env;
	}

	private void addClientId(
		UriComponentsBuilder builder, Environment env) {
		String clientId =  env.getString("client", "client_id");

		//TO comply with OIDC Core 1.0 client_id is mandatory at the level of request_uri
		if (clientId == null) {
			throw error("Couldn't find client_id");
		}

		builder.queryParam("client_id", clientId);
	}

	protected UriComponentsBuilder buildRequestUri(Environment env) {
		String requestUri = env.getString("request_uri");
		if (requestUri == null) {
			throw error("Couldn't find request_uri");
		}

		String authorizationEndpoint = env.getString("server", "authorization_endpoint");
		if (Strings.isNullOrEmpty(authorizationEndpoint)) {
			throw error("Couldn't find authorization endpoint");
		}
		// send a front channel request to start things off
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(authorizationEndpoint);

		builder.queryParam("request_uri", requestUri);

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		// add duplicates of various fields outside the request object, as required by RFC6749/OpenID Connect/FAPI-RW
		// see also https://bitbucket.org/openid/fapi/issues/304/are-duplicates-of-the-response_type
		JsonElement responseTypeElement =  requestObjectClaims.get("response_type");
		if (responseTypeElement == null) {
			throw error("Could not find response_type element in request_object_claims");
		}
		String responseType = OIDFJSON.getString(responseTypeElement);

		builder.queryParam("response_type", responseType);

		String scope = OIDFJSON.getString(requestObjectClaims.get("scope"));
		builder.queryParam("scope", scope);

		addClientId(builder, env);

		String redirectUri = env.getString("redirect_uri");
		if (!Strings.isNullOrEmpty(redirectUri)) {
			builder.queryParam("redirect_uri", redirectUri);
		}

		return builder;
	}
}
