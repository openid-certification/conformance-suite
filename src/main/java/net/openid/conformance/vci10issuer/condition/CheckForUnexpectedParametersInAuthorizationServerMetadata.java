package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCheckForUnexpectedSchemaProperties;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;

public class CheckForUnexpectedParametersInAuthorizationServerMetadata extends AbstractCheckForUnexpectedSchemaProperties {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject authorizationServerMetadata = env.getObject("server");
		return new JsonSchemaValidationInput("OAuth Authorization Server metadata",
			"json-schemas/oid4vci/rfc8414-oauth_authorization_server_metadata.json", authorizationServerMetadata);
	}

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
