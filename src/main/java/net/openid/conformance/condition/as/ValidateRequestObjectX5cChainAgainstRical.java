package net.openid.conformance.condition.as;

import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.SignedJWT;
import kotlinx.io.bytestring.ByteString;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractRicalCondition;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.crypto.X509Cert;
import org.multipaz.mdoc.rical.RicalCertificateInfo;
import org.multipaz.mdoc.rical.SignedRical;
import org.multipaz.trustmanagement.RicalTrustManager;
import org.multipaz.trustmanagement.TrustResult;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Validates that the x5c certificate chain of the verifier's request object chains to a reader
 * CA certificate listed in the configured RICAL (ISO/IEC 18013-5 second edition draft Annex
 * F.3.2.6). This is the RICAL analogue of validating the chain against the 'Request Object
 * Trust Anchor': the wallet trusts the verifier because its certificate validates against a CA
 * on the reader trust list.
 *
 * Trust constraints (Annex F.3.2.3) are ecosystem-defined and this suite has no ecosystem
 * profile to evaluate them against, so entries carrying trust constraints are reported in the
 * result rather than enforced.
 */
public class ValidateRequestObjectX5cChainAgainstRical extends AbstractRicalCondition {

	@Override
	@PreEnvironment(required = { "authorization_request_object", "rical" })
	public Environment evaluate(Environment env) {

		String requestObject = env.getString("authorization_request_object", "value");

		List<Base64> x5c;
		try {
			SignedJWT jwt = SignedJWT.parse(requestObject);
			x5c = jwt.getHeader().getX509CertChain();
		} catch (ParseException e) {
			throw error("Error parsing request object JWT", e);
		}
		if (x5c == null || x5c.isEmpty()) {
			throw error("Request object JWT does not contain an x5c header, so its certificate chain cannot be evaluated against the RICAL");
		}

		List<X509Cert> chainCerts = new ArrayList<>();
		for (Base64 certB64 : x5c) {
			try {
				byte[] der = certB64.decode();
				chainCerts.add(new X509Cert(new ByteString(der, 0, der.length)));
			} catch (Exception e) {
				throw error("Failed to parse a certificate in the request object x5c header", e);
			}
		}
		String leafSubject = chainCerts.get(0).getSubject().getName();

		// Verify the signature here (against the RICAL's embedded x5chain) so a corrupted
		// RICAL cannot silently drive the trust verdict; there is no RICAL provider trust
		// anchor in this design, so this is an integrity check, not an authenticity check.
		// This deliberately errors at the caller's severity rather than skipping: the tester
		// explicitly configured this check, and a silent skip would look like a pass - see
		// the severity policy in SetupRicalFromConfiguration. The signature is verified on the
		// original bytes and the parse is lenient (mis-encoded serialNumbers are normalized)
		// so a single mis-encoded entry does not make the whole list unusable.
		SignedRical signedRical;
		try {
			verifyRicalCoseSignature(getRicalCoseSign1(getRicalBytes(env)));
			signedRical = parseSignedRicalLenient(getRicalBytes(env)).signedRical;
		} catch (Exception e) {
			throw error("The configured RICAL could not be parsed or its COSE signature does not verify, so the request object certificate chain cannot be evaluated against it", e);
		}

		String ricalProvider = signedRical.getRical().getProvider();

		TrustResult trustResult;
		try {
			RicalTrustManager trustManager = new RicalTrustManager(signedRical, "rical");
			kotlin.time.Instant now = kotlin.time.Instant.Companion.fromEpochMilliseconds(System.currentTimeMillis());
			trustResult = kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				(scope, continuation) -> trustManager.verify(chainCerts, now, continuation)
			);
		} catch (Exception e) {
			throw error("Failed to evaluate the request object certificate chain against the RICAL", e);
		}

		if (!trustResult.isTrusted()) {
			throw error("The request object certificate chain does not chain to a reader CA certificate in the configured RICAL",
				args("request_object_leaf_subject", leafSubject,
					"rical_provider", ricalProvider,
					"error", trustResult.getError() == null ? null : trustResult.getError().getMessage()));
		}

		// F.3.2.6: the CertificateInfo of the first (bottom-up) chain certificate listed in the
		// RICAL governs the trust constraints; locate it to report constraints and entry detail
		RicalCertificateInfo governingEntry = findFirstMatchingEntry(signedRical, chainCerts);

		int trustConstraintCount = governingEntry == null || governingEntry.getTrustConstraints() == null
			? 0 : governingEntry.getTrustConstraints().size();

		logSuccess("The request object certificate chain validates against a reader CA certificate listed in the RICAL",
			args("request_object_leaf_subject", leafSubject,
				"matched_rical_entry_subject", governingEntry == null
					? null : governingEntry.getCertificate().getSubject().getName(),
				"matched_rical_entry_name", governingEntry == null ? null : governingEntry.getName(),
				"rical_provider", ricalProvider,
				// Annex F trust constraints are ecosystem-defined; this suite reports their
				// presence but has no ecosystem profile to evaluate them against
				"trust_constraints_on_matched_entry", trustConstraintCount,
				// the RICAL's own validity is checked (as a WARNING) by ValidateRicalStructure;
				// recorded here so the basis of the trust verdict is visible in this entry too
				"rical_date", signedRical.getRical().getDate().toString(),
				"rical_not_after", signedRical.getRical().getNotAfter() == null
					? null : signedRical.getRical().getNotAfter().toString()));

		return env;
	}

	private RicalCertificateInfo findFirstMatchingEntry(SignedRical signedRical, List<X509Cert> chainCerts) {
		for (X509Cert chainCert : chainCerts) {
			byte[] chainCertSki = chainCert.getSubjectKeyIdentifier();
			for (RicalCertificateInfo certInfo : signedRical.getRical().getCertificateInfos()) {
				// match by certificate or SKI (a renewed CA keeping its key has a new certificate)
				if (certInfo.getCertificate().equals(chainCert)
					|| (chainCertSki != null
						&& Arrays.equals(certInfo.getCertificate().getSubjectKeyIdentifier(), chainCertSki))) {
					return certInfo;
				}
			}
		}
		return null;
	}
}
