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
			throw error("Missing required field authorization_schemes! This is required by the CAEP Interop spec.");
		}

		JsonArray authorizationSchemes = transmitterMetadata.getAsJsonArray("authorization_schemes");

		boolean rfc6749Found = false;
		for (var element : authorizationSchemes) {
			JsonElement specUrnEl = element.getAsJsonObject().get("spec_urn");
			if (specUrnEl == null) {
				throw error("Missing required field spec_urn for authorization_schemes element!");
			}
			String specUrn = OIDFJSON.getString(specUrnEl);

			if (!specUrn.startsWith("urn:")) {
				throw error("Found invalid spec_urn for authorization_schemes element! spec_url value must start with 'urn:'", args("spec_urn", specUrn));
			}

			if (specUrn.equals("urn:ietf:rfc:6749")) {
				rfc6749Found = true;
				break;
			}
		}

		if (!rfc6749Found) {
			throw error("Missing required authorization_scheme with spec_urn 'urn:ietf:rfc:6749'", args("authorization_schemes", authorizationSchemes));
		}

		logSuccess("Found required authorization_schemes", args("authorization_schemes", authorizationSchemes));

		return env;
	}
}
