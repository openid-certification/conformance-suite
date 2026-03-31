package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.networknt.schema.ValidationMessage;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidation;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationInput;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationResult;

import java.util.ArrayList;
import java.util.List;

public class CheckForUnexpectedPropertiesInVerifiedClaimsRequest extends AbstractEkycSchemaBasedValidation {

	private static final String SCHEMA_RESOURCE = "json-schemas/ekyc-ida/verified_claims_request.json";

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject authorizationRequest = env.getObject("authorization_endpoint_request");
		JsonObject claims = authorizationRequest.has("claims") ? authorizationRequest.getAsJsonObject("claims") : null;
		return new JsonSchemaValidationInput("verified_claims request", SCHEMA_RESOURCE, claims);
	}

	@Override
	protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		JsonSchemaValidationResult additionalPropsResult = validationResult.onlyAdditionalPropertiesErrors();
		if (!additionalPropsResult.isValid()) {
			List<JsonObject> unknownProps = new ArrayList<>();
			for (ValidationMessage msg : additionalPropsResult.getValidationMessages()) {
				JsonObject entry = new JsonObject();
				entry.addProperty("property", msg.getProperty());
				entry.addProperty("path", JsonSchemaValidation.toInstancePropertyPath(msg.getInstanceLocation(), msg.getProperty()));
				unknownProps.add(entry);
			}
			throw error("Unknown properties were found in the " + input.getInputName()
					+ ". This may indicate the sender has misunderstood the spec, or it may be using extensions the test suite is unaware of.",
				args("unknown_properties", unknownProps, "input", input.getJsonObject(), "schema_link", "/" + input.getSchemaResource()));
		}
	}

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	public Environment evaluate(Environment env) {
		JsonObject authorizationRequest = env.getObject("authorization_endpoint_request");
		if (!authorizationRequest.has("claims")) {
			logSuccess("No claims to check for unexpected properties");
			return env;
		}
		return super.evaluate(env);
	}
}
