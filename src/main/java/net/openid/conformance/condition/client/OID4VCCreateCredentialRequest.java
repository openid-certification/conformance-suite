package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.openqa.selenium.json.Json;

public class OID4VCCreateCredentialRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "proof_jwt")
	@PostEnvironment(strings = "resource_request_entity")
	public Environment evaluate(Environment env) {
		String proofJwt = env.getString("proof_jwt");
		JsonObject o = new JsonObject();

		// FIXME: there may be multiple credential type entries, we
		// probably need a higher level loop to request each one
		String type = env.getString("issuance_initiation_request", "credential_type");

		o.addProperty("type", type);
		o.addProperty("format", "jwt_vc"); // FIXME needs to be configurable?

		JsonObject proof = new JsonObject();
		proof.addProperty("proof_type", "jwt"); // FIXME needs to be configurable?
		proof.addProperty("jwt", proofJwt);

		o.add("proof", proof);

		env.putString("resource_request_entity", o.toString());

		log("Created credential request", o);

		return env;
	}

}
