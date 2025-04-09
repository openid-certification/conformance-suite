package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CreateQueryParametersForAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims", strings = "request_object")
	@PostEnvironment(required = "query_parameters")
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		String requestObject = env.getString("request_object");

		JsonObject queryParameters = new JsonObject();
		queryParameters.addProperty("client_id", OIDFJSON.getString(requestObjectClaims.get("client_id")));
		queryParameters.addProperty("scope", OIDFJSON.getString(requestObjectClaims.get("scope")));
		queryParameters.addProperty("response_type", OIDFJSON.getString(requestObjectClaims.get("response_type")));
		queryParameters.addProperty("request", requestObject);
		env.putObject("query_parameters", queryParameters);

		logSuccess("Created query parameters", args("query_parameters", queryParameters));

		return env;
	}

}
