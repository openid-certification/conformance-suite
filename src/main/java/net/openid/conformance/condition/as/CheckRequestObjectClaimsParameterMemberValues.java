package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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

	// Locations of expected claims to validate.
	private static final List<String> validClaimPaths = List.of(
		"userinfo",
		"userinfo.verified_claims.verification",
		"userinfo.verified_claims.claims",
		"id_token",
		"id_token.verified_claims.verification",
		"id_token.verified_claims.claims"
	);

	// Add a claim to the list for the specified claims object.
	//
	// Parameters:
	//   claimsObject:     The claims object to be updated.
	//   claimsObjectPath: The path of the claims object (terminated with the claim name. eg. ["id_token", "name"]
	private void addClaimMemberToClaimsObject(JsonObject claimsObject, List<String> claimsObjectPath) {
		List<String> localClaimsObjectPath = new ArrayList<>(claimsObjectPath);

		// Extract the claim name from the path.
		String claim = localClaimsObjectPath.remove(localClaimsObjectPath.size() - 1);
		// Construct the claims object name from the remaining path.
		// eg. "id_token.verified_claims.verification"
		String claimsObjectPathStr = String.join(".", localClaimsObjectPath);

		// Ensure the list of claims to be updated exists.
		if (! claimsObject.has(claimsObjectPathStr)) {
			claimsObject.add(claimsObjectPathStr, new JsonArray());
		}

		JsonArray claimsArray = claimsObject.getAsJsonArray(claimsObjectPathStr);
		if (! claimsArray.contains(new JsonPrimitive(claim))) {
			claimsObject.getAsJsonArray(claimsObjectPathStr).add(claim);
		}
	}

	// Recurse through nested claims objects to identify and validate individual claims.
	//
	// Parameters:
	//   claimsObject:	  The claims object to be checked.
	//   claimsObjectPath:    The path of the claims object. eg. ["id_token", "verified_claims"]
	//   allMemberClaims:     A store for all identified claims.
	//   invalidMemberClaims: A store for invalid claims.
	private void checkClaimsObject(JsonObject claimsObject, List<String> claimsObjectPath, JsonObject allMemberClaims, JsonObject invalidMemberClaims) {

		// In the case where the claim value is an empty object eg. 'id_token.iss : {}' no validation need
		// be performed, but the claim name needs to be added to the claim object path so the claim is
		// logged correctly as 'id_token.iss'.
		if (claimsObject.keySet().isEmpty()) {
			ArrayList<String> localClaimsObjectPath = new ArrayList<>(claimsObjectPath);
			addClaimMemberToClaimsObject(allMemberClaims, localClaimsObjectPath);
			return;
		}

		// Process all claims in the claims object.
		for (String key : claimsObject.keySet()) {
			ArrayList<String> localClaimsObjectPath = new ArrayList<>(claimsObjectPath);

			// Recurse down though nested claims objects.
			if (claimsObject.get(key) instanceof JsonObject) {
				// Add the claim name to the claim object path.
				localClaimsObjectPath.add(key);

				checkClaimsObject(claimsObject.get(key).getAsJsonObject(), localClaimsObjectPath, allMemberClaims, invalidMemberClaims);
				continue;
			}

			// At this point we expect to be iterating through a claims object containing a set of claims
			// with non object values eg. '"essential" : true'. In this case we validate the claim name, but log
			// the parent object which is currently contained in the claimsObjectPath.
			//
			// So. for 'id_token.iss : { "essential" : true }' the claim is logged as "id_token.iss" with the
			// "essential" being validated.

			// In the case where the claim value is a null object eg. 'id_token.iss : null' no validation need
			// be performed, but the claim name needs to be added to the claim object path so the claim is
			// logged correctly as 'id_token.iss'.
			if (claimsObject.get(key).isJsonNull()) {
				// Add the claim name to the claims object path and log it's presence.
				localClaimsObjectPath.add(key);
				addClaimMemberToClaimsObject(allMemberClaims, localClaimsObjectPath);
				continue;
			}

			// In the case where the claim in an expected location does not contain the expected object or null eg.
			// 'id_token.iss : "12345678"'. Add the claim name to the claim object path so the claim is logged correctly as
			// 'id_token.iss'.
			for (String validPath : validClaimPaths) {
				String path = String.join(".", localClaimsObjectPath);

				if (validPath.startsWith(path)) {
					localClaimsObjectPath.add(key);
				}
			}
			addClaimMemberToClaimsObject(allMemberClaims, localClaimsObjectPath);

			if (! validValuekeys.contains(key)) {
				addClaimMemberToClaimsObject(invalidMemberClaims, localClaimsObjectPath);
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
