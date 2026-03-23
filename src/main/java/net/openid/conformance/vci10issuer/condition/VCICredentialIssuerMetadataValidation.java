package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.networknt.schema.ValidationMessage;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationInput;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationResult;

import java.util.Set;
import java.util.stream.Collectors;

public class VCICredentialIssuerMetadataValidation extends AbstractJsonSchemaBasedValidation {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();
		String schemaResource = "json-schemas/oid4vci/credential_issuer_metadata-1_0.json";
		String metadataName = "OID4VCI Credential Issuer metadata";
		return new JsonSchemaValidationInput(metadataName, schemaResource, metadata);
	}

	@Override
	protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		Set<ValidationMessage> structuralErrors = validationResult.getValidationMessages().stream()
			.filter(m -> !"additionalProperties".equals(m.getType()))
			.collect(Collectors.toSet());
		if (!structuralErrors.isEmpty()) {
			super.onValidationFailure(env, new JsonSchemaValidationResult(structuralErrors), input);
		}
	}

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
