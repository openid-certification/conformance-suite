package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCheckForUnexpectedSchemaProperties;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;

public class CheckForUnexpectedParametersInServerMetadata extends AbstractCheckForUnexpectedSchemaProperties {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject serverMetadata = env.getObject("server");
		return new JsonSchemaValidationInput("OAuth Authorization Server metadata",
			"json-schemas/rfc8414/oauth_authorization_server_metadata.json", serverMetadata);
	}

	@Override
	protected String getAllowUnexpectedFieldsConfigKey() {
		// Hidden escape hatch: a tester can add this JSON array of property names to the test
		// configuration to suppress warnings for extension metadata their authorization server
		// legitimately publishes.
		return "server.allow_unexpected_metadata_fields";
	}

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
