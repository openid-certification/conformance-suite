package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateFederationRegistrationEndpoint extends AbstractValidateMetadata {

	@Override
	@PreEnvironment(required = { "openid_provider_metadata" } )
	public Environment evaluate(Environment env) {

		JsonElement clientRegistrationTypesSupportedElement = env.getElementFromObject("openid_provider_metadata", "client_registration_types_supported");

		boolean containsExplicit = clientRegistrationTypesSupportedElement.getAsJsonArray().contains(new JsonPrimitive("explicit" ));
		if (containsExplicit) {
			JsonElement federationRegistrationEndpointElement = env.getElementFromObject("openid_provider_metadata", "federation_registration_endpoint");

			if (federationRegistrationEndpointElement == null) {
				throw error("federation_registration_endpoint is a required parameter when explicit client registration is supported",
					args("federation_registration_endpoint", federationRegistrationEndpointElement));
			}

			if (!federationRegistrationEndpointElement.isJsonPrimitive() || !federationRegistrationEndpointElement.getAsJsonPrimitive().isString()) {
				throw error("federation_registration_endpoint must be a URL string",
					args("federation_registration_endpoint", federationRegistrationEndpointElement));
			}

			if(!validateUrl(OIDFJSON.getString(federationRegistrationEndpointElement))) {
				throw error("This URL MUST use the https scheme and MAY contain port, path, " +
					"and query parameter components encoded in application/x-www-form-urlencoded format; " +
					"it MUST NOT contain a fragment component.", args("federation_registration_endpoint", federationRegistrationEndpointElement));
			}

			logSuccess("client_registration_types_supported contains explicit and the federation_registration_endpoint is valid",
				args("federation_registration_endpoint", federationRegistrationEndpointElement));

		} else {
			logSuccess("client_registration_types_supported does not contain type explicit",
				args("client_registration_types_supported", clientRegistrationTypesSupportedElement));

		}

		return env;
	}
}
