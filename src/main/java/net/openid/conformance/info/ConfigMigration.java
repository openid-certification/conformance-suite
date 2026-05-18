package net.openid.conformance.info;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Read-time migration of stored test-plan / test-info config JSON to the current
 * key shape. Mirrors the runtime fallbacks added when keys were renamed so the UI
 * picks up the new shape immediately for existing stored configs — users don't
 * have to copy values across by hand when re-running an older plan.
 *
 * <p>This is transient: the DB row is not rewritten. Apply on the read path in
 * {@link TestPlanApi#getTestPlan} and {@link TestInfoApi#getTestInfo}. Remove
 * together with the runtime fallbacks after the transition window.
 */
public final class ConfigMigration {

	private ConfigMigration() {
	}

	/**
	 * OAuth2-ATCA rename: move client-attestation / key-attestation config keys
	 * out of the {@code vci.*} namespace into the dedicated {@code client_attestation.*}
	 * namespace. See the OAuth2-ATCA MR for the full rationale.
	 */
	public static void migrateLegacyClientAttestationKeys(JsonObject config) {
		if (config == null) {
			return;
		}
		moveLeafKey(config, "vci", "client_attestation_issuer",
			"client_attestation", "issuer");
		moveLeafKey(config, "vci", "client_attestation_trust_anchor",
			"client_attestation", "trust_anchor");
		moveLeafKey(config, "vci", "key_attestation_trust_anchor_pem",
			"client_attestation", "key_attestation_trust_anchor_pem");
		moveLeafKey(config, "vci", "client_attester_keys_jwks",
			"client_attestation", "attester_jwks");
		moveLeafKey(config, "vci", "key_attestation_jwks",
			"client_attestation", "key_attestation_jwks");
	}

	/**
	 * Move {@code config.{fromParent}.{fromLeaf}} to {@code config.{toParent}.{toLeaf}}.
	 *
	 * <ul>
	 *   <li>If the source leaf is absent: no-op.</li>
	 *   <li>If the destination leaf is already populated: drop the legacy source
	 *       (the runtime reader prefers the new key, so the legacy value is dead).</li>
	 *   <li>If the source parent becomes empty after removal, also remove the parent
	 *       so the UI doesn't show an empty {@code "vci": {}} object.</li>
	 * </ul>
	 */
	private static void moveLeafKey(JsonObject config,
									String fromParent, String fromLeaf,
									String toParent, String toLeaf) {
		JsonElement fromParentEl = config.get(fromParent);
		if (fromParentEl == null || !fromParentEl.isJsonObject()) {
			return;
		}
		JsonObject fromParentObj = fromParentEl.getAsJsonObject();
		if (!fromParentObj.has(fromLeaf)) {
			return;
		}
		JsonElement legacyValue = fromParentObj.remove(fromLeaf);
		if (fromParentObj.isEmpty()) {
			config.remove(fromParent);
		}

		JsonObject toParentObj;
		JsonElement toParentEl = config.get(toParent);
		if (toParentEl != null && toParentEl.isJsonObject()) {
			toParentObj = toParentEl.getAsJsonObject();
		} else {
			toParentObj = new JsonObject();
			config.add(toParent, toParentObj);
		}
		if (toParentObj.has(toLeaf)) {
			// Destination already wins; drop the legacy value silently.
			return;
		}
		toParentObj.add(toLeaf, legacyValue);
	}
}
