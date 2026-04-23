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
		Set<List<String>> requestedClaimPaths = DcqlQueryUtils.extractClaimPathsFromCredential(matchingCredential);

		if (requestedClaimPaths.isEmpty()) {
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

		List<List<String>> unrequestedDisclosures = new ArrayList<>();
		for (List<String> path : disclosedPaths) {
			if (!requestedClaimPaths.contains(path)) {
				unrequestedDisclosures.add(path);
			}
		}

		if (!unrequestedDisclosures.isEmpty()) {
			throw error("Wallet disclosed mdoc elements that were not requested in the DCQL query. "
					+ "OID4VP §6.4.1: wallets MUST NOT send selectively disclosable claims that have not been selected.",
				args("unrequested_disclosures", unrequestedDisclosures,
					"requested_claim_paths", requestedClaimPaths,
					"credential_id", credentialId));
		}

		logSuccess("Wallet only disclosed mdoc elements that were requested in the DCQL query",
			args("requested_claim_paths", requestedClaimPaths,
				"disclosed_paths", disclosedPaths));
		return env;
	}
}
