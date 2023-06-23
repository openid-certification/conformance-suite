package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckRequestObjectClaimsParameterMemberValues extends AbstractCondition {
	// https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter
	private static final List<String> expectedRequestObjectClaimsParams = List.of(
		"userinfo",
		"id_token"
	);

	// https://openid.net/specs/openid-connect-core-1_0.html#IndividualClaimsRequests
	private static final List<String> validValuekeys = List.of(
		"essential",
		"value",
		"values"
	);

	// For success/failure result purposes we maintain allMemberClaims/invalidMemberClaims maps
	//
	// eg. allMemberClaims:
	//       id_token
	//	   given_name
	//	   ...
	//       userinfo
	//	   family_name
	//	   ...
	private void addClaimMemberToMap(String claim, String claimMember, Map<String, List<String>> map) {
		List<String> claimsList;

		if (map.containsKey(claim)) {
			claimsList = map.get(claim);
		}
		else {
			claimsList = new ArrayList<>();
		}

		claimsList.add(claimMember);
		map.put(claim, claimsList);
	}

	@Override
	@PreEnvironment(required = { "authorization_request_object" })
	public Environment evaluate(Environment env) {

		HashMap<String, List<String>> allMemberClaims = new HashMap<>();
		HashMap<String, List<String>> invalidMemberClaims = new HashMap<>();

		JsonObject requestObjectClaimsParameter = env.getElementFromObject("authorization_request_object", "claims.claims").getAsJsonObject();

		if (requestObjectClaimsParameter == null || requestObjectClaimsParameter.size() == 0) {
			logSuccess("authorization_request_object.claims.claims does not exist or is empty");
			return env;
		}

		// Process the top level claims parameters.
		// eg. 'userinfo'
		for (String claim : requestObjectClaimsParameter.keySet()) {
			// Ignore unexpected claims parameters
			if (! expectedRequestObjectClaimsParams.contains(claim)) {
				continue;
			}

			JsonElement claimObject = requestObjectClaimsParameter.get(claim);

			if (claimObject instanceof JsonObject) {
				// Process the claims parameter members.
				// eg. 'given_name'
				for (String member: claimObject.getAsJsonObject().keySet()) {

					JsonElement claimValue = claimObject.getAsJsonObject().get(member);

					addClaimMemberToMap(claim, member, allMemberClaims);

					// The member value must either be null or an object containing only the expected keys.
					if (claimValue instanceof JsonObject) {
						for (String valueKey: claimValue.getAsJsonObject().keySet()) {
							if (! validValuekeys.contains(valueKey)) {
								addClaimMemberToMap(claim, member, invalidMemberClaims);
							}
						}
					}
					// Null is a valid claim value, anything else is not.
					else if (! claimValue.isJsonNull()) {
						addClaimMemberToMap(claim, member, invalidMemberClaims);
					}
				}
			}
		}

		if (invalidMemberClaims.isEmpty()) {
			logSuccess("the authorization request id_token/userinfo claims contain only claim members with valid values", args("claim_members", allMemberClaims));
		}
		else {
			throw error("the authorization request id_token/userinfo claims contain claim members with invalid values", args("claim_members", allMemberClaims, "invalid_claims", invalidMemberClaims));
		}

		return env;
	}
}
