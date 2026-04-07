package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks data minimization: warns if the wallet disclosed selectively-disclosable
 * claims that were not requested in the DCQL query.
 *
 * Only checks selectively disclosed claims (from sdjwt.disclosures), not
 * non-selectively-disclosed claims like iat, exp, vct, cnf which always appear
 * in the credential JWT regardless of the query.
 *
 * This is a WARNING because some wallets may not support per-claim selective
 * disclosure and may disclose all claims from a matching credential.
 */
public class WarnIfUnrequestedClaimsDisclosed extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {

		String credentialId = env.getString("credential_id");
		JsonObject dcqlQuery = env.getObject("dcql_query");

		// Find the matching DCQL credential entry
		Set<String> requestedClaims = new HashSet<>();
		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");
		if (credentials != null) {
			for (JsonElement credEl : credentials) {
				JsonObject cred = credEl.getAsJsonObject();
				if (cred.has("id") && credentialId.equals(OIDFJSON.getString(cred.get("id")))) {
					JsonArray claimsArray = cred.getAsJsonArray("claims");
					if (claimsArray != null) {
						for (JsonElement claimEl : claimsArray) {
							JsonArray path = claimEl.getAsJsonObject().getAsJsonArray("path");
							if (path != null && !path.isEmpty()) {
								JsonElement first = path.get(0);
								if (first.isJsonPrimitive() && first.getAsJsonPrimitive().isString()) {
									requestedClaims.add(OIDFJSON.getString(first));
								}
							}
						}
					}
					break;
				}
			}
		}

		if (requestedClaims.isEmpty()) {
			log("No claims were requested in the DCQL query, skipping data minimization check");
			return env;
		}

		// Check each selective disclosure against the requested claims
		JsonArray disclosures = env.getElementFromObject("sdjwt", "disclosures").getAsJsonArray();
		List<String> unrequestedDisclosures = new ArrayList<>();
		for (JsonElement disclosureEl : disclosures) {
			JsonArray disclosure = JsonParser.parseString(OIDFJSON.getString(disclosureEl)).getAsJsonArray();
			if (disclosure.size() >= 2) {
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
