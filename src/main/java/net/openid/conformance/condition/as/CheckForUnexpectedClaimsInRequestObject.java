package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class CheckForUnexpectedClaimsInRequestObject extends AbstractCondition {
	// https://www.iana.org/assignments/oauth-parameters/oauth-parameters.xhtml#parameters
	public static List<String> expectedAuthRequestParams = List.of(
		// as per https://www.iana.org/go/rfc6749
		"client_id",
		"redirect_uri",
		"response_type",
		"state",
		"scope",
		// as per https://openid.net/specs/openid-connect-core-1_0.html
		"acr_values",
		"claims",
		"claims_locales",
		"display",
		"id_token_hint",
		"login_hint",
		"max_age",
		"nonce",
		"prompt",
		"registration",
		"request",
		"request_uri",
		"ui_locales",
		// as per https://www.iana.org/go/rfc7636
		"code_challenge",
		"code_challenge_method",
		// as per https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html
		"response_mode",
		// as per https://www.iana.org/go/rfc7519#section-4.1
		"aud",
		"exp",
		"nbf",
		"iat",
		"iss",
		"jti",
		"sub",
		// as per https://www.iana.org/go/draft-ietf-oauth-dpop-16
		"dpop_jkt",
		// as per https://www.iana.org/go/rfc8485
		"vtr",
		// as per https://www.iana.org/go/rfc8707
		"resource",
		// as per https://www.iana.org/go/rfc9396
		"authorization_details",
		// as per https://cdn.connectid.com.au/specifications/oauth2-purpose-01.html
		"purpose"
	);

	// as per https://www.rfc-editor.org/rfc/rfc9101#section-4
	public static List<String> paramsNotExpectedForRequestObject = List.of(
		// Authorization endpoint parameters not expected in the request object.
		"request",
		"request_uri"
	);

	@Override
	@PreEnvironment(required = { "authorization_request_object" })
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getElementFromObject("authorization_request_object", "claims").getAsJsonObject();
		List<String> unknownClaims = new ArrayList<>();

		for (String claim : requestObjectClaims.keySet()) {
			if (expectedAuthRequestParams.contains(claim) && ! paramsNotExpectedForRequestObject.contains(claim)) {
				continue;
			}

			unknownClaims.add(claim);
		}

		if (unknownClaims.isEmpty()) {
			logSuccess("all authorization_request_object claims are expected",args("claims", requestObjectClaims.keySet()));
		} else {
			throw error("unknown claims found in authorization_request_object", args("claims", requestObjectClaims.keySet(), "unknown_claims", unknownClaims));
		}

		return env;
	}
}
