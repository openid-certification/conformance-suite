package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Checks data minimization: fails if the mdoc credential disclosed [namespace, elementIdentifier]
 * pairs that were not requested in the DCQL query.
 *
 * OID4VP §6.4.1: "Wallets MUST NOT send selectively disclosable claims that have not been selected
 * according to the rules below."
 */
public class CheckOnlyRequestedMdocElementsDisclosed extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"mdoc", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {

		String credentialId = env.getString("credential_id");
		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonObject matchingCredential = DcqlQueryUtils.findCredentialById(dcqlQuery, credentialId);

		if (matchingCredential == null) {
			log("No matching DCQL credential entry found, skipping data minimization check",
				args("credential_id", credentialId));
			return env;
		}

		Set<List<String>> disclosedPaths = DcqlQueryUtils.extractDisclosedMdocPaths(env);
		Set<List<String>> allClaimPaths = DcqlQueryUtils.extractClaimPathsFromCredential(matchingCredential);

		if (allClaimPaths.isEmpty()) {
			if (!disclosedPaths.isEmpty()) {
				throw error("Wallet disclosed selectively-disclosable mdoc elements even though the DCQL query did not request any claims. "
						+ "OID4VP §6.4.1: when claims are omitted the wallet MUST NOT return selectively-disclosable elements.",
					args("disclosed_paths", disclosedPaths,
						"credential_id", credentialId));
			}
			logSuccess("DCQL query did not request any claims and the wallet disclosed no mdoc elements",
				args("credential_id", credentialId));
			return env;
		}

		// With claim_sets the Verifier requests one combination of claims, so the disclosed elements
		// must fit within a single option that the presentation satisfies. Where no such option
		// exists the elements are reported against the most preferred satisfied option; if none is
		// satisfied (which ValidateDisclosedMdocClaimsMatchDcqlQuery reports) only elements outside
		// every listed claim are reported.
		List<Set<List<String>>> satisfiedOptions = DcqlQueryUtils.extractClaimSetOptions(matchingCredential).stream()
			.filter(disclosedPaths::containsAll)
			.toList();
		Set<List<String>> requestedClaimPaths = satisfiedOptions.stream()
			.filter(option -> findUnrequestedDisclosures(disclosedPaths, option).isEmpty())
			.findFirst()
			.orElse(satisfiedOptions.isEmpty() ? allClaimPaths : satisfiedOptions.get(0));
		List<List<String>> unrequestedDisclosures = findUnrequestedDisclosures(disclosedPaths, requestedClaimPaths);

		if (!unrequestedDisclosures.isEmpty()) {
			throw error("Wallet disclosed mdoc elements that were not requested in the DCQL query. "
					+ "OID4VP §6.4.1: wallets MUST NOT send selectively disclosable claims that have not been selected. "
					+ "Where the query contains claim_sets, the verifier requests one of the listed combinations of "
					+ "claims, so elements outside the returned combination must not be disclosed.",
				args("unrequested_disclosures", unrequestedDisclosures,
					"requested_claim_paths", requestedClaimPaths,
					"credential_id", credentialId));
		}

		logSuccess("Wallet only disclosed mdoc elements that were requested in the DCQL query",
			args("requested_claim_paths", requestedClaimPaths,
				"disclosed_paths", disclosedPaths));
		return env;
	}

	private static List<List<String>> findUnrequestedDisclosures(Set<List<String>> disclosedPaths,
			Set<List<String>> requestedClaimPaths) {
		List<List<String>> unrequestedDisclosures = new ArrayList<>();
		for (List<String> path : disclosedPaths) {
			if (!requestedClaimPaths.contains(path)) {
				unrequestedDisclosures.add(path);
			}
		}
		return unrequestedDisclosures;
	}
}
