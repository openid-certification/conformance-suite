package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCheckForUnexpectedSchemaProperties;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;

/**
 * Surfaces unknown / unexpected properties in an SD-JWT VC Type Metadata
 * document as a separate finding so the caller can wire them at WARNING.
 *
 * Per draft-13 §6.2 receivers MUST ignore properties they don't understand,
 * but the conformance suite tests senders: a property the spec doesn't
 * define is typically a typo or misunderstanding by the issuer, and we flag
 * it so the issuer can correct it.
 */
public class CheckForUnexpectedParametersInSdJwtVcTypeMetadata extends AbstractCheckForUnexpectedSchemaProperties {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject typeMetadata = env.getElementFromObject("vci", "sdjwt_vc_type_metadata").getAsJsonObject();
		return new JsonSchemaValidationInput("SD-JWT VC Type Metadata",
			"json-schemas/sdjwtvc/type_metadata.json", typeMetadata);
	}
}
