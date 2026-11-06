package net.openid.conformance.poppy.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.poppy.PoppyConstants;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.time.Instant;
import java.util.List;

public class ValidatePoppyDelegation extends AbstractCondition {

	private static final List<String> PRIVATE_MEMBERS = List.of("d", "p", "q", "dp", "dq", "qi", "oth", "k");
	private static final List<String> FORBIDDEN_CLAIMS = List.of("sub", "aud", "agent_id");

	@Override
	@PreEnvironment(required = "client", strings = "poppy_session_jwt")
	@PostEnvironment(required = {"poppy_delegation_claims", "poppy_delegated_public_jwk"})
	public Environment evaluate(Environment env) {
		String sessionJwt = env.getString("poppy_session_jwt");

		SignedJWT session;
		String sessionIss;
		try {
			session = SignedJWT.parse(sessionJwt);
			sessionIss = PoppyUtil.stringMember(JsonParser.parseString(session.getPayload().toString()).getAsJsonObject(), "iss");
		} catch (ParseException | RuntimeException e) {
			throw error("The Session JWT could not be parsed", e, args("poppy_session_jwt", sessionJwt));
		}

		Object dlgParam = session.getHeader().getCustomParam("poppy_dlg");
		if (!(dlgParam instanceof String dlg) || dlg.isEmpty()) {
			throw error("The Session JWT has no 'poppy_dlg' header carrying a delegation. Section 5.2 requires the Company to reject a Session JWT without a delegation, including one signed directly with a key from jwks_uri, with invalid_session.",
				args("header", session.getHeader().toJSONObject()));
		}

		SignedJWT delegation;
		try {
			delegation = SignedJWT.parse(dlg);
		} catch (ParseException e) {
			throw error("The delegation in 'poppy_dlg' is not a signed JWT with a supported 'alg'. Section 5.2 allows ES256 or PS384.", e, args("poppy_dlg", dlg));
		}

		JWSAlgorithm alg = delegation.getHeader().getAlgorithm();
		if (!JWSAlgorithm.ES256.equals(alg) && !JWSAlgorithm.PS384.equals(alg)) {
			throw error("The delegation 'alg' is '" + alg + "' but section 5.2 allows only ES256 or PS384",
				args("header", delegation.getHeader().toJSONObject()));
		}
		if (delegation.getHeader().getType() == null || !PoppyConstants.DELEGATION_JWT_TYP.equals(delegation.getHeader().getType().getType())) {
			throw error("The delegation 'typ' header must be '" + PoppyConstants.DELEGATION_JWT_TYP + "' (section 5.2)",
				args("header", delegation.getHeader().toJSONObject()));
		}

		verifySignature(env, delegation);

		JsonObject claims;
		try {
			claims = JsonParser.parseString(delegation.getPayload().toString()).getAsJsonObject();
		} catch (RuntimeException e) {
			throw error("The delegation payload is not a JSON object", e);
		}

		for (String forbidden : FORBIDDEN_CLAIMS) {
			if (claims.has(forbidden)) {
				throw error("The delegation contains the claim '" + forbidden + "'. Section 5.2 forbids putting a User ID, an Agent ID, a Company's issuer or anything else about the User or Companies in a delegation.",
					args("claims", claims));
			}
		}

		String iss = PoppyUtil.stringMember(claims, "iss");
		if (iss == null || !iss.equals(sessionIss)) {
			throw error("The delegation 'iss' is not the Session JWT's 'iss' (section 5.2)",
				args("delegation_iss", iss, "session_iss", sessionIss));
		}
		String jti = PoppyUtil.stringMember(claims, "jti");
		if (jti == null || jti.isEmpty()) {
			throw error("The delegation has no 'jti' string claim (section 5.2)", args("claims", claims));
		}
		Long iat = PoppyUtil.longMember(claims, "iat");
		Long exp = PoppyUtil.longMember(claims, "exp");
		if (iat == null || exp == null) {
			throw error("The delegation must contain numeric 'iat' and 'exp' claims (section 5.2)", args("claims", claims));
		}
		if (exp <= Instant.now().getEpochSecond()) {
			throw error("The delegation has expired. Section 5.2 forbids signing a Session JWT after its delegation's 'exp'.",
				args("exp", exp, "now", Instant.now().getEpochSecond()));
		}
		if (exp - iat > PoppyConstants.DELEGATION_MAX_LIFETIME_SECONDS) {
			throw error("The delegation's 'exp' is more than 24 hours after its 'iat' (section 5.2)",
				args("iat", iat, "exp", exp, "lifetime_seconds", exp - iat));
		}

		JsonElement cnf = claims.get("cnf");
		JsonElement jwkElement = cnf != null && cnf.isJsonObject() ? cnf.getAsJsonObject().get("jwk") : null;
		if (jwkElement == null || !jwkElement.isJsonObject()) {
			throw error("The delegation has no 'cnf.jwk' (RFC 7800) naming the delegated public key", args("claims", claims));
		}
		JsonObject jwk = jwkElement.getAsJsonObject();
		for (String member : PRIVATE_MEMBERS) {
			if (jwk.has(member)) {
				throw error("The delegation's 'cnf.jwk' contains the private key member '" + member + "'. Section 5.2 forbids private key members.",
					args("cnf_jwk_members", jwk.keySet()));
			}
		}

		env.putObject("poppy_delegation_claims", claims);
		env.putObject("poppy_delegated_public_jwk", jwk);
		logSuccess("The delegation is valid", args("delegation_claims", claims, "alg", alg.getName()));
		return env;
	}

	private void verifySignature(Environment env, SignedJWT delegation) {
		String kid = delegation.getHeader().getKeyID();
		if (kid == null) {
			throw error("The delegation has no 'kid' header naming the jwks_uri key that signed it (section 5.2)",
				args("header", delegation.getHeader().toJSONObject()));
		}
		JWKSet set;
		try {
			set = JWKSet.parse(env.getElementFromObject("client", "jwks").toString());
		} catch (ParseException | RuntimeException e) {
			throw error("The Agent's jwks_uri key set could not be parsed", e);
		}
		JWK key = set.getKeyByKeyId(kid);
		if (key == null) {
			throw error("The delegation's 'kid' does not name a key in the Agent's jwks_uri, so the Company answers with invalid_session (section 5.2)",
				args("kid", kid, "jwks", env.getElementFromObject("client", "jwks")));
		}

		boolean ok;
		try {
			JWSVerifier verifier;
			if (key instanceof ECKey ec && JWSAlgorithm.ES256.equals(delegation.getHeader().getAlgorithm())) {
				verifier = new ECDSAVerifier(ec.toPublicJWK());
			} else if (key instanceof RSAKey rsa && JWSAlgorithm.PS384.equals(delegation.getHeader().getAlgorithm())) {
				verifier = new RSASSAVerifier(rsa.toPublicJWK());
			} else {
				throw error("The delegation's 'alg' does not fit the type of the jwks_uri key named by its 'kid'",
					args("alg", delegation.getHeader().getAlgorithm().getName(), "kty", key.getKeyType().getValue(), "kid", kid));
			}
			ok = delegation.verify(verifier);
		} catch (JOSEException e) {
			throw error("The delegation signature could not be verified with the jwks_uri key", e, args("kid", kid));
		}
		if (!ok) {
			throw error("The delegation's signature is not valid for the jwks_uri key named by its 'kid'", args("kid", kid));
		}
	}
}
