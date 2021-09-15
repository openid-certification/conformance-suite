package net.openid.conformance.condition.client.ekyc;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateIdDocumentsSupportedInServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement jsonElement = env.getElementFromObject("server", "id_documents_supported");
		if(jsonElement == null) {
			throw error("id_documents_supported is not set");
		}
		if(!jsonElement.isJsonArray()) {
			throw error("id_documents_supported must be a json array", args("actual", jsonElement));
		}
		//TODO require at least one entry? or is an empty value is also allowed?

		logSuccess("id_documents_supported is valid", args("actual", jsonElement));
		return env;
	}
}
