package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.OAuthUriUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class SendAuthorizationResponseWithResponseModeQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	@PostEnvironment(strings = "authorization_endpoint_response_redirect")
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);
		String redirectUri = OIDFJSON.getString(params.remove("redirect_uri"));

		Map<String, String> queryParams = new LinkedHashMap<>();
		for(String paramName : params.keySet()) {
			queryParams.put(paramName, OIDFJSON.getString(params.get(paramName)));
		}
		String redirectTo = OAuthUriUtil.appendFormUrlEncodedQuery(redirectUri, queryParams);

		log("Redirecting back to client", args("uri", redirectTo));

		env.putString("authorization_endpoint_response_redirect", redirectTo);

		return env;

	}

}
