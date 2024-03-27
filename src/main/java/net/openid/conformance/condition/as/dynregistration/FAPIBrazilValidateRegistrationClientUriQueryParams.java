package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.ArrayList;

public class FAPIBrazilValidateRegistrationClientUriQueryParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"registration_client_uri", "client_request"})
	public Environment evaluate(Environment env) {
		String registrationClientUri = env.getString("registration_client_uri", "fullUrl");

		MultiValueMap<String, String> parameters =
			UriComponentsBuilder.fromUriString(registrationClientUri).build().getQueryParams();

		JsonObject receivedQueryParams     = env.getElementFromObject("client_request", "query_string_params").getAsJsonObject();
		JsonObject receivedQueryParamsCopy = receivedQueryParams.deepCopy();
		ArrayList<String> missingParams    = new ArrayList<>();

		for (String inKey: parameters.keySet()) {
			if (! receivedQueryParams.has(inKey)) {
				missingParams.add(inKey);
				continue;
			}

			String value = OIDFJSON.getString(receivedQueryParams.get(inKey));

			if (value.equals(parameters.get(inKey).get(0))) {
				receivedQueryParams.remove(inKey);
			}
		}

		if (! missingParams.isEmpty()) {
			throw error("The Client Configuration Endpoint registration request is missing some expected query parameters",
				args("expected", parameters, "incoming_query_params", receivedQueryParamsCopy, "missing", missingParams));
		}

		if (! receivedQueryParams.isEmpty()) {
			throw error("The Client Configuration Endpoint registration request contains query parameters with unexpected values",
				args("expected", parameters, "incoming_query_params", receivedQueryParamsCopy, "incorrect", receivedQueryParams));
		}

		if (parameters.isEmpty()) {
			logSuccess("The Client Configuration Endpoint registration request correctly contained no query parameters");
		}
		else {
			logSuccess("The Client Configuration Endpoint registration request query parameters are correct",
				args("incoming_query_params", receivedQueryParamsCopy));
		}

		return env;
	}
}
