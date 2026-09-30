package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFCaepInteropAuthorizationSchemesTransmitterMetadataCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonObject transmitterMetadata = env.getElementFromObject("ssf","transmitter_metadata").getAsJsonObject();

		// Optional in OIDSSF-7.1.1 but required by CAEPIOP-2.3.7
		if (!transmitterMetadata.has("authorization_schemes")) {
			throw error("Transmitter metadata is missing the authorization_schemes field, which the CAEP Interop Profile requires");
		}

		JsonElement authorizationSchemesEl = transmitterMetadata.get("authorization_schemes");
		if (!authorizationSchemesEl.isJsonArray()) {
			throw error("authorization_schemes must be a JSON array of objects", args("authorization_schemes", authorizationSchemesEl));
		}
		JsonArray authorizationSchemes = authorizationSchemesEl.getAsJsonArray();

		// every element is checked, not only the ones before the first rfc6749 entry
		boolean rfc6749Found = false;
		for (var element : authorizationSchemes) {
			if (!element.isJsonObject()) {
				throw error("Each authorization_schemes element must be a JSON object", args("element", element));
			}
			JsonElement specUrnEl = element.getAsJsonObject().get("spec_urn");
			if (specUrnEl == null) {
				throw error("Missing required field spec_urn for authorization_schemes element", args("element", element));
			}
			if (!specUrnEl.isJsonPrimitive() || !specUrnEl.getAsJsonPrimitive().isString()) {
				throw error("spec_urn of an authorization_schemes element must be a JSON string", args("element", element));
			}
			String specUrn = OIDFJSON.getString(specUrnEl);

			if (!specUrn.startsWith("urn:")) {
				throw error("Found invalid spec_urn for authorization_schemes element, the value must start with 'urn:'", args("spec_urn", specUrn));
			}

			if (specUrn.equals("urn:ietf:rfc:6749")) {
				rfc6749Found = true;
			}
		}

		if (!rfc6749Found) {
			throw error("Missing required authorization_scheme with spec_urn 'urn:ietf:rfc:6749'", args("authorization_schemes", authorizationSchemes));
		}

		logSuccess("Found required authorization_schemes", args("authorization_schemes", authorizationSchemes));

		return env;
	}
}
