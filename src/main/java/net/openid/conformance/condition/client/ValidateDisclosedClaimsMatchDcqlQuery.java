package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates that the credential's disclosed claims satisfy the DCQL query's claim requests.
 *
 * The DCQL query specifies requested claims in credentials[].claims[].path.
 * For SD-JWT, the path is a single-element array with the claim name.
 * The decoded SD-JWT should contain all requested claims.
 */
public class ValidateDisclosedClaimsMatchDcqlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {

		String credentialId = env.getString("credential_id");
		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");

		if (credentials == null) {
			throw error("DCQL query does not contain a credentials array");
		}

		// Find the credential entry matching the credential_id
		JsonObject matchingCredential = null;
		for (JsonElement credEl : credentials) {
			JsonObject cred = credEl.getAsJsonObject();
			if (cred.has("id") && credentialId.equals(OIDFJSON.getString(cred.get("id")))) {
				matchingCredential = cred;
				break;
			}
		}

		if (matchingCredential == null) {
			throw error("No DCQL credential entry found matching credential_id from VP token",
				args("credential_id", credentialId, "dcql_query", dcqlQuery));
		}

		JsonArray claimsArray = matchingCredential.getAsJsonArray("claims");
		if (claimsArray == null || claimsArray.isEmpty()) {
			log("DCQL credential entry has no claims array, skipping claims validation");
			return env;
		}

		// Get the decoded SD-JWT (contains all disclosed claims)
		JsonElement decodedEl = env.getElementFromObject("sdjwt", "decoded");
		if (decodedEl == null || !decodedEl.isJsonObject()) {
			throw error("No decoded SD-JWT claims found in environment");
		}
		JsonObject decoded = decodedEl.getAsJsonObject();
		Set<String> decodedKeys = decoded.keySet();

		// Extract requested claim names from DCQL paths
		List<String> requestedClaims = new ArrayList<>();
		for (JsonElement claimEl : claimsArray) {
			JsonObject claim = claimEl.getAsJsonObject();
			JsonArray path = claim.getAsJsonArray("path");
			if (path != null && !path.isEmpty()) {
				// For SD-JWT, the top-level claim name is the first path element
				JsonElement firstPathEl = path.get(0);
				if (firstPathEl.isJsonPrimitive() && firstPathEl.getAsJsonPrimitive().isString()) {
					requestedClaims.add(OIDFJSON.getString(firstPathEl));
				}
			}
		}

		// Check which requested claims are missing from the disclosed credential
		List<String> missingClaims = new ArrayList<>();
		for (String claimName : requestedClaims) {
			if (!decoded.has(claimName)) {
				missingClaims.add(claimName);
			}
		}

		if (!missingClaims.isEmpty()) {
			throw error("Credential is missing claims that were requested in the DCQL query",
				args("missing_claims", missingClaims,
					"requested_claims", requestedClaims,
					"disclosed_claims", decodedKeys,
					"credential_id", credentialId));
		}

		logSuccess("All DCQL-requested claims are present in the disclosed credential",
			args("requested_claims", requestedClaims,
				"disclosed_claims", decodedKeys));
		return env;
	}
}
