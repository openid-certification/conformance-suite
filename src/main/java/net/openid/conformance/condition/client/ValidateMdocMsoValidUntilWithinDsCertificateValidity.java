package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.DataItem;

import java.security.cert.X509Certificate;
import java.time.Instant;

/**
 * Checks that the MSO 'validUntil' timestamp is not later than the notAfter of the document
 * signer certificate in the issuerAuth x5chain. ISO/IEC 18013-5 section 9.3.1 step 5 says an
 * mdoc reader MAY reject an MSO that outlives its signing certificate, so callers should treat
 * a failure as a warning.
 */
public class ValidateMdocMsoValidUntilWithinDsCertificateValidity extends AbstractValidateMdocMsoValidityInfo {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		DataItem issuerSigned = decodeIssuerSigned(env);
		X509Certificate dsCert = extractDsCertificate(issuerSigned);
		Instant validUntil = toInstant(parseMso(issuerSigned).getValidUntil());
		Instant notAfter = dsCert.getNotAfter().toInstant();

		if (validUntil.isAfter(notAfter)) {
			throw error("The MSO 'validUntil' timestamp is later than the notAfter of the document signer certificate in the issuerAuth x5chain; ISO/IEC 18013-5 section 9.3.1 allows an mdoc reader to reject such a credential",
				args("valid_until", validUntil.toString(),
					"certificate_not_after", notAfter.toString(),
					"certificate_subject", dsCert.getSubjectX500Principal().getName()));
		}

		logSuccess("The MSO 'validUntil' timestamp is not later than the document signer certificate's notAfter",
			args("valid_until", validUntil.toString(), "certificate_not_after", notAfter.toString()));
		return env;
	}
}
