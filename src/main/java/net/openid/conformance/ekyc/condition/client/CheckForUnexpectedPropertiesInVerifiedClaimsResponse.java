package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.networknt.schema.SchemaRegistry;
import net.openid.conformance.condition.AbstractCheckForUnexpectedSchemaProperties;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;

import java.util.function.Consumer;

public class CheckForUnexpectedPropertiesInVerifiedClaimsResponse extends AbstractCheckForUnexpectedSchemaProperties {

	private static final String SCHEMA_RESOURCE = "json-schemas/ekyc-ida/verified_claims.json";

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject wrappedClaims = env.getObject(AbstractEkycSchemaBasedValidation.EKYC_VALIDATION_INPUT_KEY);
		return new JsonSchemaValidationInput("verified_claims response", SCHEMA_RESOURCE, wrappedClaims);
	}

	@Override
	protected Consumer<SchemaRegistry.Builder> schemaBuilderCustomizer() {
		return AbstractEkycSchemaBasedValidation.ekycSchemaMapperCustomizer();
	}

	@Override
	@PreEnvironment(required = {"verified_claims_response"})
	public Environment evaluate(Environment env) {
		JsonObject claimsObject = AbstractEkycSchemaBasedValidation.extractAndWrapResponseClaims(env);
		if (claimsObject == null) {
			logSuccess("No verified claims to check for unexpected properties");
			return env;
		}

		env.putObject(AbstractEkycSchemaBasedValidation.EKYC_VALIDATION_INPUT_KEY, claimsObject);
		try {
			return super.evaluate(env);
		} finally {
			env.removeObject(AbstractEkycSchemaBasedValidation.EKYC_VALIDATION_INPUT_KEY);
		}
	}
}
