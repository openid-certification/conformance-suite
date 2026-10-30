package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Verifies the Token Status List JWT signature outside HAIP, and that the key is the referenced
 * credential's issuer's, in the ways draft-ietf-oauth-status-list section 11.3 lists for a status
 * issuer that is the issuer of the referenced token: the same key as the credential's {@code x5c}
 * end-entity certificate, or a key from the same web-based resolution, here the issuer's published
 * JWK set ({@code server_jwks}).
 *
 * <p>The verification key is the end-entity certificate of the token's {@code x5c} header, its
 * {@code jwk} header, or else a key of the issuer's JWK set. A key from the token's own header proves
 * nothing until it is tied to the issuer: it must be the credential's {@code x5c} key or in the
 * issuer's JWK set. The one exception is an {@code x5c} header when a status list trust source is
 * configured: {@link ValidateStatusListTokenX5cCertificateChain} then validates that chain, which
 * covers a status issuer other than the credential's issuer.
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
			X509Certificate leaf = X509CertUtils.parse(x5c.get(0).decode());
			if (leaf == null) {
				throw error("The first certificate in the status list token's x5c header is not a valid X.509 certificate",
					args("header", statusListTokenJwt.getHeader().toJSONObject()));
			}
			verifyWithKey(statusListTokenJwt, leaf.getPublicKey(), "the end-entity certificate of its x5c header");
			requireIssuerKey(env, statusListTokenJwt, leaf.getPublicKey(), "the end-entity certificate of its x5c header", true);
			return env;
		}

		JWK headerJwk = statusListTokenJwt.getHeader().getJWK();
		if (headerJwk != null) {
			PublicKey key = publicKeyOf(headerJwk);
			if (key == null) {
				throw error("The jwk header of the status list token is not a public key the suite can verify with",
					args("header", statusListTokenJwt.getHeader().toJSONObject()));
			}
			verifyWithKey(statusListTokenJwt, key, "its jwk header");
			requireIssuerKey(env, statusListTokenJwt, key, "its jwk header", false);
			return env;
		}

		if (env.containsObject("server_jwks")) {
			verifyJwsSignature(statusListTokenJwtString, env.getObject("server_jwks"), "status list token", false, "server");
			return env;
		}

		throw error("Unable to verify the status list token signature: its header has no x5c or jwk, and the issuer's JWK set is not available",
			args("header", statusListTokenJwt.getHeader().toJSONObject()));
	}

	private void verifyWithKey(SignedJWT jwt, PublicKey key, String keySource) {
		try {
			JWSVerifier verifier = new DefaultJWSVerifierFactory().createJWSVerifier(jwt.getHeader(), key);
			if (!jwt.verify(verifier)) {
				throw error("Status list token signature does not verify with " + keySource,
					args("header", jwt.getHeader().toJSONObject()));
			}
		} catch (JOSEException e) {
			throw error("Error verifying the status list token signature with " + keySource, e,
				args("header", jwt.getHeader().toJSONObject()));
		}
	}

	private void requireIssuerKey(Environment env, SignedJWT jwt, PublicKey key, String keySource, boolean chainCanEstablishTrust) {
		PublicKey credentialKey = credentialX5cLeafKey(env);
		if (credentialKey != null && sameKey(credentialKey, key)) {
			logSuccess("Status list token signature verified with " + keySource + ", which is the key of the credential's x5c header",
				args("header", jwt.getHeader().toJSONObject()));
			return;
		}
		if (issuerJwksKeys(env).stream().anyMatch(k -> sameKey(k, key))) {
			logSuccess("Status list token signature verified with " + keySource + ", which is in the issuer's JWK set",
				args("header", jwt.getHeader().toJSONObject()));
			return;
		}
		if (chainCanEstablishTrust && env.getString(RegisterStatusListTrustAnchor.ENV_TRUST_SOURCE_CONFIGURED) != null) {
			logSuccess("Status list token signature verified with " + keySource + "; the key is not the credential's, so its"
					+ " trust rests on the validation of the x5c chain against the configured status list trust source",
				args("header", jwt.getHeader().toJSONObject()));
			return;
		}
		throw error("The status list token is signed with " + keySource + ", but that key is neither the key of the"
				+ " credential's x5c header nor in the issuer's JWK set, so nothing ties the status list to the credential's"
				+ " issuer. Sign the status list with the credential's key, or, for a separate status issuer, sign it with"
				+ " an x5c chain and configure the 'Status List Trust Anchor' in the test configuration",
			args("header", jwt.getHeader().toJSONObject(), "credential_has_x5c", credentialKey != null));
	}

	private PublicKey credentialX5cLeafKey(Environment env) {
		JsonElement x5c = env.getElementFromObject("sdjwt", "credential.header.x5c");
		if (x5c == null || !x5c.isJsonArray() || x5c.getAsJsonArray().isEmpty()) {
			return null;
		}
		JsonArray chain = x5c.getAsJsonArray();
		X509Certificate leaf = X509CertUtils.parse(new Base64(OIDFJSON.getString(chain.get(0))).decode());
		return leaf == null ? null : leaf.getPublicKey();
	}

	private List<PublicKey> issuerJwksKeys(Environment env) {
		List<PublicKey> keys = new ArrayList<>();
		JsonObject jwks = env.getObject("server_jwks");
		if (jwks == null) {
			return keys;
		}
		try {
			for (JWK jwk : JWKSet.parse(jwks.toString()).getKeys()) {
				PublicKey key = publicKeyOf(jwk);
				if (key != null) {
					keys.add(key);
				}
			}
		} catch (ParseException e) {
			// an unparseable issuer JWK set holds no key the status list can be tied to
		}
		return keys;
	}

	private static PublicKey publicKeyOf(JWK jwk) {
		if (!(jwk instanceof AsymmetricJWK asymmetric)) {
			return null;
		}
		try {
			return asymmetric.toPublicKey();
		} catch (JOSEException e) {
			return null;
		}
	}

	private static boolean sameKey(PublicKey a, PublicKey b) {
		return Arrays.equals(a.getEncoded(), b.getEncoded());
	}
}
