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
		JsonElement verifiedClaimsSupportedElement = env.getElementFromObject("server", "claims_in_verified_claims_supported");
		if(verifiedClaimsSupportedElement==null) {
			throw error("claims_in_verified_claims_supported element in server configuration is required for this test");
		}
		//skip sub and anything that exists in claims_in_verified_claims_supported and only add 1 or 2 (even if more than 2 claims match)
		JsonArray claimsSupportedArray = claimsSupportedElement.getAsJsonArray();
		JsonArray verifiedClaimsSupportedArray = verifiedClaimsSupportedElement.getAsJsonArray();
		int matchedClaimCount = 0;
		JsonObject unverifiedClaimsToRequest = new JsonObject();
		for(JsonElement claimName : claimsSupportedArray) {
			if(verifiedClaimsSupportedArray.contains(claimName)) {
				continue;
			}
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
			if(matchedClaimCount>1) {
				break;
			}
		}
		env.putObject("unverified_claims_to_request", unverifiedClaimsToRequest);
		logSuccess("Added unverified claims to authorization request", args("unverified_claims_to_request", unverifiedClaimsToRequest));
		return env;
	}

}
