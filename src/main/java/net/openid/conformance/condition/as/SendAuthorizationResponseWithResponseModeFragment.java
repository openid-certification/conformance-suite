package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.util.UriComponentsBuilder;

public class SendAuthorizationResponseWithResponseModeFragment extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	@PostEnvironment(strings = "authorization_endpoint_response_redirect")
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);
		String redirectUri = OIDFJSON.getString(params.remove("redirect_uri"));

		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();

		for(String paramName : params.keySet()) {
			builder.queryParam(paramName, OIDFJSON.getString(params.get(paramName)));
		}

		String paramsAsString = builder.toUriString();
		if(paramsAsString.startsWith("?")) {
			paramsAsString = paramsAsString.substring(1);
		}

		String redirectTo = redirectUri + "#" + paramsAsString;

		log("Redirecting back to client", args("uri", redirectTo));

		env.putString("authorization_endpoint_response_redirect", redirectTo);

		return env;

	}

}
