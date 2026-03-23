package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.networknt.schema.ValidationMessage;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.ExtractDCQLQueryFromAuthorizationRequest;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.AbstractJsonSchemaBasedValidation;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationInput;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationResult;

import java.util.Set;
import java.util.stream.Collectors;

public class WarnUnknownDCQLProperties extends AbstractJsonSchemaBasedValidation {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject dcql = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY);
		return new JsonSchemaValidationInput("DCQL query",
			"json-schemas/oid4vp/dcql_request.json", dcql);
	}

	@Override
	@PreEnvironment(required = ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY)
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		Set<ValidationMessage> additionalPropsErrors = validationResult.getValidationMessages().stream()
			.filter(m -> "additionalProperties".equals(m.getType()))
			.collect(Collectors.toSet());
		if (!additionalPropsErrors.isEmpty()) {
			throw error("Unknown properties were found in the DCQL query. This may indicate the verifier has misunderstood the spec, or it may be using extensions the test suite is unaware of.",
				args("unknown_properties", additionalPropsErrors, "input", input.getJsonObject()));
		}
	}
}
