package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractValidateOpenIdStandardClaims;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckForUnexpectedOpenIdClaims extends AbstractValidateOpenIdStandardClaims {

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
		HashMap<String, List<String>> unknownMemberClaims = new HashMap<>();

		JsonObject claimsParameter = env.getElementFromObject("authorization_request_object", "claims.claims").getAsJsonObject();

		if (claimsParameter == null || claimsParameter.size() == 0) {
			logSuccess("authorization_request_object.claims.claims does not exist or is empty");
			return env;
		}

		for (String claim : claimsParameter.keySet()) {
			JsonElement claimObject = claimsParameter.get(claim);

			if (claimObject instanceof JsonObject) {
				for (String member : claimObject.getAsJsonObject().keySet()) {

					addClaimMemberToMap(claim, member, allMemberClaims);

					if (STANDARD_CLAIMS.containsKey(member)) {
						continue;
					}

					addClaimMemberToMap(claim, member, unknownMemberClaims);
				}
			}
		}

		if (unknownMemberClaims.isEmpty()) {
			logSuccess("authorization_request_object.claims.claims member objects contain only expected claims", args("claims", allMemberClaims));
		} else {
			throw error("unknown claims found in authorization_request_object.claims.claims member objects", args("claims", allMemberClaims, "unknown_claims", unknownMemberClaims));
		}

		return env;
	}
}
