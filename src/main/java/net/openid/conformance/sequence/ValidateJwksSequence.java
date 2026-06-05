package net.openid.conformance.sequence;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.common.EnsureJwksHasNoPrivateOrSymmetricKeyMaterial;
import net.openid.conformance.condition.common.MapJwksToValidationLocation;
import net.openid.conformance.condition.common.ValidateJwksStructure;
import net.openid.conformance.condition.common.WarnOnUnusableJwksKeys;

/**
 * Validates a JWK set wherever one enters the suite, with uniform severities: FAILURE for invalid
 * structure or private/symmetric key material, WARNING for keys using an unknown key type, curve or
 * algorithm. The caller names the source location (a top-level environment object, or a nested path
 * within one) and a human-readable label that is woven into the result messages (e.g. "client_metadata"),
 * so a single set of source-agnostic conditions still produces source-specific messages.
 *
 * @see net.openid.conformance.util.JWKUtil
 */
public class ValidateJwksSequence extends AbstractConditionSequence {

	private final String sourceKey;
	private final String sourcePath;
	private final String label;
	private final String[] requirements;
	private boolean allowPrivateKeys = false;

	/**
	 * @param sourceKey the top-level environment object holding (or containing) the JWK set
	 * @param sourcePath dot-separated path to the JWK set within {@code sourceKey}, or null/empty if
	 *                   {@code sourceKey} is itself the JWK set
	 * @param label human-readable name of the source, used in the result messages
	 * @param requirements spec requirement tags to attach to the validation results
	 */
	public ValidateJwksSequence(String sourceKey, String sourcePath, String label, String... requirements) {
		this.sourceKey = sourceKey;
		this.sourcePath = (sourcePath == null) ? "" : sourcePath;
		this.label = label;
		this.requirements = requirements.clone();
	}

	/**
	 * Mark the JWK set as a private (signing) key set that legitimately contains private key
	 * material, so the public-only check is skipped. Structure and unusable-key checks still run.
	 * Use for a set that is NOT advertised to a counterparty (e.g. the suite's own signing keys).
	 */
	public ValidateJwksSequence allowingPrivateKeys() {
		this.allowPrivateKeys = true;
		return this;
	}

	@Override
	public void evaluate() {
		call(exec().startBlock("Validate the JWK set in " + label));

		call(exec()
			.putString("jwks_validation_source_key", sourceKey)
			.putString("jwks_validation_source_path", sourcePath)
			.putString("jwks_source_label", label));

		if (sourcePath.isEmpty()) {
			call(condition(MapJwksToValidationLocation.class).skipIfObjectMissing(sourceKey));
		} else {
			call(condition(MapJwksToValidationLocation.class).skipIfElementMissing(sourceKey, sourcePath));
		}

		if (!allowPrivateKeys) {
			call(condition(EnsureJwksHasNoPrivateOrSymmetricKeyMaterial.class)
				.skipIfObjectMissing("jwks_to_validate")
				.onFail(ConditionResult.FAILURE)
				.dontStopOnFailure()
				.requirements(requirements));
		}

		call(condition(ValidateJwksStructure.class)
			.skipIfObjectMissing("jwks_to_validate")
			.onFail(ConditionResult.FAILURE)
			.dontStopOnFailure()
			.requirements(requirements));

		call(condition(WarnOnUnusableJwksKeys.class)
			.skipIfObjectMissing("jwks_to_validate")
			.onFail(ConditionResult.WARNING)
			.dontStopOnFailure()
			.requirements(requirements));

		call(exec()
			.removeObject("jwks_to_validate")
			.removeNativeValue("jwks_validation_source_key")
			.removeNativeValue("jwks_validation_source_path")
			.removeNativeValue("jwks_source_label")
			.endBlock());
	}
}
