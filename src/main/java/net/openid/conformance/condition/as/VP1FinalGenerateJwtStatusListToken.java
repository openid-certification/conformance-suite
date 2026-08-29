package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.extensions.MultiJWSSignerFactory;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.oauth.statuslists.JwtStatusListTokenClaimsBuilder;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;
import java.time.Instant;

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

	public static final String ENV_KEY = "served_status_list_jwt";

	@Override
	@PreEnvironment(required = { "config", AbstractCreateStatusListReference.ENV_KEY })
	@PostEnvironment(strings = { ENV_KEY })
	public Environment evaluate(Environment env) {

		String uri = OIDFJSON.getString(
			env.getElementFromObject(AbstractCreateStatusListReference.ENV_KEY, "uri"));

		Instant iat = Instant.now();
		Instant exp = iat.plus(VP1FinalRevocationListValidity.LIFETIME);

		JsonObject claims = JwtStatusListTokenClaimsBuilder.build(uri, iat, exp,
			VP1FinalRevocationListValidity.TTL.toSeconds(), EvenOddStatusListContents.BITS,
			EvenOddStatusListContents.create().encodeStatusList(), null);

		JWK signingJwk;
		JWSAlgorithm alg;
		try {
			signingJwk = CredentialSigningJwk.fromConfig(env);
			alg = CredentialSigningJwk.signingAlgorithm(signingJwk);
		} catch (CredentialSigningJwk.Problem e) {
			throw e.getCause() == null
				? error(e.getMessage(), e.details()) : error(e.getMessage(), e.getCause(), e.details());
		}

		JWSHeader.Builder headerBuilder = new JWSHeader.Builder(alg)
			.type(new JOSEObjectType(JwtStatusListTokenClaimsBuilder.TYP))
			// a self-asserted key is not one of the resolution routes draft-ietf-oauth-status-list
			// section 11.3 recommends, so a verifier has no reason to trust it; it is here for the
			// suite's own wallet tests, whose non-HAIP signature check uses an embedded key
			.jwk(signingJwk.toPublicJWK());
		if (signingJwk.getX509CertChain() != null) {
			headerBuilder.x509CertChain(signingJwk.getX509CertChain());
		}

		String token;
		try {
			SignedJWT jwt = new SignedJWT(headerBuilder.build(), JWTClaimsSet.parse(claims.toString()));
			JWSSigner signer = MultiJWSSignerFactory.getInstance().createJWSSigner(signingJwk, alg);
			jwt.sign(signer);
			token = jwt.serialize();
		} catch (ParseException | JOSEException e) {
			throw error("Failed to sign the status list token", e,
				args("alg", alg.getName(), "kid", signingJwk.getKeyID()));
		}

		env.putString(ENV_KEY, token);

		logSuccess("Generated the Status List Token in JWT format",
			args("sub", uri, "algorithm", alg.getName(), "exp", exp.getEpochSecond(),
				"status_list_token", token));

		return env;
	}
}
