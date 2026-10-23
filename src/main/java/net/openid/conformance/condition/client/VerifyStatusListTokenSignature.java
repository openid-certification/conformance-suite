package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

/**
 * Verifies the Token Status List JWT signature with the key its header names, in the ways
 * draft-ietf-oauth-status-list section 11.3 lists for a status issuer that is the issuer of the
 * referenced token: the end-entity certificate of an {@code x5c} header, a {@code jwk} header, or
 * a key in the issuer's published JWK set ({@code server_jwks}). This is the check outside HAIP,
 * where no trust anchor need be configured; a configured trust source adds
 * {@link ValidateStatusListTokenX5cCertificateChain}, which validates the {@code x5c} chain against it.
 *
 * <p>Skips when no status list fetch was performed (credential had no status claim).
 */
public class VerifyStatusListTokenSignature extends AbstractVerifyJwsSignature {

	@Override
	public Environment evaluate(Environment env) {

		if (!env.containsObject("status_list_token")) {
			log("No parsed status list token in environment, skipping signature verification");
			return env;
		}

		String statusListTokenJwtString = env.getString("status_list_token", "value");
		if (statusListTokenJwtString == null) {
			throw error("status_list_token has no raw JWT value");
		}

		SignedJWT statusListTokenJwt;
		try {
			statusListTokenJwt = SignedJWT.parse(statusListTokenJwtString);
		} catch (ParseException e) {
			throw error("Unable to parse status list token", e);
		}

		List<Base64> x5c = statusListTokenJwt.getHeader().getX509CertChain();
		if (x5c != null && !x5c.isEmpty()) {
			verifyWithX5cLeaf(statusListTokenJwt, x5c.get(0));
			return env;
		}

		if (statusListTokenJwt.getHeader().getJWK() != null) {
			JWKSet jwkSet = new JWKSet(statusListTokenJwt.getHeader().getJWK());
			JsonObject jwkSetObject = JsonParser.parseString(jwkSet.toString()).getAsJsonObject();
			verifyJwsSignature(statusListTokenJwtString, jwkSetObject, "status list token", false, "JWT header jwk");
			return env;
		}

		if (env.containsObject("server_jwks")) {
			verifyJwsSignature(statusListTokenJwtString, env.getObject("server_jwks"), "status list token", false, "server");
			return env;
		}

		throw error("Unable to verify the status list token signature: its header has no x5c or jwk, and the issuer's JWK set is not available",
			args("header", statusListTokenJwt.getHeader().toJSONObject()));
	}

	private void verifyWithX5cLeaf(SignedJWT jwt, Base64 leafBase64) {
		X509Certificate leaf = X509CertUtils.parse(leafBase64.decode());
		if (leaf == null) {
			throw error("The first certificate in the status list token's x5c header is not a valid X.509 certificate",
				args("header", jwt.getHeader().toJSONObject()));
		}
		try {
			JWSVerifier verifier = new DefaultJWSVerifierFactory().createJWSVerifier(jwt.getHeader(), leaf.getPublicKey());
			if (!jwt.verify(verifier)) {
				throw error("Status list token signature does not verify with the end-entity certificate of its x5c header",
					args("leaf_cert_subject", leaf.getSubjectX500Principal().getName(), "header", jwt.getHeader().toJSONObject()));
			}
		} catch (JOSEException e) {
			throw error("Error verifying the status list token signature with the end-entity certificate of its x5c header", e,
				args("leaf_cert_subject", leaf.getSubjectX500Principal().getName(), "header", jwt.getHeader().toJSONObject()));
		}
		logSuccess("Status list token signature verified with the end-entity certificate of its x5c header",
			args("leaf_cert_subject", leaf.getSubjectX500Principal().getName()));
	}
}
