package net.openid.conformance.condition.client;

import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.LoteTrustAnchors;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import net.openid.conformance.util.X509CertificateUtil;
import org.multipaz.cbor.Cbor;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.crypto.X509CertChainJvmKt;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Validates the certification path of the MSO revocation list's x5chain as per ISO/IEC 18013-5
 * 12.3.6.2, which applies to both revocation mechanisms: when the MSO's status reference
 * carries the optional Certificate element, that certificate is the trust point for the chain;
 * otherwise "the top-level certificate in the x5chain element shall be signed by the certificate
 * used to sign the certificate in the x5chain element of the MSO" — in the context of an mDL,
 * the IACA certificate.
 *
 * Without the Certificate element the same-issuer requirement is checked structurally (issuer
 * name and Authority Key Identifier of the revocation list chain's top certificate must match
 * those of the MSO chain's top certificate) and then cryptographically, by PKIX-validating the
 * chain against the IACA: with a VICAL configured, the IACA certificate the VICAL lists for the
 * MSO's own chain, as resolved and recorded by {@link ValidateMdocIssuerChainAgainstVical};
 * otherwise the 'Credential Trust Anchor' from the test configuration, when one is configured.
 * The name and key identifier comparison alone would accept a certificate that merely claims
 * the IACA as its issuer.
 */
public class ValidateMdocRevocationListCertificateChain extends AbstractRevocationListCwtCondition {

	@Override
	@PreEnvironment(strings = { ENV_TOKEN, "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {

		ParsedRevocationListCwt parsed = parseRevocationListCwt(env);
		X509CertChain statusChain = requireProtectedX5chain(parsed.coseSign1(),
			"its certification path cannot be validated");

		List<X509Certificate> statusJavaChain = X509CertChainJvmKt.getJavaX509Certificates(statusChain);
		X509Cert statusTop = statusChain.getCertificates().get(statusChain.getCertificates().size() - 1);

		String referenceCertB64 = env.getString(ENV_REFERENCE_CERTIFICATE);
		if (referenceCertB64 != null) {
			// 12.3.6.2: the Certificate element of the status reference is the trust point
			X509Certificate trustPoint = X509CertUtils.parse(Base64.getDecoder().decode(referenceCertB64));
			if (trustPoint == null) {
				throw error("Failed to parse the Certificate element of the MSO's status reference"
					+ " as an X.509 certificate");
			}
			try {
				X509CertificateUtil.validateX5cCertificateChain(statusJavaChain, trustPoint);
			} catch (X509CertificateUtil.X5cCertificateChainException e) {
				throw error("The MSO revocation list's x5chain does not validate against the Certificate element of the MSO's status reference",
					args("error", e.getMessage(),
						"trust_point_subject", trustPoint.getSubjectX500Principal().getName()));
			}
			logSuccess("The MSO revocation list's x5chain validates against the Certificate element of the MSO's status reference",
				args("trust_point_subject", trustPoint.getSubjectX500Principal().getName(),
					"chain_length", statusJavaChain.size()));
			return env;
		}

		// no Certificate element: the top-level certificate must be signed by the same CA that
		// signed the MSO's signer certificate (the IACA); check the issuer identity structurally
		X509Cert msoTop = msoChainTop(env);
		String msoIssuer = msoTop.getIssuer().getName();
		String statusIssuer = statusTop.getIssuer().getName();
		if (!statusIssuer.equals(msoIssuer)) {
			throw error("The issuer of the MSO revocation list's top-level certificate does not match the issuer of the MSO's signer certificate; without a Certificate element in the status reference, both must be signed by the same CA (the IACA)",
				args("revocation_list_chain_issuer", statusIssuer,
					"mso_chain_issuer", msoIssuer));
		}
		byte[] statusAki = statusTop.getAuthorityKeyIdentifier();
		byte[] msoAki = msoTop.getAuthorityKeyIdentifier();
		if (statusAki != null && msoAki != null && !Arrays.equals(statusAki, msoAki)) {
			throw error("The Authority Key Identifier of the MSO revocation list's top-level certificate does not match that of the MSO's signer certificate; without a Certificate element in the status reference, both must be signed by the same CA (the IACA)",
				args("revocation_list_chain_aki", HexFormat.of().formatHex(statusAki),
					"mso_chain_aki", HexFormat.of().formatHex(msoAki)));
		}

		// then cryptographically: the chain must validate against the IACA itself
		LoteTrustAnchors.TrustAnchorSet anchors;
		if (env.containsObject("vical")) {
			String vicalIacaB64 = env.getString(ValidateMdocIssuerChainAgainstVical.ENV_TRUST_POINT);
			if (vicalIacaB64 == null) {
				throw error("The mdoc's own certificate chain did not resolve to an IACA certificate in the"
					+ " configured VICAL, so the MSO revocation list's certification path cannot be"
					+ " validated against the IACA");
			}
			X509Certificate vicalIaca = X509CertUtils.parse(Base64.getDecoder().decode(vicalIacaB64));
			if (vicalIaca == null) {
				throw error("Failed to parse the IACA certificate recorded from the configured VICAL");
			}
			anchors = new LoteTrustAnchors.TrustAnchorSet(List.of(vicalIaca),
				List.of("the IACA certificate the configured VICAL lists for the MSO's issuer"));
		} else {
			// the configured 'Credential Trust Anchor' plus the PID and mDL providers listed in the configured
			// LoTEs: the IACA that signed the MSO's signer also signs the revocation list signer (12.3.6.2)
			try {
				anchors = LoteTrustAnchors.resolve(env, env.getString("credential_trust_anchor_pem"),
					"Credential Trust Anchor", LoteSlot.ISSUANCE_SUFFIX, LoteSlot.PID, LoteSlot.MDL);
			} catch (X509CertificateUtil.X5cCertificateChainException e) {
				throw error("Failed to parse the 'Credential Trust Anchor' field in the 'Credential"
					+ " Issuer' section of the test configuration as a PEM encoded X.509 certificate", e);
			}
		}
		X509Certificate matched;
		try {
			// with trust anchors this is full PKIX path validation; without any (none configured
			// outside HAIP) only the chain-internal checks run
			matched = X509CertificateUtil.validateX5cCertificateChain(statusJavaChain, anchors.anchors());
		} catch (X509CertificateUtil.X5cCertificateChainException e) {
			throw error("The MSO revocation list's x5chain does not validate"
					+ (anchors.isEmpty() ? "" : " against " + String.join(" or ", anchors.sources())),
				args("error", e.getMessage(), "trust_anchors_tried", anchors.sourcesJson()));
		}

		logSuccess("The MSO revocation list's x5chain is signed by the same CA as the MSO's signer certificate"
				+ (matched != null ? " and validates against " + anchors.sourceOf(matched)
					: "; no trust anchor is configured, so only chain-internal checks were performed"),
			args("issuer", statusIssuer, "chain_length", statusJavaChain.size(), "trust_anchors", anchors.sourcesJson()));

		return env;
	}

	private X509Cert msoChainTop(Environment env) {
		try {
			byte[] mdoc = Base64.getDecoder().decode(env.getString("mdoc_credential_cbor"));
			X509CertChain msoChain = MdocUtil.extractX5chain(Cbor.INSTANCE.decode(mdoc));
			return msoChain.getCertificates().get(msoChain.getCertificates().size() - 1);
		} catch (Exception e) {
			throw error("Failed to extract the x5chain from the mdoc issuerAuth", e);
		}
	}

}
