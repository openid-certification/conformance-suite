package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateDocumentsVerificationMethodsSupportedInServerConfiguration extends AbstractCondition {

	//TODO Yes.com returns something similar to the following for verification_methods_supported.
	// Structure of these OP items are not defined in spec
	//  {
	//    "identity_document": [
	//      "Physical In-Person Proofing (bank)",
	//      "Physical In-Person Proofing (shop)",
	//      "Physical In-Person Proofing (courier)",
	//      "Supervised remote In-Person Proofing"
	//    ]
	//  },
	//  "qes",
	//  "eID"
	//]
	//documents_verification_methods_supported: OPTIONAL.
	// JSON array containing the verification methods the OP supports (see @!predefined_values).
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement jsonElement = env.getElementFromObject("server", "documents_verification_methods_supported");
		if(jsonElement == null) {
			log("Server configuration does not contain a documents_verification_methods_supported entry");
			return env;
		}
		if(!jsonElement.isJsonArray()) {
			throw error("documents_verification_methods_supported must be a json array", args("actual", jsonElement));
		}

		logSuccess("documents_verification_methods_supported is valid", args("actual", jsonElement));
		return env;
	}

}
