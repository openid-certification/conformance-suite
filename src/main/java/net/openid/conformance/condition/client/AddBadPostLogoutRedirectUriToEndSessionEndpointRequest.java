package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddBadPostLogoutRedirectUriToEndSessionEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "end_session_endpoint_request", strings = "base_url")
	@PostEnvironment(required = "end_session_endpoint_request")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		// Note that this url shouldn't be called, but for consistency allow it to be overridden just in case the OP calls it
		String externalUrlOverride = env.getString("external_url_override");
		if (!Strings.isNullOrEmpty(externalUrlOverride)) {
			baseUrl = externalUrlOverride;
		}

		// calculate the redirect URI based on our given base URL
		String postLogoutUri = baseUrl + "/bad_post_logout_redirect_uri";

		JsonObject endSessionEndpointRequest = env.getObject("end_session_endpoint_request");

		endSessionEndpointRequest.addProperty("post_logout_redirect_uri", postLogoutUri);

		env.putObject("end_session_endpoint_request", endSessionEndpointRequest);

		logSuccess("Added bad post_logout_redirect_uri to end session endpoint request", endSessionEndpointRequest);

		return env;

	}

}
