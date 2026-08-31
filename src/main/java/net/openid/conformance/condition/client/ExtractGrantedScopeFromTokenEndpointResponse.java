package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Records the scope the authorization server actually granted, as returned in the token endpoint response.
 *
 * RFC6749-6 requires that the scope of a refresh request "MUST NOT include any scope not originally granted
 * by the resource owner", so a subsequent refresh has to be based on the granted scope rather than on the
 * scope configured for the client - the two differ whenever the user, or a grant management action, narrows
 * what was granted. {@link AddScopeToTokenEndpointRequest} picks the value up from here.
 *
 * The value is stored under the client object, which the multiple-client tests map per client, so one
 * client cannot pick up the scope granted to another. It is also cleared when the response carries no
 * scope, so a stale value cannot survive into a later request by the same client.
 */
public class ExtractGrantedScopeFromTokenEndpointResponse extends AbstractCondition {

	public static final String GRANTED_SCOPE = "granted_scope";

	@Override
	@PreEnvironment(required = { "token_endpoint_response", "client" })
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		JsonObject client = env.getObject("client");
		String grantedScope = env.getString("token_endpoint_response", "scope");

		if (Strings.isNullOrEmpty(grantedScope)) {
			client.remove(GRANTED_SCOPE);
			logSuccess("The token endpoint response does not contain a scope, so the requested scope will continue to be used");
			return env;
		}

		client.addProperty(GRANTED_SCOPE, grantedScope);
		logSuccess("Recorded the scope granted by the authorization server", args("scope", grantedScope));

		return env;
	}
}
