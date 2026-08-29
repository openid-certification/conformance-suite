package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.extensions.MultiJWSSignerFactory;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Generates the Status List Token, in JWT format, for the status list the emulated wallet's
 * SD-JWT VC credential references (draft-ietf-oauth-status-list section 5.1).
 *
 * <p>It is signed with the same key as the credential itself — the 'Signing JWK' from the
 * 'Credential Issuer' section of the test configuration, including its certificate chain — so a
 * verifier that is configured to trust the issuer of the presented credential can verify the
 * status list without any further configuration.
 *
 * <p>Stores the token in {@code served_status_list_jwt}.
 */
public class VP1FinalGenerateJwtStatusListToken extends AbstractCondition {

	public static final String STATUS_LIST_JWT_CONTENT_TYPE = "application/statuslist+jwt";

	public static final String ENV_KEY = "served_status_list_jwt";

	@Override
	@PreEnvironment(required = { "config", CreateRevokedStatusListReference.ENV_KEY })
	@PostEnvironment(strings = { ENV_KEY })
	public Environment evaluate(Environment env) {

		String uri = OIDFJSON.getString(
			env.getElementFromObject(CreateRevokedStatusListReference.ENV_KEY, "uri"));

		TokenStatusList statusList = EvenOddStatusListContents.create();

		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(10 * 60);

		Map<String, Object> statusListClaim = new LinkedHashMap<>();
		statusListClaim.put("bits", EvenOddStatusListContents.BITS);
		statusListClaim.put("lst", statusList.encodeStatusList());

		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", uri);
		claims.put("iat", iat.getEpochSecond());
		claims.put("exp", exp.getEpochSecond());
		claims.put("ttl", TimeUnit.MINUTES.toSeconds(12));
		claims.put("status_list", statusListClaim);

		JWK signingJwk = credentialSigningJwk(env);
		JWSAlgorithm alg = signingAlgorithm(signingJwk);

		JWSHeader.Builder headerBuilder = new JWSHeader.Builder(alg)
			.type(new JOSEObjectType("statuslist+jwt"));
		if (signingJwk.getX509CertChain() != null) {
			headerBuilder.x509CertChain(signingJwk.getX509CertChain());
		}

		SignedJWT jwt;
		try {
			jwt = new SignedJWT(headerBuilder.build(), JWTClaimsSet.parse(claims));
			JWSSigner signer = MultiJWSSignerFactory.getInstance().createJWSSigner(signingJwk, alg);
			jwt.sign(signer);
		} catch (ParseException | JOSEException e) {
			throw error("Failed to sign the status list token", e,
				args("alg", alg.getName(), "kid", signingJwk.getKeyID()));
		}

		env.putString(ENV_KEY, jwt.serialize());

		logSuccess("Generated the Status List Token in JWT format",
			args("sub", uri, "algorithm", alg.getName(), "exp", exp.getEpochSecond(),
				"status_list_token", jwt.serialize()));

		return env;
	}

	private JWK credentialSigningJwk(Environment env) {
		JsonElement signingJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		if (signingJwkEl == null) {
			throw error("'Signing JWK' field is missing from the 'Credential Issuer' section in the"
				+ " test configuration");
		}
		try {
			return JWK.parse(signingJwkEl.toString());
		} catch (ParseException e) {
			throw error("Failed to parse the 'Signing JWK' field in the 'Credential Issuer' section"
				+ " of the test configuration", e, args("signing_jwk", signingJwkEl));
		}
	}

	private JWSAlgorithm signingAlgorithm(JWK signingJwk) {
		if (signingJwk.getAlgorithm() != null) {
			return JWSAlgorithm.parse(signingJwk.getAlgorithm().getName());
		}
		// same default as the SD-JWT credential this status list is referenced from
		if (signingJwk instanceof ECKey) {
			return JWSAlgorithm.ES256;
		}
		throw error("'Signing JWK' field in the 'Credential Issuer' section of the test"
			+ " configuration must include an 'alg' claim specifying the signing algorithm, as"
			+ " there is no default for this key type",
			args("kty", signingJwk.getKeyType().getValue(), "kid", signingJwk.getKeyID()));
	}
}
