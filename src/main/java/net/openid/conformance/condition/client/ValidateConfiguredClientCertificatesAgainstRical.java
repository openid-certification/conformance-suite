package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import kotlinx.io.bytestring.ByteString;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import org.multipaz.crypto.X509Cert;
import org.multipaz.mdoc.rical.RicalCertificateInfo;
import org.multipaz.mdoc.rical.SignedRical;
import org.multipaz.trustmanagement.TrustResult;
import org.openqa.selenium.InvalidArgumentException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-flight interoperability check for the wallet tests: validates that the certificate chains
 * of the suite's own configured request signing keys (the x5c in the client's configured
 * jwks, and in the second client's when present) chain to a reader CA certificate listed in the configured RICAL
 * (ISO/IEC 18013-5 second edition draft Annex F.3.2.6). A wallet that trusts this RICAL is
 * expected to reject the suite's signed requests if the chain is not covered, so a failure here
 * reports a problem with the test configuration or the RICAL registration, not with the wallet
 * under test — this condition is expected to be called as a WARNING.
 */
public class ValidateConfiguredClientCertificatesAgainstRical extends AbstractRicalCondition {

	/** As labelled on schedule-test.html: the field is 'jwks', in the 'Client' section. */
	private static final String CLIENT_JWKS_FIELD = "'jwks' field in the 'Client' section";
	private static final String CLIENT2_JWKS_FIELD = "'jwks' field in the 'Second client' section";

	@Override
	@PreEnvironment(required = { "client_jwks", "rical" })
	public Environment evaluate(Environment env) {

		Map<String, List<X509Cert>> chains = new LinkedHashMap<>();
		chains.put(CLIENT_JWKS_FIELD, x5cChainFromJwks(env.getObject("client_jwks"), CLIENT_JWKS_FIELD));
		JsonObject client2Jwks = env.getObject("client2_jwks");
		if (client2Jwks != null) {
			chains.put(CLIENT2_JWKS_FIELD, x5cChainFromJwks(client2Jwks, CLIENT2_JWKS_FIELD));
		}

		SignedRical signedRical;
		try {
			verifyRicalCoseSignature(getRicalCoseSign1(getRicalBytes(env)));
			signedRical = parseSignedRical(getRicalBytes(env), true);
		} catch (ConditionError e) {
			// the helpers log their own, more specific, failure before throwing
			throw e;
		} catch (Exception e) {
			throw error("The configured RICAL could not be parsed or its COSE signature does not verify, so the suite's request signing certificate chains cannot be evaluated against it", e);
		}
		String ricalProvider = signedRical.getRical().getProvider();

		JsonObject results = new JsonObject();
		List<String> untrusted = new ArrayList<>();
		for (Map.Entry<String, List<X509Cert>> entry : chains.entrySet()) {
			TrustResult trustResult = verifyChainAgainstRical(signedRical, entry.getValue());
			JsonObject result = new JsonObject();
			result.addProperty("leaf_subject", entry.getValue().get(0).getSubject().getName());
			String trustPathDefect = trustResult.isTrusted()
				? ricalTrustPathDefect(signedRical, trustResult) : null;
			result.addProperty("trusted", trustResult.isTrusted() && trustPathDefect == null);
			if (trustResult.isTrusted() && trustPathDefect == null) {
				RicalCertificateInfo matched = findFirstMatchingRicalEntry(signedRical, entry.getValue());
				if (matched != null) {
					result.addProperty("matched_rical_entry_subject", matched.getCertificate().getSubject().getName());
					result.addProperty("trust_constraints_on_matched_entry",
						matched.getTrustConstraints() == null ? 0 : matched.getTrustConstraints().size());
				}
			} else {
				if (trustPathDefect != null) {
					result.addProperty("error", trustPathDefect);
				} else if (trustResult.getError() != null) {
					result.addProperty("error", trustResult.getError().getMessage());
				}
				untrusted.add(entry.getKey());
			}
			results.add(entry.getKey(), result);
		}

		if (!untrusted.isEmpty()) {
			throw error("The suite's configured request signing certificate does not chain to a reader CA certificate in the configured RICAL; a wallet that trusts this RICAL is expected to reject the suite's signed requests. Check the "
					+ String.join(" and the ", untrusted) + " in the test configuration, or the reader CA's registration with the RICAL provider.",
				args("results", results, "rical_provider", ricalProvider));
		}

		logSuccess("The suite's configured request signing certificate chains validate against reader CA certificates listed in the RICAL",
			args("results", results, "rical_provider", ricalProvider));

		return env;
	}

	/**
	 * Extracts the x5c chain of the JWKS's signing key as multipaz certificates. The key is
	 * selected with {@link JWKUtil#getSigningKey}, the same way the request object signer picks
	 * its key, so this evaluates the certificate the wallet will actually be presented with.
	 */
	private List<X509Cert> x5cChainFromJwks(JsonObject jwks, String fieldLabel) {
		JWK signingKey;
		try {
			signingKey = JWKUtil.getSigningKey(jwks);
		} catch (ParseException e) {
			throw error("The " + fieldLabel + " of the test configuration could not be parsed as a JWKS", e);
		} catch (InvalidArgumentException e) {
			throw error("The " + fieldLabel + " of the test configuration does not contain exactly one signing key, so there is no single certificate chain to evaluate against the RICAL",
				args("error", e.getMessage()));
		}
		List<com.nimbusds.jose.util.Base64> x5c = signingKey.getX509CertChain();
		if (x5c == null || x5c.isEmpty()) {
			throw error("The signing key in the " + fieldLabel
				+ " of the test configuration has no 'x5c' certificate chain, so there is no certificate to evaluate against the RICAL");
		}
		List<X509Cert> chain = new ArrayList<>();
		for (com.nimbusds.jose.util.Base64 certB64 : x5c) {
			try {
				byte[] der = certB64.decode();
				chain.add(new X509Cert(new ByteString(der, 0, der.length)));
			} catch (Exception e) {
				throw error("Failed to parse a certificate in the x5c of the " + fieldLabel
					+ " of the test configuration", e);
			}
		}
		return chain;
	}
}
