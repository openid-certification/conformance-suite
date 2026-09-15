package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.DataItem;
import org.multipaz.mdoc.mso.MobileSecurityObject;

import java.time.Instant;

/**
 * Checks that the MSO 'validUntil' timestamp is later than 'validFrom', as ISO/IEC 18013-5
 * section 9.1.2.4 requires of the issuer. The current-time check cannot cover this on its own:
 * its clock-skew allowance lets an empty or inverted validity period that straddles the
 * current time through.
 */
public class ValidateMdocMsoValidUntilAfterValidFrom extends AbstractValidateMdocMsoValidityInfo {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		DataItem issuerSigned = decodeIssuerSigned(env);
		MobileSecurityObject mso = parseMso(issuerSigned);
		Instant validFrom = toInstant(mso.getValidFrom());
		Instant validUntil = toInstant(mso.getValidUntil());

		if (!validUntil.isAfter(validFrom)) {
			throw error("The MSO 'validUntil' timestamp is not later than 'validFrom', " +
					"so the credential has no validity period; ISO/IEC 18013-5 section 9.1.2.4 " +
					"requires validUntil to be later than validFrom",
				args("valid_from", validFrom.toString(), "valid_until", validUntil.toString()));
		}

		logSuccess("The MSO 'validUntil' timestamp is later than 'validFrom'",
			args("valid_from", validFrom.toString(), "valid_until", validUntil.toString()));
		return env;
	}
}
