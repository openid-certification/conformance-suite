package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWSUtil;

public class ValidateRequestAuthenticationSigningAlgValuesSupported extends AbstractCondition {


	@Override
	@PreEnvironment(required = { "openid_provider_metadata" } )
	public Environment evaluate(Environment env) {

		JsonElement requestAuthenticationMethodsSupportedElement =
			env.getElementFromObject("openid_provider_metadata", "request_authentication_methods_supported");
		JsonObject requestAuthenticationMethodsSupported = requestAuthenticationMethodsSupportedElement.getAsJsonObject();

		boolean required = false;
		for (String key : requestAuthenticationMethodsSupported.keySet()) {

			// If request_authentication_methods_supported contains request_object for authorization_endpoint,
			// then request_authentication_signing_alg_values_supported is required.
			if ("authorization_endpoint".equals(key)) {
				JsonArray authorizationEndpointEntry = requestAuthenticationMethodsSupported.get(key).getAsJsonArray();
				if (authorizationEndpointEntry.contains(new JsonPrimitive("request_object"))) {
					required = true;
					break;
				}
			}

			// If request_authentication_methods_supported contains private_key_jwt for pushed_authorization_request_endpoint,
			// then request_authentication_signing_alg_values_supported is required.
			if ("pushed_authorization_request_endpoint".equals(key)) {
				JsonArray pushedAuthorizationRequestEndpointEntry = requestAuthenticationMethodsSupported.get(key).getAsJsonArray();
				if (pushedAuthorizationRequestEndpointEntry.contains(new JsonPrimitive("private_key_jwt"))) {
					required = true;
					break;
				}
			}

		}

		JsonElement requestAuthenticationSigningAlgValuesSupportedElement =
			env.getElementFromObject("openid_provider_metadata", "request_authentication_signing_alg_values_supported");
		if (required && requestAuthenticationSigningAlgValuesSupportedElement == null) {
			throw error("request_authentication_signing_alg_values_supported must be present if request_authentication_methods_supported " +
				"contains private_key_jwt or request_object for any entry.",
				args("request_authentication_methods_supported", requestAuthenticationMethodsSupportedElement));
		}

		if (!requestAuthenticationSigningAlgValuesSupportedElement.isJsonArray()) {
			throw error("request_authentication_signing_alg_values_supported must be a JSON array",
				args("request_authentication_signing_alg_values_supported", requestAuthenticationSigningAlgValuesSupportedElement));
		}

		JsonArray requestAuthenticationSigningAlgValuesSupported = requestAuthenticationSigningAlgValuesSupportedElement.getAsJsonArray();
		for (JsonElement requestAuthenticationSigningAlgValue : requestAuthenticationSigningAlgValuesSupported) {
			if (!JWSUtil.isValidJWSAlgorithm(OIDFJSON.getString(requestAuthenticationSigningAlgValue))) {
				throw error("request_authentication_signing_alg_values_supported must be a JSON array of the supported JWS [RFC7515] algorithms (alg values) for signing.",
					args("request_authentication_signing_alg_values_supported", requestAuthenticationSigningAlgValuesSupportedElement,
						"valid_jws_algs", JWSUtil.validJWSAlgorithms()));
			}
		}

		logSuccess("request_authentication_signing_alg_values_supported is present and contains valid JWS algorithms for signing",
			args("request_authentication_signing_alg_values_supported", requestAuthenticationSigningAlgValuesSupportedElement));
		return env;
	}
}
