package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.util.UriComponentsBuilder;

public class CreateEndSessionResponseRedirect extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"end_session_endpoint_response_params", "end_session_endpoint_http_request_params"})
	@PostEnvironment(strings = "end_session_endpoint_response_redirect")
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject("end_session_endpoint_response_params");
		String redirectUri = env.getString("end_session_endpoint_http_request_params", "post_logout_redirect_uri");

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(redirectUri);

		for(String paramName : params.keySet()) {
			builder.queryParam(paramName, OIDFJSON.getString(params.get(paramName)));
		}

		String redirectTo = builder.toUriString();

		logSuccess("Redirecting back to client", args("uri", redirectTo));

		env.putString("end_session_endpoint_response_redirect", redirectTo);

		return env;

	}

}
