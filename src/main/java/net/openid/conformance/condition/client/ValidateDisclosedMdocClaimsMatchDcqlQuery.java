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
 * every claim the DCQL query requested: every claim listed in claims, or, when claim_sets is
 * present, every claim of at least one of its options (OID4VP 1.0 section 6.4.1).
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

		if (DcqlQueryUtils.extractClaimPathsFromCredential(matchingCredential).isEmpty()) {
			log("DCQL credential entry has no claims, skipping claims validation");
			return env;
		}
		List<Set<List<String>>> options = DcqlQueryUtils.extractClaimSetOptions(matchingCredential);

		Set<List<String>> disclosedPaths = DcqlQueryUtils.extractDisclosedMdocPaths(env);

		List<List<List<String>>> missingClaimPathsByOption = new ArrayList<>();
		for (Set<List<String>> option : options) {
			List<List<String>> missingClaimPaths = option.stream()
				.filter(claimPath -> !disclosedPaths.contains(claimPath))
				.toList();
			if (missingClaimPaths.isEmpty()) {
				logSuccess("All DCQL-requested claims are present in the disclosed mdoc credential",
					args("requested_claim_paths", option,
						"disclosed_paths", disclosedPaths));
				return env;
			}
			missingClaimPathsByOption.add(missingClaimPaths);
		}

		throw error("mdoc credential is missing claims that were requested in the DCQL query. Where the query "
				+ "contains claim_sets, the credential must contain every claim of at least one of the options.",
			args("missing_claim_paths_by_option", missingClaimPathsByOption,
				"claim_sets_options", options,
				"disclosed_paths", disclosedPaths,
				"credential_id", credentialId));
	}
}
