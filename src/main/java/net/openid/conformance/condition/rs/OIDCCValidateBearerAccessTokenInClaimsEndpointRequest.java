package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * https://openid.net/specs/openid-connect-core-1_0.html#AggregatedDistributedClaims
 * access_token
 * 	OPTIONAL. Access Token enabling retrieval of the Claims from the endpoint URL by using the OAuth
 * 	2.0 Bearer Token Usage [RFC6750] protocol.
 * 	Claims SHOULD be requested using the Authorization Request header field and Claims Providers
 * 	MUST support this method. If the Access Token is not available, RPs MAY need to retrieve the
 * 	Access Token out of band or use an Access Token that was pre-negotiated between the Claims Provider
 * 	and RP, or the Claims Provider MAY reauthenticate the End-User and/or reauthorize the RP.
 */
public class OIDCCValidateBearerAccessTokenInClaimsEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
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

		//this is in theory allowed by RFC6750 but not recommended, we don't allow it
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

		String accessTokenInEnv = env.getString("distributed_claims_access_token");
		if(incomingAccessToken.equals(accessTokenInEnv)) {
			logSuccess("Request contains a valid access token", args("access_token", incomingAccessToken));
		} else {
			throw error("Invalid access token in request",
				args("expected_access_token", accessTokenInEnv,
					"actual_access_token", incomingAccessToken));
		}

		return env;

	}

}
