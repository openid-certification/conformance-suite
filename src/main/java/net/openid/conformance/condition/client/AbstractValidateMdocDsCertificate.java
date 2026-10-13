package net.openid.conformance.condition.client;

import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocCertificateProfileChecks;
import net.openid.conformance.util.MdocUtil;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.mdoc.vical.SignedVical;
import org.multipaz.mdoc.vical.VicalCertificateInfo;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

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

	/**
	 * The certificate that issued the DS certificate: the next certificate in the x5chain when
	 * intermediates are included. For a leaf-only chain the issuer must be located rather than
	 * assumed: the matching entry of a configured VICAL is authoritative, and the configured
	 * trust anchor is only used when it actually is the DS certificate's issuer - a credential
	 * from a different IACA (e.g. one trusted via the VICAL) must not have its issuer binding
	 * compared against an unrelated anchor. Null when no issuer can be located, in which case
	 * the binding checks are skipped.
	 */
	protected X509Certificate issuingCertificate(List<X509Certificate> chain, Environment env) {
		if (chain.size() > 1) {
			return chain.get(1);
		}
		X509Certificate dsCert = chain.get(0);
		X509Certificate fromVical = issuingCertificateFromVical(env, dsCert);
		if (fromVical != null) {
			return fromVical;
		}
		String trustAnchorPem = env.getString("credential_trust_anchor_pem");
		if (trustAnchorPem != null) {
			X509Certificate anchor = X509CertUtils.parse(trustAnchorPem);
			if (anchor != null
				&& dsCert.getIssuerX500Principal().equals(anchor.getSubjectX500Principal())) {
				return anchor;
			}
		}
		return null;
	}

	/**
	 * Finds the VICAL entry for the DS certificate's issuer: subject name match, plus key
	 * identifier match when both certificates carry one (a renewed IACA keeping its subject has
	 * a different key). Returns null when no VICAL is configured, no entry matches, or the
	 * VICAL cannot be parsed - VICAL quality problems are reported by the VICAL conditions,
	 * not here.
	 */
	protected X509Certificate issuingCertificateFromVical(Environment env, X509Certificate dsCert) {
		String vicalB64 = env.getString("vical", "value");
		if (vicalB64 == null) {
			return null;
		}
		try {
			byte[] vicalBytes = Base64.getDecoder().decode(vicalB64);
			// signature verification disabled: ValidateVicalSignature reports on the signature
			SignedVical signedVical = kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				(scope, continuation) -> SignedVical.Companion.parse(vicalBytes, true, continuation));
			byte[] dsAkiValue = dsCert.getExtensionValue(MdocCertificateProfileChecks.OID_AUTHORITY_KEY_IDENTIFIER);
			byte[] dsAki = dsAkiValue == null ? null : AuthorityKeyIdentifier.getInstance(
				JcaX509ExtensionUtils.parseExtensionValue(dsAkiValue)).getKeyIdentifierOctets();
			for (VicalCertificateInfo certInfo : signedVical.getVical().getCertificateInfos()) {
				X509Cert iaca = certInfo.getCertificate();
				X509Certificate javaIaca = (X509Certificate) CertificateFactory.getInstance("X.509")
					.generateCertificate(new ByteArrayInputStream(
						iaca.getEncoded().toByteArray(0, iaca.getEncoded().getSize())));
				byte[] iacaSki = iaca.getSubjectKeyIdentifier();
				boolean subjectMatches =
					dsCert.getIssuerX500Principal().equals(javaIaca.getSubjectX500Principal());
				boolean skiMatches = dsAki == null || iacaSki == null
					|| java.util.Arrays.equals(dsAki, iacaSki);
				if (subjectMatches && skiMatches && signedThisCertificate(javaIaca, dsCert)) {
					return javaIaca;
				}
			}
		} catch (Exception e) {
			// leave the issuer unlocated; the binding checks are then skipped
		}
		return null;
	}

	/**
	 * Whether the candidate issuer's public key verifies the certificate's signature. Name and
	 * key identifier matching alone can select a certificate that did not issue this one - two
	 * IACAs of the same authority share a subject name, and a VICAL may list both - and the
	 * issuer binding checks would then report on the wrong certificate.
	 */
	protected boolean signedThisCertificate(X509Certificate candidateIssuer, X509Certificate cert) {
		try {
			cert.verify(candidateIssuer.getPublicKey());
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
