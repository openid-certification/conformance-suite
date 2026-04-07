package net.openid.conformance.condition.client;

import com.authlete.sd.Disclosure;
import com.google.gson.Gson;
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
 */
public class WarnIfUnrequestedClaimsDisclosed extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {

		String credentialId = env.getString("credential_id");
		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonObject matchingCredential = DcqlQueryUtils.findCredentialById(dcqlQuery, credentialId);

		Set<List<String>> requestedClaimPaths = matchingCredential != null
			? DcqlQueryUtils.extractClaimPathsFromCredential(matchingCredential)
			: Set.of();

		if (requestedClaimPaths.isEmpty()) {
			log("No claims were requested in the DCQL query, skipping data minimization check");
			return env;
		}

		JsonElement decodedEl = env.getElementFromObject("sdjwt", "decoded");
		if (decodedEl == null || !decodedEl.isJsonObject()) {
			throw error("No decoded SD-JWT claims found in environment");
		}
		JsonObject decoded = decodedEl.getAsJsonObject();

		JsonArray disclosures = env.getElementFromObject("sdjwt", "disclosures").getAsJsonArray();

		// Set of digests referenced from the JWT body and from object property disclosures.
		// An array element disclosure whose digest is not in this set is orphan: nothing reveals
		// what it belongs to, so its value is leaked without context.
		Set<String> referencedDigests = new HashSet<>();
		JsonElement credentialEl = env.getElementFromObject("sdjwt", "credential");
		if (credentialEl != null) {
			DcqlQueryUtils.collectReferencedDigests(credentialEl, referencedDigests);
		}

		List<String> unrequestedDisclosures = new ArrayList<>();
		List<String> orphanArrayElementDisclosures = new ArrayList<>();
		for (JsonElement disclosureEl : disclosures) {
			String disclosureStr = OIDFJSON.getString(disclosureEl);
			JsonArray disclosure = JsonParser.parseString(disclosureStr).getAsJsonArray();
			// Object property disclosures: [salt, claimName, value]
			// Array element disclosures: [salt, value]
			if (disclosure.size() >= 3) {
				String claimName = OIDFJSON.getString(disclosure.get(1));
				JsonElement claimValue = disclosure.get(2);
				DcqlQueryUtils.collectReferencedDigests(claimValue, referencedDigests);
				Set<List<String>> matchingPaths = DcqlQueryUtils.findMatchingClaimPaths(decoded, claimName, claimValue);
				boolean requested = matchingPaths.stream()
					.anyMatch(path -> DcqlQueryUtils.isRequestedPathOrAncestor(requestedClaimPaths, path));
				if (!requested) {
					unrequestedDisclosures.add(claimName);
				}
			}
		}

		Gson gson = new Gson();
		for (JsonElement disclosureEl : disclosures) {
			String disclosureStr = OIDFJSON.getString(disclosureEl);
			JsonArray disclosure = JsonParser.parseString(disclosureStr).getAsJsonArray();
			if (disclosure.size() == 2) {
				// Reconstruct via the constructor so authlete recomputes its canonical JSON
				// encoding before hashing — defaults to SHA-256, matching the issuer.
				String salt = OIDFJSON.getString(disclosure.get(0));
				Object value = gson.fromJson(disclosure.get(1), Object.class);
				String digest = new Disclosure(salt, null, value).digest();
				if (!referencedDigests.contains(digest)) {
					orphanArrayElementDisclosures.add(disclosureStr);
				}
			}
		}

		if (!unrequestedDisclosures.isEmpty() || !orphanArrayElementDisclosures.isEmpty()) {
			throw error("Wallet disclosed claims that were not requested in the DCQL query. "
					+ "Wallets should practice data minimization and only disclose requested claims.",
				args("unrequested_disclosures", unrequestedDisclosures,
					"orphan_array_element_disclosures", orphanArrayElementDisclosures,
					"requested_claim_paths", requestedClaimPaths,
					"credential_id", credentialId));
		}

		logSuccess("Wallet only disclosed claims that were requested in the DCQL query",
			args("requested_claim_paths", requestedClaimPaths,
				"disclosure_count", disclosures.size()));
		return env;
	}
}
