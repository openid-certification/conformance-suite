package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared utilities for DCQL query processing.
 */
public class DcqlQueryUtils {

	private DcqlQueryUtils() {
	}

	/**
	 * Find a credential entry in the DCQL query matching the given credential ID.
	 *
	 * @return the matching credential JsonObject, or null if not found
	 */
	public static JsonObject findCredentialById(JsonObject dcqlQuery, String credentialId) {
		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");
		if (credentials == null) {
			return null;
		}
		for (JsonElement credEl : credentials) {
			JsonObject cred = credEl.getAsJsonObject();
			if (cred.has("id") && credentialId.equals(OIDFJSON.getString(cred.get("id")))) {
				return cred;
			}
		}
		return null;
	}

	/**
	 * Extract the set of claim names that may appear in SD-JWT disclosures for the requested paths.
	 * This includes both leaf claim names and any ancestor object names needed to disclose nested paths.
	 *
	 * Limitation: this is a name-only flattening. A path like {@code ["address", "street_address"]}
	 * yields {@code {"address", "street_address"}}, so a top-level {@code street_address} disclosure
	 * would be considered "requested" even though it isn't structurally inside {@code address}. In the
	 * wallet-test scenario the credential is issued by an external issuer the suite does not control,
	 * so the structural location of a disclosed claim cannot be reliably verified here. Use this for
	 * over-approximating the set of acceptable claim names; do not rely on it for strict containment.
	 *
	 * TODO: This currently flattens the credential entry's entire claims array and does not
	 * implement DCQL claim_sets semantics. Where claim_sets are present, callers should ideally
	 * derive the effective requested claims from a satisfiable set instead of treating every
	 * declared claim as simultaneously required/requested.
	 */
	public static Set<String> extractRequestedClaimNames(JsonObject dcqlQuery) {
		Set<String> claims = new HashSet<>();
		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");
		if (credentials == null) {
			return claims;
		}
		for (JsonElement credEl : credentials) {
			extractClaimNamesFromCredential(credEl.getAsJsonObject(), claims);
		}
		return claims;
	}

	/**
	 * Extract the set of full claim paths requested across all credentials in the DCQL query.
	 */
	public static Set<List<String>> extractRequestedClaimPaths(JsonObject dcqlQuery) {
		Set<List<String>> claimPaths = new HashSet<>();
		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");
		if (credentials == null) {
			return claimPaths;
		}
		for (JsonElement credEl : credentials) {
			extractClaimPathsFromCredential(credEl.getAsJsonObject(), claimPaths);
		}
		return claimPaths;
	}

	/**
	 * Extract the set of claim names that may appear in SD-JWT disclosures for the requested paths
	 * of a specific credential entry.
	 */
	public static Set<String> extractClaimNamesFromCredential(JsonObject credential) {
		Set<String> claims = new HashSet<>();
		extractClaimNamesFromCredential(credential, claims);
		return claims;
	}

	/**
	 * Extract the set of full claim paths requested for a specific credential entry.
	 *
	 * TODO: This currently models "all claims listed under claims". It does not yet account for
	 * claim_sets, which can make only a subset of those claim ids effectively required.
	 */
	public static Set<List<String>> extractClaimPathsFromCredential(JsonObject credential) {
		Set<List<String>> claimPaths = new HashSet<>();
		extractClaimPathsFromCredential(credential, claimPaths);
		return claimPaths;
	}

	/**
	 * Check whether a full DCQL claim path is present in the decoded credential payload.
	 *
	 * TODO: Path handling here is intentionally limited to string-only object traversal. DCQL path
	 * elements may also contain integers and null, so array-index and wildcard-like semantics are
	 * not implemented yet.
	 */
	public static boolean isClaimPathPresent(JsonObject decoded, List<String> claimPath) {
		JsonElement current = decoded;
		for (String segment : claimPath) {
			if (current == null || !current.isJsonObject()) {
				return false;
			}
			current = current.getAsJsonObject().get(segment);
		}
		return current != null && !current.isJsonNull();
	}

	/**
	 * Find all decoded claim paths whose terminal member name and value match the supplied disclosure.
	 */
	public static Set<List<String>> findMatchingClaimPaths(JsonObject decoded, String claimName, JsonElement claimValue) {
		Set<List<String>> matchingPaths = new HashSet<>();
		findMatchingClaimPaths(decoded, List.of(), claimName, claimValue, matchingPaths);
		return matchingPaths;
	}

	/**
	 * Check whether the supplied path is either directly requested or is an ancestor of a requested path.
	 * Ancestor matching is needed because nested SD-JWT claims may require disclosing the containing object.
	 */
	public static boolean isRequestedPathOrAncestor(Set<List<String>> requestedPaths, List<String> candidatePath) {
		for (List<String> requestedPath : requestedPaths) {
			if (requestedPath.equals(candidatePath)) {
				return true;
			}
			if (requestedPath.size() > candidatePath.size() &&
				requestedPath.subList(0, candidatePath.size()).equals(candidatePath)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Walk an SD-JWT structure (JWT body or disclosure value) and collect all selectively-disclosable
	 * digest references — both the digest hashes inside <code>_sd</code> arrays (object property
	 * disclosures) and inside <code>{"...": digest}</code> entries (array element disclosures).
	 */
	public static void collectReferencedDigests(JsonElement value, Set<String> digests) {
		if (value == null || value.isJsonNull()) {
			return;
		}
		if (value.isJsonObject()) {
			for (var entry : value.getAsJsonObject().entrySet()) {
				String key = entry.getKey();
				JsonElement v = entry.getValue();
				if ("_sd".equals(key) && v.isJsonArray()) {
					for (JsonElement item : v.getAsJsonArray()) {
						if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
							digests.add(OIDFJSON.getString(item));
						}
					}
				} else if ("...".equals(key) && v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
					digests.add(OIDFJSON.getString(v));
				} else {
					collectReferencedDigests(v, digests);
				}
			}
			return;
		}
		if (value.isJsonArray()) {
			for (JsonElement item : value.getAsJsonArray()) {
				collectReferencedDigests(item, digests);
			}
		}
	}

	private static void extractClaimNamesFromCredential(JsonObject credential, Set<String> claims) {
		JsonArray claimsArray = credential.getAsJsonArray("claims");
		if (claimsArray == null) {
			return;
		}
		for (JsonElement claimEl : claimsArray) {
			List<String> pathSegments = extractPathSegments(claimEl.getAsJsonObject().getAsJsonArray("path"));
			claims.addAll(pathSegments);
		}
	}

	private static void extractClaimPathsFromCredential(JsonObject credential, Set<List<String>> claimPaths) {
		JsonArray claimsArray = credential.getAsJsonArray("claims");
		if (claimsArray == null) {
			return;
		}
		for (JsonElement claimEl : claimsArray) {
			List<String> pathSegments = extractPathSegments(claimEl.getAsJsonObject().getAsJsonArray("path"));
			if (!pathSegments.isEmpty()) {
				claimPaths.add(List.copyOf(pathSegments));
			}
		}
	}

	private static List<String> extractPathSegments(JsonArray path) {
		List<String> pathSegments = new ArrayList<>();
		if (path == null || path.isEmpty()) {
			return pathSegments;
		}
		// TODO: This currently drops non-string path elements. DCQL permits integers and null in
		// claim paths, but the current helper layer only preserves string object-member segments.
		for (JsonElement pathElement : path) {
			if (pathElement.isJsonPrimitive() && pathElement.getAsJsonPrimitive().isString()) {
				pathSegments.add(OIDFJSON.getString(pathElement));
			}
		}
		return pathSegments;
	}

	private static void findMatchingClaimPaths(JsonElement current, List<String> currentPath, String claimName,
		JsonElement claimValue, Set<List<String>> matchingPaths) {

		if (current == null || current.isJsonNull()) {
			return;
		}
		if (current.isJsonObject()) {
			for (var entry : current.getAsJsonObject().entrySet()) {
				List<String> nextPath = new ArrayList<>(currentPath);
				nextPath.add(entry.getKey());
				if (entry.getKey().equals(claimName) && valuesMatch(entry.getValue(), claimValue)) {
					matchingPaths.add(List.copyOf(nextPath));
				}
				findMatchingClaimPaths(entry.getValue(), nextPath, claimName, claimValue, matchingPaths);
			}
			return;
		}
		if (current.isJsonArray()) {
			for (JsonElement element : current.getAsJsonArray()) {
				findMatchingClaimPaths(element, currentPath, claimName, claimValue, matchingPaths);
			}
		}
	}

	private static boolean valuesMatch(JsonElement decodedValue, JsonElement disclosedValue) {
		if (decodedValue == null || disclosedValue == null || decodedValue.isJsonNull() || disclosedValue.isJsonNull()) {
			return false;
		}
		// Containers are accepted on type alone: post-disclosure the decoded value differs
		// from the raw disclosed value (digest references replaced with revealed values, decoy
		// elements dropped), so structural equality is the most we can check here.
		if (decodedValue.isJsonObject() && disclosedValue.isJsonObject()) {
			return true;
		}
		if (decodedValue.isJsonArray() && disclosedValue.isJsonArray()) {
			return true;
		}
		return decodedValue.equals(disclosedValue);
	}
}
