package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks that the verifier did not reuse a response-encryption public key (from
 * {@code client_metadata.jwks}) across Authorization Requests. HAIP requires verifiers
 * to supply an ephemeral encryption key specific to each Authorization Request
 * (OID4VP 1.0 Final &sect; 8.3).
 *
 * <p>Reuse is detected against the process-wide {@link RecentValueHistory},
 * scoped to the logged-in suite user (the {@code owner_id} surfaced into the environment
 * by the test module). Each verifier module finishes after one Authorization Request, so
 * a duplicate is only observed when a later request (the next module in a {@code direct_post.jwt}
 * plan, or a re-run within the retention window) presents the same key.
 *
 * <p>The caller selects the severity (FAILURE under HAIP, WARNING otherwise).
 */
public class VP1FinalCheckEncryptionKeyNotReused extends AbstractCondition {

	private static final String NAMESPACE = "vp1final_verifier_encryption_key";

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {

		JsonElement clientMetadata = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_metadata");
		if (clientMetadata == null || !clientMetadata.isJsonObject()) {
			throw error("client_metadata is missing or not a JSON object");
		}

		JsonElement jwksEl = clientMetadata.getAsJsonObject().get("jwks");
		if (jwksEl == null || !jwksEl.isJsonObject()) {
			throw error("client_metadata.jwks is missing or not a JSON object", args("client_metadata", clientMetadata));
		}

		JsonElement keysEl = jwksEl.getAsJsonObject().get("keys");
		if (keysEl == null || !keysEl.isJsonArray() || keysEl.getAsJsonArray().isEmpty()) {
			throw error("client_metadata.jwks.keys must be a non-empty array", args("jwks", jwksEl));
		}

		String scope = env.getString("owner_id");
		if (scope == null || scope.isBlank()) {
			// owner_id is set unconditionally by the test module's configure() (via
			// exposeOwnerIdToEnvironment()), so its absence is a test suite bug, not a verifier issue.
			throw error("owner_id is missing from the environment - this is a test suite bug; the test "
				+ "module must surface it via exposeOwnerIdToEnvironment() in configure()");
		}

		List<String> thumbprints = collectEncryptionKeyThumbprints(keysEl.getAsJsonArray());
		if (thumbprints.isEmpty()) {
			logSuccess("No encryption-usable keys found in client_metadata.jwks to check for reuse");
			return env;
		}

		// checkAndRecord checks all thumbprints before recording any, so a single request
		// legitimately containing the same key twice does not self-trigger (duplicate-kid is a
		// separate check in VP1FinalCheckForKeyIdInClientMetadataJWKs).
		RecentValueHistory.SeenValue reused = RecentValueHistory.checkAndRecord(NAMESPACE, scope, thumbprints, getTestId());
		if (reused != null) {
			throw error("The verifier reused a response-encryption public key across Authorization Requests. "
					+ "Verifiers MUST supply an ephemeral encryption public key specific to each Authorization Request. "
					+ "If this test was started by re-presenting a request from an earlier test (for example by "
					+ "re-scanning an old QR code or reusing an old request link), start a fresh presentation flow "
					+ "at the verifier and run the test again.",
				args("jwk_thumbprint", reused.value(), "first_seen_in_test", reused.testId(), "jwks", jwksEl));
		}

		logSuccess("Verifier response-encryption key(s) were not seen in a previous Authorization Request",
			args("jwk_thumbprints", thumbprints));
		return env;
	}

	private List<String> collectEncryptionKeyThumbprints(JsonArray keys) {
		List<String> thumbprints = new ArrayList<>();
		for (JsonElement keyEl : keys) {
			if (!keyEl.isJsonObject()) {
				continue; // structural validation is handled by other conditions
			}
			JsonObject key = keyEl.getAsJsonObject();

			// Only consider keys usable for encryption: 'use' is 'enc' or 'use' is absent. Matches
			// VP1FinalValidateClientMetadataJwksForEncryptedResponse.
			if (key.has("use") && !"enc".equals(OIDFJSON.getString(key.get("use")))) {
				continue;
			}

			try {
				thumbprints.add(JWK.parse(key.toString()).computeThumbprint().toString());
			} catch (ParseException | JOSEException e) {
				// Unparseable / malformed keys are flagged by other conditions; don't double-fail here.
			}
		}
		return thumbprints;
	}
}
