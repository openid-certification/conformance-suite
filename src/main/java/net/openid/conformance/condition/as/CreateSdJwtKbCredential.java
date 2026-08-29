package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.PreGeneratedJwks;

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
	 * defined in draft-ietf-oauth-status-list section 6.2, or null when the test allocated no
	 * status list reference - which the module presenting a credential without revocation
	 * information does deliberately, the claim being optional.
	 */
	protected Map<String, Object> statusClaims(Environment env) {
		JsonObject reference = env.getObject(AbstractCreateStatusListReference.ENV_KEY);
		if (reference == null) {
			return null;
		}

		return Map.of("status", Map.of("status_list", Map.of(
			"idx", OIDFJSON.getInt(reference.get("idx")),
			"uri", OIDFJSON.getString(reference.get("uri")))));
	}

}
