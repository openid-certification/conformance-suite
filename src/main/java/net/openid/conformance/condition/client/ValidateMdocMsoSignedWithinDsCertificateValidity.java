package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.DataItem;

import java.security.cert.X509Certificate;
import java.time.Instant;

/**
 * Checks that the MSO 'signed' timestamp falls within the validity period of the document
 * signer certificate carried in the issuerAuth x5chain, the first check of ISO/IEC 18013-5
 * section 9.3.1 step 5. An mdoc reader rejects a credential that fails this check even though
 * its signatures verify.
 */
public class ValidateMdocMsoSignedWithinDsCertificateValidity extends AbstractValidateMdocMsoValidityInfo {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		DataItem issuerSigned = decodeIssuerSigned(env);
		X509Certificate dsCert = extractDsCertificate(issuerSigned);
		Instant signed = toInstant(parseMso(issuerSigned).getSignedAt());
		Instant notBefore = dsCert.getNotBefore().toInstant();
		Instant notAfter = dsCert.getNotAfter().toInstant();

		if (signed.isBefore(notBefore) || signed.isAfter(notAfter)) {
			throw error("The MSO 'signed' timestamp is outside the validity period of the document signer certificate in the issuerAuth x5chain; an mdoc reader following ISO/IEC 18013-5 section 9.3.1 rejects the credential",
				args("signed", signed.toString(),
					"certificate_not_before", notBefore.toString(),
					"certificate_not_after", notAfter.toString(),
					"certificate_subject", dsCert.getSubjectX500Principal().getName()));
		}

		logSuccess("The MSO 'signed' timestamp is within the validity period of the document signer certificate",
			args("signed", signed.toString(),
				"certificate_not_before", notBefore.toString(),
				"certificate_not_after", notAfter.toString()));
		return env;
	}
}
