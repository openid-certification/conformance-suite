package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.DataItem;
import org.multipaz.mdoc.mso.MobileSecurityObject;

import java.time.Instant;

/**
 * Checks that the MSO 'validFrom' timestamp is equal to or later than 'signed', as
 * ISO/IEC 18013-5 section 9.1.2.4 requires of the issuer.
 */
public class ValidateMdocMsoValidFromNotBeforeSigned extends AbstractValidateMdocMsoValidityInfo {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		DataItem issuerSigned = decodeIssuerSigned(env);
		MobileSecurityObject mso = parseMso(issuerSigned);
		Instant signed = toInstant(mso.getSignedAt());
		Instant validFrom = toInstant(mso.getValidFrom());

		if (validFrom.isBefore(signed)) {
			throw error("The MSO 'validFrom' timestamp is earlier than 'signed'; ISO/IEC 18013-5 section 9.1.2.4 requires validFrom to be equal to or later than the time the MSO was signed",
				args("signed", signed.toString(), "valid_from", validFrom.toString()));
		}

		logSuccess("The MSO 'validFrom' timestamp is equal to or later than 'signed'",
			args("signed", signed.toString(), "valid_from", validFrom.toString()));
		return env;
	}
}
