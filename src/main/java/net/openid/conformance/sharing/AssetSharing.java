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
import net.openid.conformance.security.KeyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
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

	@Autowired
	private KeyManager keyManager;

	private JWK jwk;
	private JWSVerifier cachedVerifier;

	@PostConstruct
	public void init() {
		jwk = keyManager.getPrivateLinkKey();
		try {
			cachedVerifier = new DefaultJWSVerifierFactory()
				.createJWSVerifier(
					new JWSHeader.Builder(new JWSAlgorithm(jwk.getAlgorithm().toString())).build(),
					jwk.toPublicJWK().toRSAKey().toPublicKey());
		} catch (JOSEException e) {
			throw new IllegalStateException("Failed to create JWS verifier for private link key", e);
		}
	}

	public OneTimeToken generateSharingToken(String planId, Map<String, String> owner, String exp) {
		return generateSharingToken(planId, null, owner, exp);
	}

	@SuppressWarnings("unchecked")
	public OneTimeToken generateSharingToken(String planId, String testId, Map<String, String> owner, String exp) {

		int expInt;

		try {
			expInt = Integer.parseInt(exp);
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid expiration value: " + exp);
		}

		if (expInt < 1) {
			throw new IllegalArgumentException("Expiration must be at least 1 day");
		}

		Duration lifetime = Duration.ofDays(expInt);
		String tokenId = UUID.randomUUID().toString();
		String sharingToken = generateSharingToken(tokenId, planId, testId, owner, lifetime);
		String username = "Guest" + Integer.toString(tokenId.hashCode(), 32);

		return new DefaultOneTimeToken(sharingToken, username, Instant.now().plus(lifetime));
	}

	protected String generateSharingToken(String tokenId, String planId, String testId, Map<String, String> owner, Duration shareLifetime) {

		Instant now = Instant.now();
		String audience = baseURL;
		String issuer = baseURL;

		String redirectUri;

		if (testId == null) {
			// Share a test plan
			redirectUri = baseURL + "/plan-detail.html?plan=" + planId;
		}
		else {
			// Share test results.
			redirectUri = baseURL + "/log-detail.html?log=" + testId;
		}

		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.jwtID(tokenId)
			.issueTime(Date.from(now))
			.expirationTime(Date.from(now.plus(shareLifetime)))
			.audience(audience)
			.issuer(issuer)
			.claim("ct_plan_id", planId)
			.claim("ct_test_id", testId)
			.claim("ct_testplan_owner", owner)
			.claim("ct_redirect_uri", redirectUri)
			.build();

		JWSAlgorithm alg = new JWSAlgorithm(jwk.getAlgorithm().toString());

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

	public Map<String, String> generateShareLink(String planId, Map<String, String> owner, String exp) {
		return generateShareLink(planId, null, owner, exp);
	}

	public Map<String, String> generateShareLink(String planId, String testId, Map<String, String> owner, String exp) {
		OneTimeToken oneTimeToken = generateSharingToken(planId, testId, owner, exp);
		String supplementalMessage = keyManager.privateLinkKeyWasConfigured() ? "" : "INFO: This link will be invalidated on a server restart";
		return Map.of(
			"link", baseURL + "/login.html?token=" + oneTimeToken.getTokenValue(),
			"message", supplementalMessage);
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
			throw new BadCredentialsException("Invalid sharing token JWS");
		}

		try {
			if (!signed.verify(cachedVerifier)) {
				throw new BadCredentialsException("Invalid sharing token JWS signature");
			}
		} catch (JOSEException e) {
			throw new BadCredentialsException("Sharing token JWS signature verification JOSE exception");
		}

		JWTClaimsSet claims;
		try {
			claims = signed.getJWTClaimsSet();
		} catch (ParseException e) {
			throw new BadCredentialsException("Invalid sharing token JWT claims set");
		}

		if (claims.getExpirationTime().before(Date.from(Instant.now()))) {
			throw new BadCredentialsException("JWT has expired");
		}

		if (!claims.getAudience().contains(baseURL)) {
			throw new BadCredentialsException("Invalid sharing token audience");
		}

		if (!baseURL.equals(claims.getIssuer())) {
			throw new BadCredentialsException("Invalid sharing token issuer");
		}

		try {
			return new Jwt(
				tokenValue,
				claims.getIssueTime().toInstant(),
				claims.getExpirationTime().toInstant(),
				Map.of("alg", signed.getHeader().getAlgorithm().getName()),
				claims.getClaims()
			);
		} catch (java.lang.IllegalArgumentException e) {
			throw new BadCredentialsException("JWT construction error: " + e.getMessage());
		}
	}
}
