package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFAuthorizationSchemesTransmitterMetadataCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonObject transmitterMetadata = env.getElementFromObject("ssf","transmitter_metadata").getAsJsonObject();

		if (!transmitterMetadata.has("authorization_schemes")) {
			throw error("Missing required field authorization_schemes!");
		}

		// OIDSSF-6.1.1
		JsonArray authorizationSchemes = transmitterMetadata.getAsJsonArray("authorization_schemes");

		// CAEPIOP-2.3.7
		boolean rfc6749Found = false;
		for (var element : authorizationSchemes) {
			String specUrn = OIDFJSON.getString(element.getAsJsonObject().get("spec_urn"));
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
