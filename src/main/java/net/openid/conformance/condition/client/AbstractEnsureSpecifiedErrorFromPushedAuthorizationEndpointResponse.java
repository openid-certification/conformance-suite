package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractEnsureSpecifiedErrorFromPushedAuthorizationEndpointResponse extends AbstractCondition {

	private static final int HTTP_STATUS_BAD_REQUEST = 400;

	protected abstract String[] getExpectedError();

	protected int getExpectedResponseCode() {
		return HTTP_STATUS_BAD_REQUEST;
	}

	@Override
	@PreEnvironment(required = {CallPAREndpoint.RESPONSE_KEY})
	public Environment evaluate(Environment env) {

		Integer status = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status");
		if (status != getExpectedResponseCode()) {
			throw error("Invalid pushed authorization request endpoint response http status code",
				args("expected", getExpectedResponseCode(), "actual", status));
		}

		String error = env.getString(CallPAREndpoint.RESPONSE_KEY, "body_json.error");
		List<String> expected = Arrays.asList(getExpectedError());

		if (Strings.isNullOrEmpty(error)) {
			throw error("Expected 'error' field not found");
		} else if (!expected.contains(error)) {
			throw error("'error' field has unexpected value", args("expected", expected, "actual", error));
		} else {
			logSuccess("Pushed Authorization Request Endpoint returned expected 'error' of '"+expected+"'",
				args("error", error, "expected", expected));

			return env;
		}
	}
}
