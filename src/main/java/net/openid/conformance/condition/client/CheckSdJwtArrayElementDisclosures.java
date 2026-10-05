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
 * Checks the presentation of a top-level array claim that the DCQL query selects with a two-element
 * claims path pointer: {@code [name, 0]} (the first element) or {@code [name, null]} (all elements),
 * as defined in OID4VP section 7.1.
 *
 * What a verifier can observe is limited by two things the wallet does not control. The issuer
 * decides whether the array elements are individually selectively disclosable, and SD-JWT allows
 * decoy digests among array elements, so a placeholder with no matching disclosure is either an
 * element the wallet withheld or a decoy. The checks are therefore:
 *
 * - in both modes, the array claim is present after applying the disclosures and has at least one element;
 * - for index 0, no individually selectively disclosable element is disclosed other than the first
 *   visible element of the array. The first visible element is the only candidate for index 0, as any
 *   unresolved placeholders before it may be decoys. OID4VP section 6.4: "Wallets MUST NOT send
 *   selectively disclosable claims that have not been selected";
 * - for null, unresolved placeholders are logged but not treated as a failure.
 *
 * Only index 0 is supported: for any other index the position of the selected element cannot be
 * established from the presentation when placeholders are unresolved. Even for index 0 this
 * condition cannot tell whether the element presented is really the first one: a wallet that
 * withholds the real first element and discloses a later one is indistinguishable from an issuer
 * that put a decoy digest first.
 *
 * Disclosure digests are computed with the hash algorithm named in the credential's _sd_alg claim
 * (sha-256 when absent). To avoid passing without having checked anything, the condition fails if
 * the presented elements cannot all be accounted for as plain elements or matched element disclosures.
 */
public class CheckSdJwtArrayElementDisclosures extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {

		String credentialId = env.getString("credential_id");
		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonObject credentialQuery = DcqlQueryUtils.findCredentialById(dcqlQuery, credentialId);
		if (credentialQuery == null) {
			throw error("No DCQL credential entry found matching credential_id",
				args("credential_id", credentialId, "dcql_query", dcqlQuery));
		}

		JsonObject decoded = DcqlQueryUtils.getDecodedSdJwtClaims(env);
		if (decoded == null) {
			throw error("No decoded SD-JWT claims found in environment");
		}
		JsonObject rawPayload = env.getElementFromObject("sdjwt", "credential.claims").getAsJsonObject();
		JsonArray disclosures = env.getElementFromObject("sdjwt", "disclosures").getAsJsonArray();

		// the hash algorithm the issuer used for the disclosure digests; sha-256 when absent (SD-JWT section 4.1.1)
		String sdAlg = "sha-256";
		JsonElement sdAlgEl = rawPayload.get("_sd_alg");
		if (sdAlgEl != null) {
			if (!OIDFJSON.isString(sdAlgEl)) {
				throw error("The _sd_alg claim in the issuer-signed JWT is not a string", args("_sd_alg", sdAlgEl));
			}
			sdAlg = OIDFJSON.getString(sdAlgEl);
		}

		JsonArray claims = credentialQuery.getAsJsonArray("claims");
		int checked = 0;
		if (claims != null) {
			for (JsonElement claimEl : claims) {
				JsonArray path = claimEl.getAsJsonObject().getAsJsonArray("path");
				if (path == null || path.size() != 2 || !OIDFJSON.isString(path.get(0))) {
					continue;
				}
				JsonElement selector = path.get(1);
				boolean allElements = selector.isJsonNull();
				boolean byIndex = selector.isJsonPrimitive() && selector.getAsJsonPrimitive().isNumber();
				if (!allElements && !byIndex) {
					continue;
				}
				if (byIndex && OIDFJSON.getInt(selector) != 0) {
					throw error("This check only supports selecting array index 0 or all elements (null)",
						args("claim_path", path));
				}
				checkArrayClaim(OIDFJSON.getString(path.get(0)), allElements, path, rawPayload, disclosures, decoded, sdAlg);
				checked++;
			}
		}

		if (checked == 0) {
			throw error("The DCQL query does not select array elements of a top-level claim with a path of the form [name, 0] or [name, null]",
				args("dcql_query", dcqlQuery));
		}

		return env;
	}

	private void checkArrayClaim(String claimName, boolean allElements, JsonArray path,
			JsonObject rawPayload, JsonArray disclosures, JsonObject decoded, String sdAlg) {

		JsonElement decodedValue = decoded.get(claimName);
		if (decodedValue == null || !decodedValue.isJsonArray()) {
			throw error("The array claim selected by the DCQL query is not present as an array in the presented credential",
				args("claim_path", path, "decoded_credential", decoded));
		}
		if (decodedValue.getAsJsonArray().isEmpty()) {
			throw error("The array claim selected by the DCQL query was presented without any elements",
				args("claim_path", path, "decoded_credential", decoded));
		}

		JsonArray rawArray = findRawArray(claimName, rawPayload, disclosures);
		if (rawArray == null) {
			throw error("Could not locate the array claim in the issuer-signed JWT or in an object property disclosure",
				args("claim_path", path));
		}

		Set<String> presentedElementDigests = new HashSet<>();
		for (JsonElement disclosureEl : disclosures) {
			String disclosureJson = OIDFJSON.getString(disclosureEl);
			if (JsonParser.parseString(disclosureJson).getAsJsonArray().size() == 2) {
				try {
					presentedElementDigests.add(digestOf(disclosureJson, sdAlg));
				} catch (IllegalArgumentException e) {
					throw error("The conformance suite does not support the disclosure digest algorithm named in the credential's _sd_alg claim, "
							+ "so the array element disclosures cannot be checked",
						e, args("_sd_alg", sdAlg));
				}
			}
		}

		int firstVisible = -1;
		List<Integer> disclosedSdPositions = new ArrayList<>();
		int plainElements = 0;
		int unresolvedPlaceholders = 0;
		for (int i = 0; i < rawArray.size(); i++) {
			String placeholderDigest = placeholderDigest(rawArray.get(i));
			if (placeholderDigest == null) {
				// not selectively disclosable: always visible to the verifier
				plainElements++;
				if (firstVisible < 0) {
					firstVisible = i;
				}
			} else if (presentedElementDigests.contains(placeholderDigest)) {
				disclosedSdPositions.add(i);
				if (firstVisible < 0) {
					firstVisible = i;
				}
			} else {
				unresolvedPlaceholders++;
			}
		}

		// Every element the wallet presented must be either a plain element or a placeholder this
		// condition matched to a disclosure. If not, the digest matching here has gone wrong, and
		// carrying on would pass without checking anything.
		int presentedElements = decodedValue.getAsJsonArray().size();
		if (plainElements + disclosedSdPositions.size() != presentedElements) {
			throw error("Could not account for every presented element of the array claim, so the array element disclosures cannot be checked",
				args("claim_path", path,
					"_sd_alg", sdAlg,
					"presented_element_count", presentedElements,
					"plain_elements", plainElements,
					"matched_element_disclosures", disclosedSdPositions.size(),
					"issuer_signed_array", rawArray));
		}

		if (!allElements) {
			List<Integer> unselected = new ArrayList<>(disclosedSdPositions);
			unselected.remove(Integer.valueOf(firstVisible));
			if (!unselected.isEmpty()) {
				throw error("Wallet disclosed selectively disclosable array elements other than the one selected by index 0. "
						+ "OID4VP §6.4: wallets MUST NOT send selectively disclosable claims that have not been selected.",
					args("claim_path", path,
						"first_visible_position", firstVisible,
						"unselected_disclosed_positions", unselected,
						"issuer_signed_array", rawArray));
			}
			logSuccess("Wallet presented the array claim and disclosed no selectively disclosable element other than the first visible one",
				args("claim_path", path,
					"first_visible_position", firstVisible,
					"placeholders_without_disclosure", unresolvedPlaceholders));
		} else {
			logSuccess("Wallet presented the array claim with at least one element. Placeholders without a disclosure "
					+ "cannot be distinguished from decoy digests, so they are reported here but are not a failure.",
				args("claim_path", path,
					"presented_element_count", decodedValue.getAsJsonArray().size(),
					"placeholders_without_disclosure", unresolvedPlaceholders));
		}
	}

	/**
	 * The array as the issuer signed it: either a member of the JWT payload, or the value of the
	 * object property disclosure that reveals the claim when the array itself is selectively disclosable.
	 */
	private JsonArray findRawArray(String claimName, JsonObject rawPayload, JsonArray disclosures) {
		JsonElement inPayload = rawPayload.get(claimName);
		if (inPayload != null && inPayload.isJsonArray()) {
			return inPayload.getAsJsonArray();
		}
		for (JsonElement disclosureEl : disclosures) {
			JsonArray disclosure = JsonParser.parseString(OIDFJSON.getString(disclosureEl)).getAsJsonArray();
			if (disclosure.size() == 3 && OIDFJSON.isString(disclosure.get(1))
					&& claimName.equals(OIDFJSON.getString(disclosure.get(1)))
					&& disclosure.get(2).isJsonArray()) {
				return disclosure.get(2).getAsJsonArray();
			}
		}
		return null;
	}

	/** The digest if the element is an SD-JWT array element placeholder {"...": "<digest>"}, else null. */
	private static String placeholderDigest(JsonElement element) {
		if (!element.isJsonObject()) {
			return null;
		}
		JsonObject obj = element.getAsJsonObject();
		if (obj.size() != 1 || !obj.has("...") || !OIDFJSON.isString(obj.get("..."))) {
			return null;
		}
		return OIDFJSON.getString(obj.get("..."));
	}

	/**
	 * The digest of a disclosure, computed over its base64url encoding with the credential's
	 * _sd_alg (SD-JWT section 4.2.3). The environment holds the decoded JSON text of each
	 * disclosure, so it is re-encoded here, in the same way as {@link CheckOnlyRequestedClaimsDisclosed}
	 * does; unlike that condition this one honours _sd_alg rather than assuming sha-256.
	 *
	 * @throws IllegalArgumentException if the hash algorithm is not one the SD-JWT library supports
	 */
	private static String digestOf(String disclosureJson, String sdAlg) {
		String base64url = Base64.getUrlEncoder().withoutPadding()
			.encodeToString(disclosureJson.getBytes(StandardCharsets.UTF_8));
		return Disclosure.parse(base64url).digest(sdAlg);
	}

}
