package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

/**
 * For a pushed authorization request that carries a request object: the only form parameters
 * permitted alongside 'request' are those of the client authentication method, everything else
 * must be a claim of the request object (RFC 9126 section 3).
 *
 * This class covers client authentication methods that put no credential in the form body
 * (e.g. mutual TLS), where only 'client_id' may accompany 'request'.
 */
public class CheckForUnexpectedParametersInSignedPAREndpointRequest extends AbstractCondition {

	protected List<String> getExpectedParameters() {
		return List.of("request", "client_id");
	}

	@Override
	@PreEnvironment(required = {"par_endpoint_http_request"})
	public Environment evaluate(Environment env) {
		JsonElement parameters = env.getElementFromObject("par_endpoint_http_request", "body_form_params");
		JsonObject unexpectedParams = new JsonObject();

		if (parameters == null) {
			throw error("PAR endpoint request does not contain any parameters.", unexpectedParams);
		}

		List<String> expectedParams = getExpectedParameters();
		parameters.getAsJsonObject().entrySet().forEach(entry -> {
			if (!expectedParams.contains(entry.getKey())) {
				unexpectedParams.add(entry.getKey(), entry.getValue());
			}
		});

		if (unexpectedParams.size() == 0) {
			logSuccess("PAR endpoint request includes only the request object and client authentication parameters", parameters.getAsJsonObject());
		} else {
			throw error("PAR endpoint request includes parameters outside the request object that are not part of client authentication. "
				+ "When a request object is used, all authorization request parameters must be sent as claims of the request object.",
				args("unexpected", unexpectedParams, "expected", expectedParams));
		}

		return env;
	}

}
