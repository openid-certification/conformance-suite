package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * HAIP section 8 only requires entities to support SHA-256 for SD-JWT VC digests, so a credential
 * whose issuer chose another _sd_alg is valid but may not be accepted by every HAIP verifier.
 * Intended to be called as a warning.
 */
public class WarnIfSdJwtDigestAlgorithmNotSha256 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "sdjwt")
	public Environment evaluate(Environment env) {
		String sdAlg = ValidateSdJwtKbSdHash.getSdAlg(env);

		if (!ValidateSdJwtKbSdHash.DEFAULT_SD_ALG.equals(sdAlg)) {
			throw error("The credential's _sd_alg claim names a hash algorithm other than sha-256. HAIP only requires "
					+ "verifiers to support sha-256 for SD-JWT VC digests, so verifiers may not accept this credential",
				args("_sd_alg", sdAlg));
		}

		logSuccess("The credential's disclosure digests use sha-256, which all HAIP entities must support",
			args("_sd_alg", sdAlg));

		return env;
	}
}
