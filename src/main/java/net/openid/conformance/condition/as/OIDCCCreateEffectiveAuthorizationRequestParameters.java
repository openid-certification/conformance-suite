package net.openid.conformance.condition.as;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

/**
 * Only to be used when a request object (request or request_uri) is used
 * Merges http request parameters and request object parameters
 * and creates effective_authorization_endpoint_request environment entry
 */
public class OIDCCCreateEffectiveAuthorizationRequestParameters extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "authorization_request_object"})
	@PostEnvironment(required = {"effective_authorization_endpoint_request"})
	public Environment evaluate(Environment env) {

		JsonObject authzEndpointReqParams = env.getElementFromObject("authorization_endpoint_request", "params").getAsJsonObject();
		JsonObject effective = new JsonObject();
		JsonObject paramsObject = authzEndpointReqParams.deepCopy();

		JsonObject requestObjectClaims = env.getElementFromObject("authorization_request_object", "claims").getAsJsonObject();

		for(String paramName : requestObjectClaims.keySet()) {
			paramsObject.add(paramName, requestObjectClaims.get(paramName));
		}
		//max_age special case
		if(paramsObject.has("max_age")) {
			paramsObject.addProperty("max_age", OIDFJSON.getNumber(paramsObject.get("max_age")).intValue());
		}
		effective.add("params", paramsObject);
		env.putObject("effective_authorization_endpoint_request", effective);
		logSuccess("Merged http request parameters with request object claims",
					args("effective_authorization_endpoint_request", effective));
		return env;
	}

}
