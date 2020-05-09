package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * extract either from headers or parameters
 * but don't allow more than one (https://tools.ietf.org/html/rfc6750#section-2
 *    Clients MUST NOT use more
 *    than one method to transmit the token in each request.)
 */
public class OIDCCExtractBearerAccessTokenFromRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = "incoming_access_token")
	public Environment evaluate(Environment env) {

		String tokenFromHeader = null;
		String tokenFromParams = null;

		String authHeader = env.getString("incoming_request", "headers.authorization");
		if (!Strings.isNullOrEmpty(authHeader)) {
			if (authHeader.toLowerCase().startsWith("bearer ")) {
				tokenFromHeader = authHeader.substring("bearer ".length());
			}
		}
		JsonElement accessTokenElementFromForm = env.getElementFromObject("incoming_request", "body_form_params.access_token");
		JsonElement accessTokenElementFromQuery = env.getElementFromObject("incoming_request", "query_string_params.access_token");

		if(accessTokenElementFromQuery!=null) {
			throw error("Request contains access_token parameter in query string",
				args("access_token_query_parameter", accessTokenElementFromQuery));
		}

		if(accessTokenElementFromForm!=null) {
			if(accessTokenElementFromForm.isJsonPrimitive()) {
				tokenFromParams = OIDFJSON.getString(accessTokenElementFromForm);
			} else {
				//unexpected type
				throw error("Request body contains multiple access_token parameters", args("access_token", accessTokenElementFromForm));
			}
		}

		if(Strings.isNullOrEmpty(tokenFromHeader) && Strings.isNullOrEmpty(tokenFromParams)) {
			throw error("Couldn't find a bearer token in request");
		}
		if(!Strings.isNullOrEmpty(tokenFromHeader) && !Strings.isNullOrEmpty(tokenFromParams)) {
			throw error("Found more than one access token in request",
				args("token_from_authorization_header", tokenFromHeader,
					"token_from_request_parameters", tokenFromParams));
		}
		String incomingAccessToken = null;
		if(!Strings.isNullOrEmpty(tokenFromHeader)) {
			incomingAccessToken = tokenFromHeader;
		} else {
			incomingAccessToken = tokenFromParams;
		}

		env.putString("incoming_access_token", incomingAccessToken);
		logSuccess("Found access token on incoming request", args("access_token", incomingAccessToken));

		return env;

	}

}
