package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureValidResourceSearchResponse extends EnsureValidSearchResponse {

	private Environment env;
	private String resourceType;

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request", "authzen_search_endpoint_response"})
	public Environment evaluate(Environment env) {
		this.env = env;
		return super.evaluate(env);
	}

	private String getRequestResourceType() {
		if(resourceType == null) {
			JsonElement subjectTypeElem = env.getElementFromObject("authzen_api_endpoint_request", "resource.type");
			if(subjectTypeElem==null) {
				throw error("No resource type in authzen_api_endpoint_request");
			}
			resourceType = OIDFJSON.getString(subjectTypeElem);
		}
		return resourceType;
	}

	@Override
	protected void ensureValidResponseResultsObject(JsonObject resultsObj) {
		getRequiredStringMember(resultsObj, "id", "A resource object");
		String type = getRequiredStringMember(resultsObj, "type", "A resource object");
		if (!type.equals(getRequestResourceType())) {
			throw error("A resource object in the results array does not contain the requested resource type", args("expected", getRequestResourceType(), "actual", type));
		}
		ensurePropertiesIsObject(resultsObj, "A resource object");
	}
}
