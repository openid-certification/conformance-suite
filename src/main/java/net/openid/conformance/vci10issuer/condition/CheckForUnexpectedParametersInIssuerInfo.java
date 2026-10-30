package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCheckForUnexpectedSchemaProperties;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;

public class CheckForUnexpectedParametersInIssuerInfo extends AbstractCheckForUnexpectedSchemaProperties {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject wrapper = env.getObject(VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata.ENV_KEY);
		return new JsonSchemaValidationInput("issuer_info",
			"json-schemas/etsi/issuer_info.json", wrapper);
	}

	@Override
	@PreEnvironment(required = VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata.ENV_KEY)
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
