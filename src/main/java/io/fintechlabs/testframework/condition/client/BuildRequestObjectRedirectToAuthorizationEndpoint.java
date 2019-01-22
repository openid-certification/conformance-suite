package io.fintechlabs.testframework.condition.client;

import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonElement;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class BuildRequestObjectRedirectToAuthorizationEndpoint extends AbstractCondition {

	private static final List<String> REQUIRED_PARAMETERS = Arrays.asList(new String[] {
		"response_type",
		"client_id",
		"scope",
		"redirect_uri"
	});

	/**
	 * @param testId
	 * @param log
	 */
	public BuildRequestObjectRedirectToAuthorizationEndpoint(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "request_object_claims", "server" }, strings = "request_object")
	@PostEnvironment(strings = "redirect_to_authorization_endpoint")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		if (authorizationEndpointRequest == null) {
			throw error("Couldn't find authorization endpoint request");
		}

		String requestObject = env.getString("request_object");
		if (requestObject == null) {
			throw error("Couldn't find request object");
		}

		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		if (requestObjectClaims == null) {
			throw error("Couldn't find request object claims");
		}

		String authorizationEndpoint = env.getString("server", "authorization_endpoint");
		if (Strings.isNullOrEmpty(authorizationEndpoint)) {
			throw error("Couldn't find authorization endpoint");
		}

		// send a front channel request to start things off
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(authorizationEndpoint);

		builder.queryParam("request", requestObject);

		for (String key : authorizationEndpointRequest.keySet()) {

			JsonElement requestObjectElement = requestObjectClaims.get(key);
			JsonElement requestParameterElement = authorizationEndpointRequest.get(key);
			if (requestObjectElement != null && !(requestObjectElement instanceof JsonPrimitive)
				|| !(requestParameterElement instanceof JsonPrimitive)) {
				// only handle stringable values for now (as BuildPlainRedirectToAuthorizationEndpoint)
				continue;
			}

			String requestObjectValue = null;
			if (requestObjectElement != null) {
				requestObjectValue = requestObjectElement.getAsString();
			}
			String requestParameterValue = requestParameterElement.getAsString();

			if (key.equals("state")) {
				Boolean exposeState = env.getBoolean("expose_state_in_authorization_endpoint_request");
				if (exposeState != null && exposeState.equals(true) ) {
					builder.queryParam("state", env.getString("state"));
				}
			}

			if (REQUIRED_PARAMETERS.contains(key)
				|| requestObjectValue == null
				|| !requestParameterValue.equals(requestObjectValue)) {
				builder.queryParam(key, requestParameterValue);
			}
		}

		String redirectTo = builder.toUriString();

		logSuccess("Sending to authorization endpoint", args("redirect_to_authorization_endpoint", redirectTo));

		env.putString("redirect_to_authorization_endpoint", redirectTo);

		return env;
	}

}
