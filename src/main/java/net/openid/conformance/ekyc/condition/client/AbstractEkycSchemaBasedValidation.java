package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.networknt.schema.JsonSchemaFactory;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.condition.AbstractJsonSchemaBasedValidation;
import net.openid.conformance.util.validation.JsonSchemaValidation;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;

import java.util.function.Consumer;

/**
 * Base class for eKYC schema validation conditions. Configures the schema mapper
 * for eKYC cross-schema $ref resolution.
 */
public abstract class AbstractEkycSchemaBasedValidation extends AbstractJsonSchemaBasedValidation {

	private static final String EKYC_SCHEMA_PREFIX = "https://bitbucket.org/openid/ekyc-ida/raw/master/schema/";
	private static final String EKYC_RESOURCE_PREFIX = "resource:json-schemas/ekyc-ida/";

	static final String EKYC_VALIDATION_INPUT_KEY = "ekyc_schema_validation_input";

	@Override
	protected JsonSchemaValidation createJsonSchemaValidation(JsonSchemaValidationInput input) {
		JsonSchemaValidation validation = super.createJsonSchemaValidation(input);
		validation.setSchemaBuilderCustomizer(ekycSchemaMapperCustomizer());
		return validation;
	}

	static Consumer<JsonSchemaFactory.Builder> ekycSchemaMapperCustomizer() {
		return builder -> builder.schemaMappers(schemaMappers ->
			schemaMappers.mapPrefix(EKYC_SCHEMA_PREFIX, EKYC_RESOURCE_PREFIX));
	}

	/**
	 * Extracts verified claims from the response, checking userinfo then id_token,
	 * and wraps in {"verified_claims": ...} for schema validation.
	 *
	 * <p>This prefers userinfo over id_token when both are present. This works
	 * correctly because AbstractEKYCTestWithOIDCCore processes id_token first
	 * (calling schema validation before userinfo is extracted), then processes
	 * userinfo second. If this ordering changes, both locations may not be
	 * validated. Ideally this should be refactored to explicitly target a
	 * specific location rather than relying on processing order.</p>
	 *
	 * @return the wrapped claims object, or null if no claims element found
	 */
	protected static JsonObject extractAndWrapResponseClaims(Environment env) {
		JsonObject verifiedClaimsResponse = env.getObject("verified_claims_response");
		JsonElement claimsElement;

		if (verifiedClaimsResponse.has("userinfo")) {
			claimsElement = verifiedClaimsResponse.get("userinfo");
		} else {
			claimsElement = verifiedClaimsResponse.get("id_token");
		}
		if (claimsElement == null) {
			return null;
		}

		JsonObject claimsObject = new JsonObject();
		claimsObject.add("verified_claims", claimsElement);
		return claimsObject;
	}
}
