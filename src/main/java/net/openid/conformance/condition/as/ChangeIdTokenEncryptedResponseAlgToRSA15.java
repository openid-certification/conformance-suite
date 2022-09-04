package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ChangeIdTokenEncryptedResponseAlgToRSA15 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
		client.addProperty("id_token_encrypted_response_alg", "RSA1_5");
		log("Changed id_token_encrypted_response_alg to RSA1_5");
		return env;
	}
}
