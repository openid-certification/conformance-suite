package net.openid.conformance.condition.as;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class AustraliaConnectIdCheckForUnexpectedParametersInPAREndpointRequest extends AbstractCondition {

	// https://www.rfc-editor.org/rfc/rfc9126.html#section-3
	private static final List<String> EXPECTED_PARAMS = ImmutableList.of(
		// signed request object
		"request",
		// private_key_jwt client authentication
		"client_assertion",
		"client_assertion_type"
	);

	@Override
	@PreEnvironment(required = {"par_endpoint_http_request"})
	public Environment evaluate(Environment env) {
		JsonElement parameters = env.getElementFromObject("par_endpoint_http_request", "body_form_params");
		JsonObject unexpectedParams = new JsonObject();

		if (parameters == null) {
			throw error("PAR endpoint request does not contain any parameters.", unexpectedParams);
		}

		parameters.getAsJsonObject().entrySet().forEach(entry -> {
			if (!EXPECTED_PARAMS.contains(entry.getKey())) {
				unexpectedParams.add(entry.getKey(), entry.getValue());
			}
		});

		if (unexpectedParams.size() == 0) {
			logSuccess("PAR endpoint request includes only expected parameters", parameters.getAsJsonObject());
		} else {
			throw error("PAR endpoint request includes unexpected parameters.", unexpectedParams);
		}

		return env;
	}

}
