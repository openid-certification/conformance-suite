package net.openid.conformance.condition.client.ekyc;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractEnsureJsonArray;
import net.openid.conformance.condition.client.AbstractValidateJsonBoolean;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateIdDocumentsVerificationMethodsSupportedInServerConfiguration extends AbstractCondition
{

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		//id_documents_verification_methods_supported: JSON array containing the ID document verification methods the OP supports as defined in Section 5.1.
		JsonElement jsonElement = env.getElementFromObject("server", "id_documents_verification_methods_supported");
		if(jsonElement == null) {
			//TODO is this required?
			throw error("Server configuration does not contain an id_documents_verification_methods_supported entry");
		}
		if(!jsonElement.isJsonArray()) {
			throw error("id_documents_verification_methods_supported must be a json array", args("actual", jsonElement));
		}
		JsonArray methods = jsonElement.getAsJsonArray();
		int validVerificationElementCount = 0;
		for(JsonElement verificationElement : methods) {
			if(verificationElement.isJsonNull()) {
				//TODO is json null allowed here?
			} else {
				validVerificationElementCount++;
				//TODO clarify additional validation requirements
			}
		}
		if(validVerificationElementCount<1) {
			//TODO is the following correct?
			throw error("At least one verification element is required in id_documents_verification_methods_supported");
		}
		logSuccess("Validated id_documents_verification_methods_supported", args("actual", jsonElement));
		return env;
	}

}
