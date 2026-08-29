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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks whether the RICAL entry governing the verifier's certificate chain carries trust
 * constraints (ISO/IEC 18013-5 second edition draft Annex F.3.2.3). The specification defines
 * no concrete trust constraint types — ecosystems are expected to profile their own — so this
 * suite cannot evaluate them; per F.3.2.6 a wallet treats constraint types it does not
 * understand as not satisfied, so a wallet enforcing the RICAL strictly may reject this
 * verifier even though the chain itself is trusted. Expected to be called as a WARNING; it can
 * be raised per-profile once an ecosystem defines machine-checkable constraint semantics.
 */
public class EnsureMatchedRicalEntryHasNoTrustConstraints extends AbstractRicalCondition {

	@Override
	@PreEnvironment(required = { "authorization_request_object", "rical" })
	public Environment evaluate(Environment env) {

		List<Base64> x5c;
		try {
			x5c = SignedJWT.parse(env.getString("authorization_request_object", "value"))
				.getHeader().getX509CertChain();
		} catch (ParseException e) {
			throw error("Error parsing request object JWT", e);
		}
		if (x5c == null || x5c.isEmpty()) {
			log("Request object JWT does not contain an x5c header; there is no chain to match against the RICAL");
			return env;
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

		SignedRical signedRical;
		try {
			signedRical = parseSignedRicalLenient(getRicalBytes(env)).signedRical;
		} catch (Exception e) {
			throw error("The configured RICAL could not be parsed, so the matched entry's trust constraints cannot be examined", e);
		}

		RicalCertificateInfo governingEntry = findFirstMatchingRicalEntry(signedRical, chainCerts);
		if (governingEntry == null) {
			// an untrusted chain is reported by ValidateRequestObjectX5cChainAgainstRical; there
			// is no governing entry whose constraints could apply
			log("No RICAL entry matches the request object certificate chain; the chain trust itself is checked separately");
			return env;
		}

		int constraintCount = governingEntry.getTrustConstraints() == null
			? 0 : governingEntry.getTrustConstraints().size();
		if (constraintCount > 0) {
			throw error("The RICAL entry governing the verifier's certificate chain carries trust constraints, which are ecosystem-defined and cannot be evaluated by this suite; a wallet enforcing this RICAL strictly treats constraint types it does not understand as not satisfied and may therefore reject this verifier",
				args("matched_rical_entry_subject", governingEntry.getCertificate().getSubject().getName(),
					"trust_constraint_count", constraintCount));
		}

		logSuccess("The RICAL entry governing the verifier's certificate chain carries no trust constraints",
			args("matched_rical_entry_subject", governingEntry.getCertificate().getSubject().getName()));

		return env;
	}
}
