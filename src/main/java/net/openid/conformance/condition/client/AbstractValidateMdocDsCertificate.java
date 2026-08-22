package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.crypto.X509CertChain;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Base for conditions that validate the document signer (DS) certificate carried in the
 * x5chain header of an mdoc's issuerAuth against the DS certificate profile in
 * ISO/IEC 18013-5 Annex B.1.4 / Table B.3.
 */
public abstract class AbstractValidateMdocDsCertificate extends AbstractCondition {

	/**
	 * Decodes the IssuerSigned structure from 'mdoc_credential_cbor'.
	 */
	protected DataItem decodeIssuerSigned(Environment env) {
		String mdocCborBase64 = env.getString("mdoc_credential_cbor");
		if (mdocCborBase64 == null || mdocCborBase64.isEmpty()) {
			throw error("mdoc_credential_cbor is null or empty");
		}
		try {
			return Cbor.INSTANCE.decode(Base64.getDecoder().decode(mdocCborBase64));
		} catch (Exception e) {
			throw error("Failed to decode IssuerSigned CBOR", e);
		}
	}

	/**
	 * Extracts the DS certificate (the first certificate in the issuerAuth x5chain) from a
	 * decoded IssuerSigned structure.
	 */
	protected X509Certificate extractDsCertificate(DataItem issuerSigned) {
		return extractDsCertificateChain(issuerSigned).get(0);
	}

	/**
	 * Extracts the full (non-empty, leaf-first) x5chain from a decoded IssuerSigned structure
	 * as parsed X.509 certificates.
	 */
	protected java.util.List<X509Certificate> extractDsCertificateChain(DataItem issuerSigned) {
		X509CertChain certChain;
		try {
			certChain = MdocUtil.extractX5chain(issuerSigned);
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		}

		try {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			java.util.List<X509Certificate> certs = new java.util.ArrayList<>();
			for (org.multipaz.crypto.X509Cert cert : certChain.getCertificates()) {
				var encoded = cert.getEncoded();
				certs.add((X509Certificate) certificateFactory.generateCertificate(
					new ByteArrayInputStream(encoded.toByteArray(0, encoded.getSize()))));
			}
			return certs;
		} catch (Exception e) {
			throw error("Failed to parse a certificate in x5chain as an X.509 certificate", e);
		}
	}
}
