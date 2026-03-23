package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationInput;

public class CheckForUnexpectedParametersInAuthorizationServerMetadata extends AbstractCheckForUnexpectedSchemaProperties {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		String currentAuthServerMetadataPath = env.getEffectiveKey("current_auth_server_metadata_path");
		JsonObject authorizationServerMetadata = env.getElementFromObject("vci", currentAuthServerMetadataPath).getAsJsonObject();
		return new JsonSchemaValidationInput("OAuth Authorization Server metadata",
			"json-schemas/oid4vci/rfc8414-oauth_authorization_server_metadata.json", authorizationServerMetadata);
	}

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
