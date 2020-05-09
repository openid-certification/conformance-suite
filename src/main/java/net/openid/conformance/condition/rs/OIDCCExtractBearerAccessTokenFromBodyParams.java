package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Extract access token from body parameters
 * but don't allow more than access_token values or more than one method (header + body etc)
 * and don't allow access_token in query string
 */
public class OIDCCExtractBearerAccessTokenFromBodyParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = "incoming_access_token")
	public Environment evaluate(Environment env) {

		String authHeader = env.getString("incoming_request", "headers.authorization");
		if (!Strings.isNullOrEmpty(authHeader)) {
			if (authHeader.toLowerCase().startsWith("bearer ")) {
				String tokenFromHeader = authHeader.substring("bearer ".length());
				if(!Strings.isNullOrEmpty(tokenFromHeader)) {
					throw error("Authorization header contains a bearer token but access_token was expected " +
								"in request body parameters",
								args("authorization_header", authHeader));
				}
			}
		}
		JsonElement accessTokenElementFromQuery = env.getElementFromObject("incoming_request", "query_string_params.access_token");

		if(accessTokenElementFromQuery!=null) {
			throw error("Request contains access_token parameter in query string which is not allowed",
				args("access_token_query_parameter", accessTokenElementFromQuery));
		}

		JsonElement accessTokenElementFromForm = env.getElementFromObject("incoming_request", "body_form_params.access_token");
		String tokenFromBody = null;
		if(accessTokenElementFromForm==null) {
			throw error("Could not find an access_token parameter in request body parameters");
		} else {
			if(accessTokenElementFromForm.isJsonPrimitive()) {
				tokenFromBody = OIDFJSON.getString(accessTokenElementFromForm);
				env.putString("incoming_access_token", tokenFromBody);
				logSuccess("Found access token in incoming body parameters", args("access_token", tokenFromBody));
				return env;
			} else {
				//unexpected type
				throw error("Request body contains multiple access_token parameters", args("access_token", accessTokenElementFromForm));
			}
		}
	}

}
