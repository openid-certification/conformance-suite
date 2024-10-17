package net.openid.conformance.openid.federation;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateRequestAuthenticationMethodsSupported extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "openid_provider_metadata" } )
	public Environment evaluate(Environment env) {

		JsonElement requestAuthenticationMethodsSupportedElement = env.getElementFromObject("openid_provider_metadata", "request_authentication_methods_supported");

		if (requestAuthenticationMethodsSupportedElement == null) {
			logSuccess("request_authentication_methods_supported is optional");
			return env;
		}

		if (!requestAuthenticationMethodsSupportedElement.isJsonObject()) {
			throw error("request_authentication_methods_supported must be a JSON object",
				args("request_authentication_methods_supported", requestAuthenticationMethodsSupportedElement));
		}

		JsonObject requestAuthenticationMethodsSupported = requestAuthenticationMethodsSupportedElement.getAsJsonObject();
		for (String key : requestAuthenticationMethodsSupported.keySet()) {

			// In federation, we care only about these two
			if (ImmutableSet.of("authorization_endpoint", "pushed_authorization_request_endpoint").contains(key)) {

				JsonElement value = requestAuthenticationMethodsSupported.get(key);
				if (value == null || !value.isJsonArray()) {
					throw error("The value for %s must be an array".formatted(key), args("request_authentication_method", value));
				}

				try {
					OIDFJSON.convertJsonArrayToList(value.getAsJsonArray());
				} catch (OIDFJSON.UnexpectedJsonTypeException e) {
					throw error("The values of the JSON object members for the endpoint names are JSON arrays containing the names of " +
						"the request authentication methods used at those endpoints.", args("request_authentication_method", value));
				}
			}

		}

		logSuccess("request_authentication_methods_supported is well-formed",
			args("request_authentication_methods_supported", requestAuthenticationMethodsSupportedElement));
		return env;
	}
}
