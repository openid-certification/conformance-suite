package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
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

		Set<List<String>> allClaimPaths = DcqlQueryUtils.extractClaimPathsFromCredential(matchingCredential);

		if (allClaimPaths.isEmpty()) {
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

		JsonObject decoded = DcqlQueryUtils.getDecodedSdJwtClaims(env);
		if (decoded == null) {
			throw error("No decoded SD-JWT claims found in environment");
		}

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
		List<DisclosedClaim> disclosedClaims = new ArrayList<>();
		List<String> arrayElementRaws = new ArrayList<>();
		for (JsonElement disclosureEl : disclosures) {
			String disclosureStr = OIDFJSON.getString(disclosureEl);
			JsonArray disclosure = JsonParser.parseString(disclosureStr).getAsJsonArray();
			if (disclosure.size() >= 3) {
				String name = OIDFJSON.getString(disclosure.get(1));
				JsonElement value = disclosure.get(2);
				DcqlQueryUtils.collectReferencedDigests(value, referencedDigests);
				disclosedClaims.add(new DisclosedClaim(name, DcqlQueryUtils.findMatchingClaimPaths(decoded, name, value)));
			} else if (disclosure.size() == 2) {
				arrayElementRaws.add(disclosureStr);
			}
		}

		// With claim_sets the Verifier requests one combination of claims, so the disclosures must
		// fit within a single option that the presentation satisfies. Where no such option exists
		// the disclosures are reported against the most preferred satisfied option; if none is
		// satisfied (which ValidateDisclosedClaimsMatchDcqlQuery reports) only disclosures outside
		// every listed claim are reported.
		List<Set<List<String>>> satisfiedOptions = DcqlQueryUtils.extractClaimSetOptions(matchingCredential).stream()
			.filter(option -> DcqlQueryUtils.isClaimSetOptionPresent(decoded, option))
			.toList();
		Set<List<String>> requestedClaimPaths = satisfiedOptions.stream()
			.filter(option -> findUnrequestedDisclosures(disclosedClaims, option).isEmpty())
			.findFirst()
			.orElse(satisfiedOptions.isEmpty() ? allClaimPaths : satisfiedOptions.get(0));
		List<String> unrequestedDisclosures = findUnrequestedDisclosures(disclosedClaims, requestedClaimPaths);

		// Every object-property disclosure has been scanned above, so referencedDigests now holds
		// every digest reachable via an object property and the orphan check can run.

		// the digests in the credential were made with its _sd_alg, so the comparison must use it too
		String sdAlg = ValidateSdJwtKbSdHash.getSdAlg(env);
		List<String> orphanArrayElementDisclosures = new ArrayList<>();
		for (String raw : arrayElementRaws) {
			// The digest for a disclosure is computed over the base64url-encoded disclosure
			// bytes (SD-JWT §4.2.3). Env storage holds the decoded JSON form, so re-encode it
			// and hash that, so the digest covers the original byte sequence — avoids
			// lossy Gson round-trips (e.g. integer array elements coerced to Double).
			String base64url = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
			String digest;
			try {
				digest = ValidateSdJwtKbSdHash.calculateDigest(base64url, sdAlg);
			} catch (NoSuchAlgorithmException e) {
				throw error("The credential's _sd_alg claim is not a hash algorithm the conformance suite supports, "
						+ "so array element disclosures cannot be checked",
					e, args("_sd_alg", sdAlg, "supported", ValidateSdJwtKbSdHash.supportedSdAlgs()));
			}
			if (!referencedDigests.contains(digest)) {
				orphanArrayElementDisclosures.add(raw);
			}
		}

		if (!unrequestedDisclosures.isEmpty() || !orphanArrayElementDisclosures.isEmpty()) {
			throw error("Wallet disclosed claims that were not requested in the DCQL query. "
					+ "OID4VP §6.4.1: wallets MUST NOT send selectively disclosable claims that have not been selected. "
					+ "Where the query contains claim_sets, the verifier requests one of the listed combinations of "
					+ "claims, so claims outside the returned combination must not be disclosed.",
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

	/** An object property disclosure and the decoded claim paths its name and value match. */
	private record DisclosedClaim(String name, Set<List<String>> matchingPaths) {}

	private static List<String> findUnrequestedDisclosures(List<DisclosedClaim> disclosedClaims,
			Set<List<String>> requestedClaimPaths) {
		List<String> unrequestedDisclosures = new ArrayList<>();
		for (DisclosedClaim disclosed : disclosedClaims) {
			boolean requested = disclosed.matchingPaths().stream()
				.anyMatch(path -> DcqlQueryUtils.isRequestedPathAncestorOrDescendant(requestedClaimPaths, path));
			if (!requested) {
				unrequestedDisclosures.add(disclosed.name());
			}
		}
		return unrequestedDisclosures;
	}
}
