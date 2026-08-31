package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilAddRequiredIdTokenEncryptionToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		JsonObject request = env.getObject("dynamic_registration_request");
		request.addProperty("id_token_encrypted_response_alg", "RSA-OAEP");
		request.addProperty("id_token_encrypted_response_enc", "A256GCM");
		env.putObject("dynamic_registration_request", request);

		logSuccess("Added Open Finance Brazil required ID Token encryption metadata to dynamic registration request",
			args("dynamic_registration_request", request));
		return env;
	}
}
