package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

/**
 * Checks that the disclosed PID contains the requested picture (portrait) claim. Callers invoke
 * this at WARNING severity: the EUDI PID Rulebook lists the portrait as a mandatory attribute, but
 * its mandatory inclusion only applies 24 months after entry into force of the regulation amending
 * CIR 2024/2977 and users may opt out of it, so a conformant PID is not guaranteed to contain it.
 */
public class EnsurePidPictureClaimDisclosed extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt"})
	public Environment evaluate(Environment env) {

		JsonObject decoded = DcqlQueryUtils.getDecodedSdJwtClaims(env);
		if (decoded == null) {
			throw error("No decoded SD-JWT claims found in environment");
		}

		if (!DcqlQueryUtils.isClaimPathPresent(decoded, List.of("picture"))) {
			throw error("The presentation does not contain the picture (portrait) claim. The DCQL " +
				"query's claim_sets offer a fallback without the picture, so this presentation is " +
				"valid; this warning highlights that the credential does not contain the portrait. " +
				"That is permitted because mandatory inclusion of the portrait only applies 24 months " +
				"after entry into force of the regulation amending CIR 2024/2977, and users may opt " +
				"out of it, so a conformant PID is not guaranteed to contain it.",
				args("decoded_credential", decoded));
		}

		logSuccess("The presentation contains the requested picture (portrait) claim");
		return env;
	}
}
