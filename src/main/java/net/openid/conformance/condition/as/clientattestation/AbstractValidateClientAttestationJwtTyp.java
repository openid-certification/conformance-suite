package net.openid.conformance.condition.as.clientattestation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks the JOSE header 'typ' of one of the two client attestation JWTs against the value the
 * attestation-based client authentication draft gives for it.
 *
 * <p>The comparison is an exact, case-sensitive string match, the same policy as the suite's DPoP
 * proof and key attestation checks. RFC 7515 section 4.1.9 treats 'typ' as a media type, so a
 * receiver could also accept an 'application/' prefix or a different case; the draft states the
 * bare lower-case value as a MUST for the sender, and that literal is what the client is tested on.
 */
public abstract class AbstractValidateClientAttestationJwtTyp extends AbstractCondition {

	private final String envKey;

	private final String expectedTyp;

	private final String jwtName;

	protected AbstractValidateClientAttestationJwtTyp(String envKey, String expectedTyp, String jwtName) {
		this.envKey = envKey;
		this.expectedTyp = expectedTyp;
		this.jwtName = jwtName;
	}

	@Override
	public Environment evaluate(Environment env) {
		String typ = env.getString(envKey, "header.typ");
		if (!expectedTyp.equals(typ)) {
			throw error(jwtName + " JOSE header 'typ' must be '" + expectedTyp + "'",
				args("expected", expectedTyp, "header", env.getElementFromObject(envKey, "header")));
		}
		logSuccess(jwtName + " JOSE header 'typ' is '" + expectedTyp + "'");
		return env;
	}
}
