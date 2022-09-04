package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveRequestObjectEncryptionValuesFromServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");
		if(server.has("request_object_encryption_alg_values_supported")) {
			server.remove("request_object_encryption_alg_values_supported");
		}
		if(server.has("request_object_encryption_enc_values_supported")) {
			server.remove("request_object_encryption_enc_values_supported");
		}

		log("Removed request_object_encryption_alg_values_supported and request_object_encryption_enc_values_supported" +
			" from server configuration", args("server", server));
		return env;
	}

}
