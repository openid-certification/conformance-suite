package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.multipaz.crypto.X509Cert;
import org.multipaz.mdoc.vical.SignedVical;
import org.multipaz.mdoc.vical.VicalCertificateInfo;
import org.multipaz.trustmanagement.TrustResult;
import org.multipaz.trustmanagement.VicalTrustManager;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pre-flight interoperability check for the verifier tests: validates that the suite's own mdoc
 * document signer certificate (minted under the suite IACA in {@link TestKeysAndCerts}) chains
 * to an IACA certificate listed in the configured VICAL. A verifier that trusts this VICAL is
 * expected to reject the suite's presented mdocs if the IACA is not covered, so a failure here
 * reports a problem with the suite's VICAL registration, not with the verifier under test —
 * this condition is expected to be called as a WARNING.
 *
 * The document type the suite will present is only known once the verifier's DCQL query
 * arrives, so this configure-time check reports the matched entry's listed docTypes rather
 * than asserting a specific one.
 */
public class ValidateOwnMdocSigningChainAgainstVical extends AbstractVicalCondition {

	@Override
	@PreEnvironment(required = "vical")
	public Environment evaluate(Environment env) {

		X509Cert dsCert = TestKeysAndCerts.getDocumentSignerCert();
		String dsSubject = dsCert.getSubject().getName();

		// as in ValidateMdocIssuerChainAgainstVical: verify the signature here so a corrupted
		// VICAL cannot silently drive the trust verdict, erring at the caller's severity
		SignedVical signedVical;
		try {
			signedVical = parseSignedVical(getVicalBytes(env), false);
		} catch (Exception e) {
			throw error("The configured VICAL could not be parsed or its COSE signature does not verify, so the suite's mdoc signing chain cannot be evaluated against it", e);
		}
		String vicalProvider = signedVical.getVical().getVicalProvider();

		TrustResult trustResult;
		try {
			VicalTrustManager trustManager = new VicalTrustManager(signedVical, "vical");
			kotlin.time.Instant now = kotlin.time.Instant.Companion.fromEpochMilliseconds(System.currentTimeMillis());
			trustResult = kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				// true = also validate the validity intervals of CA certificates in the chain
				(scope, continuation) -> trustManager.verify(List.of(dsCert), now, true, continuation)
			);
		} catch (Exception e) {
			throw error("Failed to evaluate the suite's mdoc signing chain against the VICAL", e);
		}

		if (!trustResult.isTrusted()) {
			throw error("The suite's mdoc document signer certificate does not chain to an IACA certificate in the configured VICAL; a verifier that trusts this VICAL is expected to reject the suite's presented mdocs. Check the suite IACA's registration with the VICAL provider.",
				args("document_signer_subject", dsSubject,
					"suite_iaca_subject", TestKeysAndCerts.getIacaRootCert().getSubject().getName(),
					"vical_provider", vicalProvider,
					"error", trustResult.getError() == null ? null : trustResult.getError().getMessage()));
		}

		// the same IACA may be listed in several entries with different docTypes; union them
		byte[] iacaSki = TestKeysAndCerts.getIacaRootCert().getSubjectKeyIdentifier();
		Set<String> listedDocTypes = new LinkedHashSet<>();
		for (VicalCertificateInfo certInfo : signedVical.getVical().getCertificateInfos()) {
			if (iacaSki != null
				&& Arrays.equals(certInfo.getCertificate().getSubjectKeyIdentifier(), iacaSki)) {
				listedDocTypes.addAll(certInfo.getDocTypes());
			}
		}
		JsonArray docTypes = new JsonArray();
		listedDocTypes.forEach(docTypes::add);

		logSuccess("The suite's mdoc document signer certificate chains to an IACA certificate listed in the VICAL; presented credentials must use one of the listed document types to be accepted",
			args("document_signer_subject", dsSubject,
				"suite_iaca_subject", TestKeysAndCerts.getIacaRootCert().getSubject().getName(),
				"listed_doc_types", docTypes,
				"vical_provider", vicalProvider));

		return env;
	}
}
