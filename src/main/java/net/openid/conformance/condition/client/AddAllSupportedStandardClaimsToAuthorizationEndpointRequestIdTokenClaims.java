package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class AddAllSupportedStandardClaimsToAuthorizationEndpointRequestIdTokenClaims extends AbstractAddClaimToAuthorizationEndpointRequest {

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

	private enum ClaimRequestType {
		// We test (if we have enough supported claims!) all the ways a claim can be requested as per
		// https://openid.net/specs/openid-connect-core-1_0.html#IndividualClaimsRequests
		// We don't test value / values as we don't know what values the server may return
		AsNull, AsEmpty, EssentialTrue, EssentialFalse;

		static public final ClaimRequestType[] values = values();

		public ClaimRequestType next() {
			return values[(ordinal() + 1) % values.length];
		}
	}

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "server"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		String locationStr = "id_token";
		JsonObject claimsIdToken = getClaimsForLocation(authorizationEndpointRequest, locationStr);
		ClaimRequestType requestType = ClaimRequestType.values[0];
		JsonArray nonOidcClaims = new JsonArray();
		boolean addedClaim = false;

		JsonArray serverValues = env.getElementFromObject("server", "claims_supported").getAsJsonArray();
		for (JsonElement el: serverValues) {
			String claimName = OIDFJSON.getString(el);
			if (!oidcClaims.contains(claimName)) {
				nonOidcClaims.add(claimName);
				continue;
			}
			addedClaim = true;
			if (requestType == ClaimRequestType.AsNull) {
				claimsIdToken.add(claimName, JsonNull.INSTANCE);
			} else {
				JsonObject claimBody = new JsonObject();
				switch (requestType) {
					case AsEmpty:
						break;
					case EssentialTrue:
						claimBody.addProperty("essential", true);
						break;
					case EssentialFalse:
						claimBody.addProperty("essential", false);
						break;
				}
				claimsIdToken.add(claimName, claimBody);
			}
			requestType = requestType.next();
		}

		if (!addedClaim) {
			throw error("Server does not seem to support any standard OpenID Connect claims.",
				args("oidc_claims", oidcClaims, "supported_claims", serverValues));
		}

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added OpenID Connect claims from claims_supported to authorization_endpoint_request using various different forms of request",
			args("authorization_endpoint_request", authorizationEndpointRequest,
				"oidc_claims", oidcClaims, "non_oidc_claims", nonOidcClaims));

		return env;
	}

}
