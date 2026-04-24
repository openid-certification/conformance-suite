package net.openid.conformance.condition.client;

import com.authlete.sd.Disclosure;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks data minimization: fails if the wallet disclosed selectively-disclosable
 * claims that were not requested in the DCQL query.
 *
 * OID4VP §6.4.1: "Wallets MUST NOT send selectively disclosable claims that have
 * not been selected according to the rules below." When the verifier omits {@code claims},
 * the wallet MUST return only mandatory claims and no selective disclosures.
 */
public class CheckOnlyRequestedClaimsDisclosed extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {

		String credentialId = env.getString("credential_id");
		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonObject matchingCredential = DcqlQueryUtils.findCredentialById(dcqlQuery, credentialId);
		JsonArray disclosures = env.getElementFromObject("sdjwt", "disclosures").getAsJsonArray();

		if (matchingCredential == null) {
			log("No matching DCQL credential entry found, skipping data minimization check",
				args("credential_id", credentialId));
			return env;
		}

		// TODO: This treats every claim path declared under "claims" as requested, so it does not
		// yet implement DCQL claim_sets semantics. When claim_sets are present a wallet returning
		// claims from any one of the options will pass here even though only one option should be
		// honored. Matching TODOs in DcqlQueryUtils and AbstractCreateSdJwtCredential.
		Set<List<String>> requestedClaimPaths = DcqlQueryUtils.extractClaimPathsFromCredential(matchingCredential);

		if (requestedClaimPaths.isEmpty()) {
			if (!disclosures.isEmpty()) {
				throw error("Wallet disclosed selectively-disclosable claims even though the DCQL query did not request any claims. "
						+ "OID4VP §6.4.1: when claims are omitted the wallet MUST return only mandatory claims.",
					args("disclosure_count", disclosures.size(),
						"credential_id", credentialId));
			}
			logSuccess("DCQL query did not request any claims and the wallet disclosed no selectively-disclosable claims",
				args("credential_id", credentialId));
			return env;
		}

		JsonElement decodedEl = env.getElementFromObject("sdjwt", "decoded");
		if (decodedEl == null || !decodedEl.isJsonObject()) {
			throw error("No decoded SD-JWT claims found in environment");
		}
		JsonObject decoded = decodedEl.getAsJsonObject();

		// Set of digests referenced from the JWT body and from object property disclosures.
		// An array element disclosure whose digest is not in this set is orphan: nothing reveals
		// what it belongs to, so its value is leaked without context.
		Set<String> referencedDigests = new HashSet<>();
		JsonElement credentialEl = env.getElementFromObject("sdjwt", "credential");
		if (credentialEl != null) {
			DcqlQueryUtils.collectReferencedDigests(credentialEl, referencedDigests);
		}

		// Parse each disclosure once, splitting by shape:
		//   object property disclosures: [salt, claimName, value]
		//   array element disclosures:   [salt, value]
		record ObjectProperty(String name, JsonElement value) {}
		List<ObjectProperty> objectProperties = new ArrayList<>();
		List<String> arrayElementRaws = new ArrayList<>();
		for (JsonElement disclosureEl : disclosures) {
			String disclosureStr = OIDFJSON.getString(disclosureEl);
			JsonArray disclosure = JsonParser.parseString(disclosureStr).getAsJsonArray();
			if (disclosure.size() >= 3) {
				objectProperties.add(new ObjectProperty(
					OIDFJSON.getString(disclosure.get(1)),
					disclosure.get(2)));
			} else if (disclosure.size() == 2) {
				arrayElementRaws.add(disclosureStr);
			}
		}

		// Process all object-property disclosures first so referencedDigests contains every
		// digest reachable via an object property before array-element orphan checks run.
		List<String> unrequestedDisclosures = new ArrayList<>();
		for (ObjectProperty op : objectProperties) {
			DcqlQueryUtils.collectReferencedDigests(op.value(), referencedDigests);
			Set<List<String>> matchingPaths = DcqlQueryUtils.findMatchingClaimPaths(decoded, op.name(), op.value());
			boolean requested = matchingPaths.stream()
				.anyMatch(path -> DcqlQueryUtils.isRequestedPathAncestorOrDescendant(requestedClaimPaths, path));
			if (!requested) {
				unrequestedDisclosures.add(op.name());
			}
		}

		List<String> orphanArrayElementDisclosures = new ArrayList<>();
		for (String raw : arrayElementRaws) {
			// The digest for a disclosure is computed over the base64url-encoded disclosure
			// bytes (SD-JWT §4.2.3). Env storage holds the decoded JSON form, so re-encode it
			// and parse via authlete so digest() hashes the original byte sequence — avoids
			// lossy Gson round-trips (e.g. integer array elements coerced to Double).
			String base64url = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
			String digest = Disclosure.parse(base64url).digest();
			if (!referencedDigests.contains(digest)) {
				orphanArrayElementDisclosures.add(raw);
			}
		}

		if (!unrequestedDisclosures.isEmpty() || !orphanArrayElementDisclosures.isEmpty()) {
			throw error("Wallet disclosed claims that were not requested in the DCQL query. "
					+ "OID4VP §6.4.1: wallets MUST NOT send selectively disclosable claims that have not been selected.",
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
