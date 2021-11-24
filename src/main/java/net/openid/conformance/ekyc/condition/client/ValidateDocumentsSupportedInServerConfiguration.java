package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateDocumentsSupportedInServerConfiguration extends AbstractCondition {

	//documents_supported: REQUIRED when evidence_supported contains "document" or "id_document".
	// JSON array containing all identity document types utilized by the OP for identity verification.
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonArray evidenceSupported = getJsonArrayFromEnvironment(env, "server", "evidence_supported", "evidence_supported in authorization server metadata");
		JsonElement documentsSupportedElement = env.getElementFromObject("server", "documents_supported");
		if(evidenceSupported.contains(new JsonPrimitive("document")) ||
			evidenceSupported.contains(new JsonPrimitive("id_document"))){
			if(documentsSupportedElement==null) {
				throw error("documents_supported is required when evidence_supported contains document or id_document");
			}
			if(documentsSupportedElement.isJsonArray()){
				//TODO I assumed this must have at least one entry
				if(documentsSupportedElement.getAsJsonArray().size()<1){
					throw error("documents_supported is empty but must have at least one entry",
						args("documents_supported", documentsSupportedElement));
				} else {
					logSuccess("documents_supported is an array");
				}
			} else {
				throw error("documents_supported is not an array",
					args("documents_supported", documentsSupportedElement));
			}
		} else {
			//not required but if it's set it must be an array
			if(documentsSupportedElement!=null) {
				if(documentsSupportedElement.isJsonArray()){
					logSuccess("documents_supported is an array");
				} else {
					throw error("documents_supported is not an array",
						args("documents_supported", documentsSupportedElement));
				}
			}
		}
		return env;
	}
}
