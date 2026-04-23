package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates that the mdoc credential's disclosed [namespace, elementIdentifier] pairs cover
 * every claim the DCQL query requested.
 */
public class ValidateDisclosedMdocClaimsMatchDcqlQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"mdoc", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {

		String credentialId = env.getString("credential_id");
		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonObject matchingCredential = DcqlQueryUtils.findCredentialById(dcqlQuery, credentialId);

		if (matchingCredential == null) {
			throw error("No DCQL credential entry found matching credential_id",
				args("credential_id", credentialId, "dcql_query", dcqlQuery));
		}

		// TODO: This currently inherits DcqlQueryUtils' "flatten all claims" behavior and therefore
		// does not yet honor DCQL claim_sets semantics when deciding which claims are required.
		Set<List<String>> requestedClaimPaths = DcqlQueryUtils.extractClaimPathsFromCredential(matchingCredential);
		if (requestedClaimPaths.isEmpty()) {
			log("DCQL credential entry has no claims, skipping claims validation");
			return env;
		}

		Set<List<String>> disclosedPaths = DcqlQueryUtils.extractDisclosedMdocPaths(env);

		List<List<String>> missingClaimPaths = new ArrayList<>();
		for (List<String> claimPath : requestedClaimPaths) {
			if (!disclosedPaths.contains(claimPath)) {
				missingClaimPaths.add(claimPath);
			}
		}

		if (!missingClaimPaths.isEmpty()) {
			throw error("mdoc credential is missing claims that were requested in the DCQL query",
				args("missing_claim_paths", missingClaimPaths,
					"requested_claim_paths", requestedClaimPaths,
					"disclosed_paths", disclosedPaths,
					"credential_id", credentialId));
		}

		logSuccess("All DCQL-requested claims are present in the disclosed mdoc credential",
			args("requested_claim_paths", requestedClaimPaths,
				"disclosed_paths", disclosedPaths));
		return env;
	}
}
