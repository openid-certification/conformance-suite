package net.openid.conformance.poppy.condition;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.poppy.PoppyConstants;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class CreatePoppyDelegation extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client_jwks", "poppy_delegated_key"}, strings = "client_id")
	@PostEnvironment(strings = "poppy_delegation")
	public Environment evaluate(Environment env) {
		String clientId = env.getString("client_id");

		ECKey signingKey = findSigningKey(env);
		ECKey delegated;
		try {
			delegated = ECKey.parse(env.getObject("poppy_delegated_key").toString());
		} catch (ParseException e) {
			throw error("The delegated key is not a valid EC JWK", e);
		}

		Instant now = Instant.now();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.issuer(clientId)
			.claim("cnf", Map.of("jwk", delegated.toPublicJWK().toJSONObject()))
			.issueTime(Date.from(now))
			.expirationTime(Date.from(now.plusSeconds(PoppyConstants.DELEGATION_LIFETIME_SECONDS)))
			.jwtID(RandomStringUtils.secure().nextAlphanumeric(24))
			.build();

		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(new JOSEObjectType(PoppyConstants.DELEGATION_JWT_TYP))
			.keyID(signingKey.getKeyID())
			.build();

		SignedJWT jwt = new SignedJWT(header, claims);
		try {
			jwt.sign(new ECDSASigner(signingKey));
		} catch (JOSEException e) {
			throw error("Failed to sign the delegation", e);
		}

		String serialized = jwt.serialize();
		env.putString("poppy_delegation", serialized);

		logSuccess("Created the delegation signed with the key published at jwks_uri",
			args("delegation", serialized, "kid", signingKey.getKeyID(), "claims", claims.toJSONObject()));
		return env;
	}

	private ECKey findSigningKey(Environment env) {
		JWKSet set;
		try {
			set = JWKSet.parse(env.getObject("client_jwks").toString());
		} catch (ParseException e) {
			throw error("The client JWKs in the test configuration could not be parsed", e);
		}
		for (JWK jwk : set.getKeys()) {
			if (jwk instanceof ECKey ec && jwk.isPrivate()
				&& (JWSAlgorithm.ES256.equals(jwk.getAlgorithm()) || (jwk.getAlgorithm() == null && Curve.P_256.equals(ec.getCurve())))) {
				return ec;
			}
		}
		throw error("The client JWKs in the test configuration contain no ES256 (EC P-256) private key. Poppy requires the delegation to be signed with ES256 by a key published at the client's jwks_uri.",
			args("client_jwks", env.getObject("client_jwks")));
	}
}
