package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateAttachmentsSupportedInServerConfiguration extends AbstractCondition {

	//attachments_supported: REQUIRED when OP supports external attachments.
	// JSON array containing all attachment types supported by the OP.
	// Possible values are external and embedded. If the list is empty, the OP does not support attachments.
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement jsonElement = env.getElementFromObject("server", "attachments_supported");
		if(jsonElement == null) {
			log("attachments_supported is not set, the OP does not support attachments");
			return env;
		}
		if(!jsonElement.isJsonArray()) {
			throw error("attachments_supported must be a json array", args("actual", jsonElement));
		}
		JsonElement external = new JsonPrimitive("external");
		JsonElement embedded = new JsonPrimitive("embedded");
		for(JsonElement arrayEntry : jsonElement.getAsJsonArray()) {
			if(!(external.equals(arrayEntry) || embedded.equals(arrayEntry))){
				throw error("Unexpected entry in attachments_supported. Allowed values are external and embedded",
					args("offending_entry", arrayEntry));
			}
		}
		logSuccess("attachments_supported is valid", args("actual", jsonElement));
		return env;
	}
}
