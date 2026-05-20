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
			if(subjectTypeElem==null) {
				throw error("No subject type in authzen_api_endpoint_request");
			}
			subjectType = OIDFJSON.getString(subjectTypeElem);
		}
		return subjectType;
	}

	@Override
	protected void ensureValidResponseResultsObject(JsonObject resultsObj) {
		// check result contains id and type and type value matches requested type
		if(!resultsObj.has("id")) {
			throw error("A subject object in the results array does not contain an id");
		}
		if(!resultsObj.has("type")) {
			throw error("A subject object int the results array does not contain a type");
		}
		if(!OIDFJSON.getString(resultsObj.get("type")).equals(getRequestSubjectType())) {
			throw error("A subject object in the results array does not contain the requested subject type", args("expected", getRequestSubjectType(), "actual", OIDFJSON.getString(resultsObj.get("type"))));
		}
	}


}
