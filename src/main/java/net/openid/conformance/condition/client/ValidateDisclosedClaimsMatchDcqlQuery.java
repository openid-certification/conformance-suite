package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates that the credential's disclosed claims satisfy the DCQL query's claim requests.
 */
public class ValidateDisclosedClaimsMatchDcqlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {

		String credentialId = env.getString("credential_id");
		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonObject matchingCredential = DcqlQueryUtils.findCredentialById(dcqlQuery, credentialId);

		if (matchingCredential == null) {
			throw error("No DCQL credential entry found matching credential_id",
				args("credential_id", credentialId, "dcql_query", dcqlQuery));
		}

		Set<List<String>> requestedClaimPaths = DcqlQueryUtils.extractClaimPathsFromCredential(matchingCredential);
		if (requestedClaimPaths.isEmpty()) {
			log("DCQL credential entry has no claims, skipping claims validation");
			return env;
		}

		JsonElement decodedEl = env.getElementFromObject("sdjwt", "decoded");
		if (decodedEl == null || !decodedEl.isJsonObject()) {
			throw error("No decoded SD-JWT claims found in environment");
		}
		JsonObject decoded = decodedEl.getAsJsonObject();

		List<List<String>> missingClaimPaths = new ArrayList<>();
		for (List<String> claimPath : requestedClaimPaths) {
			if (!DcqlQueryUtils.isClaimPathPresent(decoded, claimPath)) {
				missingClaimPaths.add(claimPath);
			}
		}

		if (!missingClaimPaths.isEmpty()) {
			throw error("Credential is missing claims that were requested in the DCQL query",
				args("missing_claim_paths", missingClaimPaths,
					"requested_claim_paths", requestedClaimPaths,
					"decoded_credential", decoded,
					"credential_id", credentialId));
		}

		logSuccess("All DCQL-requested claims are present in the disclosed credential",
			args("requested_claim_paths", requestedClaimPaths,
				"decoded_credential", decoded));
		return env;
	}
}
