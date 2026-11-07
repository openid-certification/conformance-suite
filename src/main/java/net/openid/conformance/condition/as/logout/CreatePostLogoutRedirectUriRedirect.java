package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.OAuthUriUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class CreatePostLogoutRedirectUriRedirect extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"post_logout_redirect_uri_params", "end_session_endpoint_http_request_params"})
	@PostEnvironment(strings = "post_logout_redirect_uri_redirect")
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject("post_logout_redirect_uri_params");
		String redirectUri = env.getString("end_session_endpoint_http_request_params", "post_logout_redirect_uri");

		Map<String, String> queryParams = new LinkedHashMap<>();
		for(String paramName : params.keySet()) {
			queryParams.put(paramName, OIDFJSON.getString(params.get(paramName)));
		}
		String redirectTo = OAuthUriUtil.appendFormUrlEncodedQuery(redirectUri, queryParams);

		logSuccess("Created post_logout_redirect_uri redirect", args("uri", redirectTo));

		env.putString("post_logout_redirect_uri_redirect", redirectTo);

		return env;

	}

}
