package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.DataItem;
import org.multipaz.mdoc.mso.MobileSecurityObject;

import java.time.Duration;
import java.time.Instant;

/**
 * Checks that the current time falls within the MSO's 'validFrom'..'validUntil' period, the
 * second and third checks of ISO/IEC 18013-5 section 9.3.1 step 5, allowing the same clock
 * skew as the SD-JWT 'nbf' and 'exp' checks.
 */
public class ValidateMdocMsoValidityPeriodIsCurrent extends AbstractValidateMdocMsoValidityInfo {

	private static final Duration ALLOWED_CLOCK_SKEW = Duration.ofMinutes(5);

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		DataItem issuerSigned = decodeIssuerSigned(env);
		MobileSecurityObject mso = parseMso(issuerSigned);
		Instant validFrom = toInstant(mso.getValidFrom());
		Instant validUntil = toInstant(mso.getValidUntil());
		Instant now = Instant.now();

		if (validFrom.isAfter(now.plus(ALLOWED_CLOCK_SKEW))) {
			throw error("The MSO 'validFrom' timestamp is in the future, so the credential is not yet valid; an mdoc reader following ISO/IEC 18013-5 section 9.3.1 rejects it",
				args("valid_from", validFrom.toString(), "now", now.toString()));
		}
		if (validUntil.isBefore(now.minus(ALLOWED_CLOCK_SKEW))) {
			throw error("The MSO 'validUntil' timestamp is in the past, so the credential has expired; an mdoc reader following ISO/IEC 18013-5 section 9.3.1 rejects it",
				args("valid_until", validUntil.toString(), "now", now.toString()));
		}

		logSuccess("The current time is within the MSO 'validFrom'..'validUntil' period",
			args("valid_from", validFrom.toString(), "valid_until", validUntil.toString(), "now", now.toString()));
		return env;
	}
}
