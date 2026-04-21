package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthzenApiEndpointRequestSearchPage extends CreateAuthzenApiEndpointRequestParameter {

	public CreateAuthzenApiEndpointRequestSearchPage(JsonObject requestParameter) {
		super("page", requestParameter);
		this.optionalProperties = new String[] {"token", "limit", "properties"};
	}

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject page = createAuthzenApiEndpointRequestParameter(env).getAsJsonObject();
		JsonObject request = env.getObject("authzen_api_endpoint_request");

		// Set token in page if exists,
		// token in the initial paginated request should not be set since no token exist yet
		if(!Strings.isNullOrEmpty(env.getString("authzen_search_endpoint_request_page_token"))) {
			page.addProperty("token", env.getString("authzen_search_endpoint_request_page_token"));
		} else {
			page.remove("token");
		}
		request.add("page", page);
		logSuccess("Created Search API page parameter", args(requestParameterName, page));
		return env;
	}

}
