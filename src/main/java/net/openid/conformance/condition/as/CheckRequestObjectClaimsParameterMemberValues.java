package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

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

	// Add a claim to the list for the specified claims object.
	//
	// Parameters:
	//   claimsObject:     The claims object to be updated.
	//   claimsObjectPath: The path of the claims object (terminated with the claim name. eg ["id_token", "name"]
	private void addClaimMemberToClaimsObject(JsonObject claimsObject, List<String> claimsObjectPath) {
		// Extract the claim name from the path.
		String claim = claimsObjectPath.remove(claimsObjectPath.size() - 1);
		// Construct the claims object name from the remaining path.
		// eg. "id_token.verified_claims.verification"
		String claimsObjectPathStr = String.join(".", claimsObjectPath);

		// Ensure the list of claims to be updated exists.
		if (! claimsObject.has(claimsObjectPathStr)) {
			claimsObject.add(claimsObjectPathStr, new JsonArray());
		}

		claimsObject.getAsJsonArray(claimsObjectPathStr).add(claim);
	}

	// Recurse through nested claims objects to identify and validate individual claims.
	//
	// Parameters:
	//   claimsObject:	The claims object to be checked.
	//   claimsObjectPath:    The path of the claims object. eg. ["id_token", "verified_claims"]
	//   allMemberClaims:     A store for all identified claims.
	//   invalidMemberClaims: A store for all invalid claims.
	private void checkClaimsObject(JsonObject claimsObject, List<String> claimsObjectPath, JsonObject allMemberClaims, JsonObject invalidMemberClaims) {

		// Process all entries in the claims object.
		for (String key : claimsObject.keySet()) {
			ArrayList<String> localClaimsObjectPath = new ArrayList<>(claimsObjectPath);

			// Recurse down though claims that are objects.
			if (claimsObject.get(key) instanceof JsonObject) {
				// Add the claim name to the claim object path.
				localClaimsObjectPath.add(key);

				checkClaimsObject(claimsObject.get(key).getAsJsonObject(), localClaimsObjectPath, allMemberClaims, invalidMemberClaims);
				continue;
			}

			// The claim has a value. Add to the all claims store.
			if (claimsObject.get(key).isJsonNull()) {
				// If the claim has a JsonNull value the claim name must be added to the
				// claim object path. Otherwise the recursion through claims objects will
				// already have added the claim to the path.
				localClaimsObjectPath.add(key);
			}
			addClaimMemberToClaimsObject(allMemberClaims, localClaimsObjectPath);

			if (! claimsObject.get(key).isJsonNull()) {
				if (! validValuekeys.contains(key)) {
					addClaimMemberToClaimsObject(invalidMemberClaims, claimsObjectPath);
				}
			}
		}
	}

	@Override
	@PreEnvironment(required = { "authorization_request_object" })
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaimsParameter = env.getElementFromObject("authorization_request_object", "claims.claims").getAsJsonObject();

		if (requestObjectClaimsParameter == null || requestObjectClaimsParameter.size() == 0) {
			logSuccess("authorization_request_object.claims.claims does not exist or is empty");
			return env;
		}

		JsonObject allMemberClaims = new JsonObject();
		JsonObject invalidMemberClaims = new JsonObject();

		// Process the top level claims parameters.
		// eg. 'userinfo'
		for (String claim : requestObjectClaimsParameter.keySet()) {
			// Ignore unexpected claims parameters
			if (! expectedRequestObjectClaimsParams.contains(claim)) {
				continue;
			}

			ArrayList<String> claimsObjectPath = new ArrayList<>(List.of(claim));
			checkClaimsObject(requestObjectClaimsParameter.getAsJsonObject(claim), claimsObjectPath, allMemberClaims, invalidMemberClaims);
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
