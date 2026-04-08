package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
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
	 * Extract the set of top-level claim names requested across all credentials in the DCQL query.
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
	 * Extract the set of top-level claim names requested for a specific credential entry.
	 */
	public static Set<String> extractClaimNamesFromCredential(JsonObject credential) {
		Set<String> claims = new HashSet<>();
		extractClaimNamesFromCredential(credential, claims);
		return claims;
	}

	private static void extractClaimNamesFromCredential(JsonObject credential, Set<String> claims) {
		JsonArray claimsArray = credential.getAsJsonArray("claims");
		if (claimsArray == null) {
			return;
		}
		for (JsonElement claimEl : claimsArray) {
			JsonArray path = claimEl.getAsJsonObject().getAsJsonArray("path");
			if (path != null && !path.isEmpty()) {
				JsonElement first = path.get(0);
				if (first.isJsonPrimitive() && first.getAsJsonPrimitive().isString()) {
					claims.add(OIDFJSON.getString(first));
				}
			}
		}
	}
}
