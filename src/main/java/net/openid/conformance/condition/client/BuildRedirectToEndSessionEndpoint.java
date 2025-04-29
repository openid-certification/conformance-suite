package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.util.UriComponentsBuilder;

public class BuildRedirectToEndSessionEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "end_session_endpoint_request", "server" })
	@PostEnvironment(strings = "redirect_to_end_session_endpoint")
	public Environment evaluate(Environment env) {
		JsonObject endSessionEndpointRequest = env.getObject("end_session_endpoint_request");

		String endSessionEndpoint = env.getString("server", "end_session_endpoint");
		if (Strings.isNullOrEmpty(endSessionEndpoint)) {
			throw error("Couldn't find end_session endpoint");
		}

		// send a front channel request to start things off
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endSessionEndpoint);

		for (String key : endSessionEndpointRequest.keySet()) {
			JsonElement element = endSessionEndpointRequest.get(key);
			builder.queryParam(key, OIDFJSON.getString(element));
		}

		String redirectTo = builder.toUriString();

		logSuccess("Sending to end_session endpoint", args("redirect_to_end_session_endpoint", redirectTo));

		env.putString("redirect_to_end_session_endpoint", redirectTo);

		return env;
	}

}
