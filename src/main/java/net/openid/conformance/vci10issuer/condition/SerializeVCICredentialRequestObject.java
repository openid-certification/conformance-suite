package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SerializeVCICredentialRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci_credential_request_object")
	@PostEnvironment(strings = "resource_request_entity")
	public Environment evaluate(Environment env) {
		JsonObject credentialRequestObject = env.getObject("vci_credential_request_object");
		String requestBodyString = credentialRequestObject.toString();

		env.putString("resource_request_entity", requestBodyString);

		logSuccess("Serialized credential request object",
			args("credential_request_object", credentialRequestObject));

		return env;
	}
}
