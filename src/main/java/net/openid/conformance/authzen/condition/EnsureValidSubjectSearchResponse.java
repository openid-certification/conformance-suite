package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureValidSubjectSearchResponse extends EnsureValidSearchResponse {

	private Environment env;
	private String subjectType;

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request", "authzen_search_endpoint_response"})
	public Environment evaluate(Environment env) {
		this.env = env;
		return super.evaluate(env);
	}

	private String getRequestSubjectType() {
		if(subjectType == null) {
			JsonElement subjectTypeElem = env.getElementFromObject("authzen_api_endpoint_request", "subject.type");
			if (!OIDFJSON.isString(subjectTypeElem)) {
				throw error("The search request the suite sent has no string subject type, so the type of each result cannot be checked",
					args("subject", env.getElementFromObject("authzen_api_endpoint_request", "subject")));
			}
			subjectType = OIDFJSON.getString(subjectTypeElem);
		}
		return subjectType;
	}

	@Override
	protected void ensureValidResponseResultsObject(JsonObject resultsObj) {
		getRequiredStringMember(resultsObj, "id", "A subject object");
		String type = getRequiredStringMember(resultsObj, "type", "A subject object");
		if (!type.equals(getRequestSubjectType())) {
			throw error("A subject object in the results array does not contain the requested subject type", args("expected", getRequestSubjectType(), "actual", type));
		}
		ensurePropertiesIsObject(resultsObj, "A subject object");
	}
}
