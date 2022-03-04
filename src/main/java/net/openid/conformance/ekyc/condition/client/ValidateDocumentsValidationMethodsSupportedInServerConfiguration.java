package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateDocumentsValidationMethodsSupportedInServerConfiguration extends AbstractCondition {

	//documents_validation_methods_supported: OPTIONAL. JSON array containing the document validation methods
	// the OP supports (see @!predefined_values).
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement jsonElement = env.getElementFromObject("server", "documents_validation_methods_supported");
		if(jsonElement == null) {
			log("documents_validation_methods_supported is not set");
			return env;
		}
		if(!jsonElement.isJsonArray()) {
			throw error("documents_validation_methods_supported must be a json array", args("actual", jsonElement));
		}

		logSuccess("documents_validation_methods_supported is valid", args("actual", jsonElement));
		return env;
	}
}
