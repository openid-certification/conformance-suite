package net.openid.conformance.util;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.oauth.statuslists.CwtStatusListTokenBuilder;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.oauth.statuslists.JwtStatusListTokenClaimsBuilder;
import org.multipaz.crypto.Algorithm;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * The Token Status List that credentials issued with the suite's built-in signing certificates
 * reference, served at {@link #PATH} for as long as the suite runs. A credential issued by an
 * OpenID4VCI wallet test outlives the test, so its status must not depend on the test instance:
 * a verifier, the suite's own included, checks it later.
 *
 * <p>Its contents are {@link EvenOddStatusListContents}, the same as the list each test instance
 * serves, so an index the issuing conditions allocate means the same in both. The JWT form is
 * signed by the built-in PID provider certificate ({@link TestKeysAndCerts#getSdJwtPidSigner()}),
 * the issuer of the SD-JWT credentials that reference it; the CWT form, the one an mdoc holder
 * asks for, by the MSO revocation list signer under the suite's IACA root.
 */
public final class SuiteIssuedCredentialStatusList {

	/** Where the suite serves the status list, under its https base URL. */
	public static final String PATH = "/status-lists/issued-credentials";

	private static final Duration VALIDITY = Duration.ofHours(24);
	private static final long TTL_SECONDS = TimeUnit.HOURS.toSeconds(1);

	private static final Supplier<String> JWT = Suppliers.memoizeWithExpiration(() -> jwt(Instant.now()), 1, TimeUnit.HOURS);
	private static final Supplier<byte[]> CWT = Suppliers.memoizeWithExpiration(() -> cwt(Instant.now()), 1, TimeUnit.HOURS);

	private SuiteIssuedCredentialStatusList() {
	}

	/** The URL the suite serves the status list at, also the {@code sub} of both forms. */
	public static String url() {
		return TestKeysAndCerts.getHttpsBaseUrl() + PATH;
	}

	/** The status list in use as a Status List Token in JWT format, a compact JWS. */
	public static String jwt() {
		return JWT.get();
	}

	/** The status list in use as a Status List Token in CWT format, a tagged COSE_Sign1. */
	public static byte[] cwt() {
		return CWT.get().clone();
	}

	/** The JWT form issued at {@code issued}. */
	public static String jwt(Instant issued) {
		Instant iat = issued.truncatedTo(ChronoUnit.SECONDS);
		JsonObject claims = JwtStatusListTokenClaimsBuilder.build(url(), iat, iat.plus(VALIDITY), TTL_SECONDS,
			EvenOddStatusListContents.BITS, EvenOddStatusListContents.create().encodeStatusList(), null);
		ECKey signingKey = TestKeysAndCerts.getSdJwtPidSigner().getJwk();
		JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(new JOSEObjectType(JwtStatusListTokenClaimsBuilder.TYP))
			.keyID(signingKey.getKeyID());
		if (signingKey.getX509CertChain() != null) {
			header.x509CertChain(signingKey.getX509CertChain());
		}
		try {
			SignedJWT jwt = new SignedJWT(header.build(), JWTClaimsSet.parse(claims.toString()));
			jwt.sign(new ECDSASigner(signingKey));
			return jwt.serialize();
		} catch (ParseException | JOSEException e) {
			throw new IllegalStateException("Failed to sign the status list of credentials the suite issues", e);
		}
	}

	/** The CWT form issued at {@code issued}. */
	public static byte[] cwt(Instant issued) {
		Instant iat = issued.truncatedTo(ChronoUnit.SECONDS);
		try {
			return CwtStatusListTokenBuilder.build(url(), iat, iat.plus(VALIDITY), TTL_SECONDS,
				EvenOddStatusListContents.BITS, EvenOddStatusListContents.create().compressStatusList(),
				TestKeysAndCerts.getStatusListSignerKey(), Algorithm.ES256, null);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to sign the CWT status list of credentials the suite issues", e);
		}
	}
}
