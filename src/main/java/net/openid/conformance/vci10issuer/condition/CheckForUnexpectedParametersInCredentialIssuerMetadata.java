package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCheckForUnexpectedSchemaProperties;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;

public class CheckForUnexpectedParametersInCredentialIssuerMetadata extends AbstractCheckForUnexpectedSchemaProperties {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();
		return new JsonSchemaValidationInput("OID4VCI Credential Issuer metadata",
			"json-schemas/oid4vci/credential_issuer_metadata-1_0.json", metadata);
	}

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
