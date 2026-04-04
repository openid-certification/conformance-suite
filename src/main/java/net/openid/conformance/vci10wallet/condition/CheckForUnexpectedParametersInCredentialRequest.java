package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCheckForUnexpectedSchemaProperties;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;

public class CheckForUnexpectedParametersInCredentialRequest extends AbstractCheckForUnexpectedSchemaProperties {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject credentialRequestBodyJson = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();
		return new JsonSchemaValidationInput("OID4VCI Credential Request",
			"json-schemas/oid4vci/credential_request-1_0.json", credentialRequestBodyJson);
	}

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
