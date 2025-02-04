package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractAddClaimToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class CreateUnverifiedClaimsToRequestInAuthorizationEndpointRequest extends AbstractAddClaimToAuthorizationEndpointRequest {
	// as per https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
	// and https://openid.net/specs/openid-connect-4-ida-claims-1_0.html
	public static final List<String> eKycClaims = List.of(
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
		"updated_at",
		"place_of_birth",
		"nationalities",
		"birth_family_name",
		"birth_given_name",
		"birth_middle_name",
		"salutation",
		"title",
		"msisdn",
		"also_known_as"
	);

	/**
	 * unverified_claims_to_request must be like
	 * {
	 *     "a claim name": {
	 *         "location":"ID_TOKEN",
	 *         "value":"some value",
	 *         "essential:true
	 *     }
	 * }
	 * essential is optional
	 * @param env
	 * @return
	 */
	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = "unverified_claims_to_request")
	public Environment evaluate(Environment env) {
		JsonElement claimsSupportedElement = env.getElementFromObject("server", "claims_supported");
		if(claimsSupportedElement==null) {
			throw error("claims_supported element in server configuration is required for this test");
		}
		JsonArray claimsSupportedArray = claimsSupportedElement.getAsJsonArray();
		JsonElement verifiedClaimsSupportedElement = env.getElementFromObject("server", "claims_in_verified_claims_supported");
		if(verifiedClaimsSupportedElement==null) {
			throw error("claims_in_verified_claims_supported element in server configuration is required for this test");
		}

		JsonElement unverifiedClaimsElement = env.getElementFromObject("config", "ekyc_unverified_claims_names");
		JsonArray requestedUnverifiedClaimsList;
		int maxRequestedClaims = 1;
		if(null != unverifiedClaimsElement) {
			if(unverifiedClaimsElement.isJsonArray()) {
				requestedUnverifiedClaimsList = unverifiedClaimsElement.getAsJsonArray();
				maxRequestedClaims = requestedUnverifiedClaimsList.size();
			} else if(unverifiedClaimsElement.isJsonPrimitive()) {
				requestedUnverifiedClaimsList = new JsonArray();
				requestedUnverifiedClaimsList.add(unverifiedClaimsElement);
			} else {
				throw error("ekyc_unverified_claims_names is not JSON array or primitive", args("ekyc_unverified_claims_names", unverifiedClaimsElement));
			}
		} else {
			requestedUnverifiedClaimsList = claimsSupportedArray;  // use claims from claims_supported
		}

		JsonArray verifiedClaimsSupportedArray = verifiedClaimsSupportedElement.getAsJsonArray();
		int matchedClaimCount = 0;
		JsonObject unverifiedClaimsToRequest = new JsonObject();
		for(JsonElement claimName : requestedUnverifiedClaimsList) {
			// check server supports the claim in claims_supported or claims_in_verified_claims_supported metadata
			if(!verifiedClaimsSupportedArray.contains(claimName) && !claimsSupportedArray.contains(claimName)) {
				continue;
			}
			//skip sub and anything that doesn't exist in eKycClaims
			if("sub".equals(OIDFJSON.getString(claimName))) {
				continue;
			}
			if(!eKycClaims.contains(OIDFJSON.getString(claimName))) {
				continue;
			}
			JsonObject claimInfo = new JsonObject();
			claimInfo.addProperty("essential", false);
			unverifiedClaimsToRequest.add(OIDFJSON.getString(claimName), claimInfo);
			matchedClaimCount++;
			if(matchedClaimCount > maxRequestedClaims) {
				break;
			}
		}
		env.putObject("unverified_claims_to_request", unverifiedClaimsToRequest);
		logSuccess("Added unverified claims to authorization request", args("unverified_claims_to_request", unverifiedClaimsToRequest));
		return env;
	}

}
