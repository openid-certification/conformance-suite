package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

public class AddAllSupportedStandardClaimsToAuthorizationEndpointRequestIdTokenAndUserinfoClaims extends AbstractAddClaimToAuthorizationEndpointRequest {

	// as per https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
	public static final List<String> oidcClaims = List.of(
		"sub",
		"name",
		"given_name",
		"family_name",
		"middle_name",
		"nickname",
		"preferred_username",
		"profile",
		"picture",
		"website",
		"email",
		"email_verified",
		"gender",
		"birthdate",
		"zoneinfo",
		"locale",
		"phone_number",
		"phone_number_verified",
		"address",
		"updated_at"
	);

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "server"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		JsonArray nonOidcClaims = new JsonArray();
		List<String> supportedOidcClaims = new ArrayList<>();

		JsonArray serverValues = env.getElementFromObject("server", "claims_supported").getAsJsonArray();
		for (JsonElement el: serverValues) {
			String claimName = OIDFJSON.getString(el);
			if (!oidcClaims.contains(claimName)) {
				nonOidcClaims.add(claimName);
				continue;
			}
			supportedOidcClaims.add(claimName);
		}

		if (supportedOidcClaims.isEmpty()) {
			throw error("Server does not seem to support any standard OpenID Connect claims.",
				args("oidc_claims", oidcClaims, "supported_claims", serverValues));
		}

		for (String locationStr: new String[]{"id_token", "userinfo"}) {
			JsonObject claimsObject = getClaimsForLocation(authorizationEndpointRequest, locationStr);
			addRequestsForClaims(claimsObject, supportedOidcClaims);
		}

		logSuccess("Added OpenID Connect claims from claims_supported to authorization_endpoint_request for id_token and userinfo using various different forms of request",
			args("authorization_endpoint_request", authorizationEndpointRequest,
				"oidc_claims", oidcClaims, "non_oidc_claims", nonOidcClaims));

		return env;
	}

}
