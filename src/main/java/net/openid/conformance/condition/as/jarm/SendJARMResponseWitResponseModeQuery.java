package net.openid.conformance.condition.as.jarm;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.util.UriComponentsBuilder;

public class SendJARMResponseWitResponseModeQuery extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "jarm_response")
	@PostEnvironment(strings = "authorization_endpoint_response_redirect")
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);
		String redirectUri = OIDFJSON.getString(params.remove("redirect_uri"));

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri);
		String jarmResponse = env.getString("jarm_response");
		builder.queryParam("response", jarmResponse);

		String redirectTo = builder.toUriString();

		log("Redirecting back to client", args("uri", redirectTo));

		env.putString("authorization_endpoint_response_redirect", redirectTo);

		return env;

	}

}
