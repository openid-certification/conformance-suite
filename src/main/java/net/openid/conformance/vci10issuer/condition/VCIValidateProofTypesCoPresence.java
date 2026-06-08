package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Enforces the co-presence rule between {@code cryptographic_binding_methods_supported}
 * and {@code proof_types_supported} on each credential configuration. The OID4VCI 1.0
 * Final §12.2.4 definition of {@code proof_types_supported} is:
 *
 * <blockquote>
 *   "OPTIONAL. Object that describes specifics of the key proof(s) that the Credential
 *   Issuer supports. <b>It MUST be present if cryptographic_binding_methods_supported is
 *   present, and omitted otherwise.</b> If absent, the Wallet is not required to supply
 *   proofs when requesting this credential."
 * </blockquote>
 *
 * The "MUST be present if ... is present, and omitted otherwise" wording makes the
 * relationship bidirectional: each one is present if and only if the other is. This
 * condition flags either direction of violation.
 */
public class VCIValidateProofTypesCoPresence extends AbstractVciCredentialConfigurationsCondition {

	private static final String SPEC_QUOTE =
		"OID4VCI 1.0 Final §12.2.4: proof_types_supported \"MUST be present if "
			+ "cryptographic_binding_methods_supported is present, and omitted otherwise\".";

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		List<String> issues = new ArrayList<>();
		forEachCredentialConfiguration(env, (configId, config) -> {
			boolean hasBinding = isPresent(config, "cryptographic_binding_methods_supported");
			boolean hasProof = isPresent(config, "proof_types_supported");
			if (hasBinding && !hasProof) {
				issues.add(String.format("credential_configurations_supported.%s: cryptographic_binding_methods_supported is present but proof_types_supported is missing", configId));
			} else if (!hasBinding && hasProof) {
				issues.add(String.format("credential_configurations_supported.%s: proof_types_supported is present but cryptographic_binding_methods_supported is missing", configId));
			}
		});

		if (!issues.isEmpty()) {
			throw error("cryptographic_binding_methods_supported and proof_types_supported must appear together or both be omitted: " + SPEC_QUOTE,
				args("issues", issues, "spec_quote", SPEC_QUOTE));
		}

		logSuccess("cryptographic_binding_methods_supported and proof_types_supported are consistently paired across all credential configurations");
		return env;
	}

	private static boolean isPresent(JsonObject config, String member) {
		// JsonObject.has() returns true for explicit JSON null; treat null as absent so a
		// null value doesn't satisfy the co-presence requirement on its own.
		JsonElement el = config.get(member);
		return el != null && !el.isJsonNull();
	}
}
