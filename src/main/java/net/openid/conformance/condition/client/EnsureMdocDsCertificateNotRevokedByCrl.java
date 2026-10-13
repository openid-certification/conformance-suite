package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;

/**
 * Checks that the document signer certificate's serial number is not listed on the CRL its
 * distribution point named: a revoked document signer certificate fails RFC 5280 path
 * validation, which ISO/IEC 18013-5 12.8.3 requires of an mdoc reader, so no reader accepts the
 * credential.
 */
public class EnsureMdocDsCertificateNotRevokedByCrl extends AbstractMdocDsCertificateCrlCondition {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor", ENV_CRL })
	public Environment evaluate(Environment env) {
		X509Certificate dsCert = extractDsCertificate(decodeIssuerSigned(env));
		X509CRL crl = parseCrl(env);

		X509CRLEntry entry = crl.getRevokedCertificate(dsCert.getSerialNumber());
		if (entry != null) {
			throw error("The document signer certificate is revoked: its serial number is listed on the IACA's CRL",
				args("serial_number", dsCert.getSerialNumber().toString(16),
					"revocation_date", entry.getRevocationDate(),
					"uri", env.getString(ENV_URI)));
		}

		logSuccess("The document signer certificate is not listed on the IACA's CRL",
			args("serial_number", dsCert.getSerialNumber().toString(16), "uri", env.getString(ENV_URI)));
		return env;
	}
}
