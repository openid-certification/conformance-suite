package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCheckForUnexpectedSchemaProperties;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;

public class CheckForUnexpectedParametersInIssuerInfoRegistrarDataset extends AbstractCheckForUnexpectedSchemaProperties {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject dataset = env.getObject(VCIEnsureIssuerInfoContainsOneRegistrarDataset.ENV_KEY);
		return new JsonSchemaValidationInput("issuer_info registrar_dataset",
			"json-schemas/etsi/registrar_dataset.json", dataset);
	}

	@Override
	@PreEnvironment(required = VCIEnsureIssuerInfoContainsOneRegistrarDataset.ENV_KEY)
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
