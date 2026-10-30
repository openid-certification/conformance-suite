package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractJsonSchemaBasedValidation;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;

/**
 * Validates the data of the {@code registrar_dataset} element of {@code issuer_info}: the members
 * ETSI TS 119 472-3 requires (identifier, srvDescription, registryURI, providesAttestations) with the
 * types ETSI TS 119 475 Annex B gives them.
 */
public class VCIValidateIssuerInfoRegistrarDataset extends AbstractJsonSchemaBasedValidation {

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

	/**
	 * Unknown properties are surfaced (as a warning) by
	 * {@link CheckForUnexpectedParametersInIssuerInfoRegistrarDataset}, not by this condition.
	 */
	@Override
	protected boolean ignoreUnknownPropertyStrictness() {
		return true;
	}
}
