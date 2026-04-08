package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Checks data minimization: warns if the wallet disclosed selectively-disclosable
 * claims that were not requested in the DCQL query.
 */
public class WarnIfUnrequestedClaimsDisclosed extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {

		String credentialId = env.getString("credential_id");
		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonObject matchingCredential = DcqlQueryUtils.findCredentialById(dcqlQuery, credentialId);

		Set<String> requestedClaims = matchingCredential != null
			? DcqlQueryUtils.extractClaimNamesFromCredential(matchingCredential)
			: Set.of();

		if (requestedClaims.isEmpty()) {
			log("No claims were requested in the DCQL query, skipping data minimization check");
			return env;
		}

		JsonArray disclosures = env.getElementFromObject("sdjwt", "disclosures").getAsJsonArray();
		List<String> unrequestedDisclosures = new ArrayList<>();
		for (var disclosureEl : disclosures) {
			JsonArray disclosure = JsonParser.parseString(OIDFJSON.getString(disclosureEl)).getAsJsonArray();
			// Object property disclosures have 3 elements: [salt, claimName, value]
			// Array element disclosures have 2 elements: [salt, value] — skip these
			if (disclosure.size() >= 3) {
				String claimName = OIDFJSON.getString(disclosure.get(1));
				if (!requestedClaims.contains(claimName)) {
					unrequestedDisclosures.add(claimName);
				}
			}
		}

		if (!unrequestedDisclosures.isEmpty()) {
			throw error("Wallet disclosed claims that were not requested in the DCQL query. "
					+ "Wallets should practice data minimization and only disclose requested claims.",
				args("unrequested_disclosures", unrequestedDisclosures,
					"requested_claims", requestedClaims,
					"credential_id", credentialId));
		}

		logSuccess("Wallet only disclosed claims that were requested in the DCQL query",
			args("requested_claims", requestedClaims,
				"disclosure_count", disclosures.size()));
		return env;
	}
}
