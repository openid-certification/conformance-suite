package net.openid.conformance.sharing;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import net.openid.conformance.util.JWKUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.ott.DefaultOneTimeToken;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class AssetSharing {

	@Value("${fintechlabs.base_url}")
	private String baseURL;

	private JWK jwk;

	@PostConstruct
	public void init() {

		try {
			jwk = JWKUtil.parseJWKSet("""
				{
				    "keys": [
				        {
				            "kty": "EC",
				            "d": "KPLAwqKMcHhSUPYqOoxPF42YVZU_ZQNyaI4wVXOrJ7k",
				            "use": "sig",
				            "crv": "P-256",
				            "x": "uK2Ak1r83p5FjYmBP6TKiN_FvThvwerZZRd2vOXWX1Q",
				            "y": "2c2_xWlYCIAnm8wYF1X9c4eP4FNbG9mUTR49PgxfUko",
				            "alg": "ES256"
				        }
				    ]
				}
				""").getKeys().get(0);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

	}

	@SuppressWarnings("unchecked")
	public OneTimeToken generateSharingToken(String planId, String testId, Map<String, String> owner) {

		String tokenId = UUID.randomUUID().toString();
		String sharingToken = generateSharingToken(tokenId, planId, testId, owner);
		Duration lifetime = Duration.ofDays(1);
		String username = "Guest" + Integer.toString(tokenId.hashCode(), 32);

		return new DefaultOneTimeToken(sharingToken, username, Instant.now().plus(lifetime));
	}

	protected String generateSharingToken(String tokenId, String planId, String testId, Map<String, String> owner) {

		Instant now = Instant.now();
		Duration shareLifetime = Duration.ofDays(90);
		String audience = baseURL;
		String issuer = baseURL;

		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.jwtID(tokenId)
			.issueTime(Date.from(now))
			.expirationTime(Date.from(now.plus(shareLifetime)))
			.audience(audience)
			.issuer(issuer)
			.claim("ct_token_type", "share")
			.claim("ct_plan_id", planId)
			.claim("ct_test_id", testId)
			.claim("ct_testplan_owner", owner)
			.claim("ct_redirect_uri", baseURL + "/log-detail.html?log=" + testId)
			.build();

		// Pick an algorithm: prefer jwk.alg, else infer from key type/curve
		JWSAlgorithm alg = JWSAlgorithm.ES256;;

		JWSHeader header = new JWSHeader.Builder(alg)
			.type(JOSEObjectType.JWT)
			.keyID(jwk.getKeyID())
			.build();

		SignedJWT jwt = new SignedJWT(header, claims);

		JWSSigner signer;
		try {
			signer = new DefaultJWSSignerFactory().createJWSSigner(jwk, alg);

			jwt.sign(signer);
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}
		return jwt.serialize();
	}

	public SharedAsset getSharedAssetFromSharingToken(String token) {
		Jwt jwt = decodeSharingToken(token);
		if (jwt == null) {
			return null;
		}

		String testId = jwt.getClaimAsString("ct_test_id");
		String planId = jwt.getClaimAsString("ct_plan_id");
		Map<String, String> ctTestplanOwner = jwt.getClaim("ct_testplan_owner");
		String redirectUri = jwt.getClaimAsString("ct_redirect_uri");

		return new SharedAsset(jwt.getId(), planId, testId, ctTestplanOwner, redirectUri);
	}

	public Jwt decodeSharingToken(String tokenValue) {
		SignedJWT signed;
		try {
			signed = SignedJWT.parse(tokenValue);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		JWSVerifier verifier;
		try {
			verifier = new DefaultJWSVerifierFactory()
				.createJWSVerifier(signed.getHeader(), jwk.toPublicJWK().toECKey().toPublicKey());
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}

		try {
			if (!signed.verify(verifier)) {
				throw new IllegalArgumentException("Invalid signature");
			}
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}

		JWTClaimsSet claims;
		try {
			claims = signed.getJWTClaimsSet();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		return new Jwt(
			tokenValue,
			claims.getIssueTime().toInstant(),
			claims.getExpirationTime().toInstant(),
			Map.of("alg", signed.getHeader().getAlgorithm().getName()),
			claims.getClaims()
		);
	}
}
