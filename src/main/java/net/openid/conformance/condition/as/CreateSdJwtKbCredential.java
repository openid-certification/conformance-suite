package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.PreGeneratedJwks;

import java.util.LinkedHashMap;
import java.util.Map;

public class CreateSdJwtKbCredential extends AbstractCreateSdJwtCredential {

	@Override
	@PostEnvironment(strings = {"credential", "holder_private_jwk"})
	public Environment evaluate(Environment env) {

		// Create a private key for the credential key binding
		ECKey privateKey = PreGeneratedJwks.nextEcKey(env, Curve.P_256);
		String sdJwt = createSdJwt(env, privateKey.toPublicJWK(), privateKey, "urn:eudi:pid:1",
			statusClaims(env));

		env.putString("credential", sdJwt);
		env.putString("holder_private_jwk", privateKey.toJSONString());

		log("Created an SD-JWT+KB", args("sdjwt", sdJwt));

		return env;

	}

	/**
	 * The {@code status} claim referencing the Token Status List this test instance serves, as
	 * defined in draft-ietf-oauth-status-list section 6.2, or null when the test did not allocate
	 * a status list reference (the usual case — a status reference is optional).
	 */
	protected Map<String, Object> statusClaims(Environment env) {
		JsonObject reference = env.getObject(CreateRevokedStatusListReference.ENV_KEY);
		if (reference == null) {
			return additionalClaims;
		}

		Map<String, Object> statusList = new LinkedHashMap<>();
		statusList.put("idx", OIDFJSON.getInt(reference.get("idx")));
		statusList.put("uri", OIDFJSON.getString(reference.get("uri")));

		Map<String, Object> claims = additionalClaims == null
			? new LinkedHashMap<>() : new LinkedHashMap<>(additionalClaims);
		claims.put("status", Map.of("status_list", statusList));
		return claims;
	}

}
