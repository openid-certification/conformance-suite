package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		String metadataIssuer = OIDFJSON.getString(env.getElementFromObject("ssf", "transmitter_metadata.issuer"));

		String responseIssuer = env.getString("endpoint_response", "body_json.iss");

		if (responseIssuer == null || !removeSlashEndpointURL(metadataIssuer).equals(removeSlashEndpointURL(responseIssuer))) {
			throw error("Issuer in response does not match issuer from transmitter metadata", args("metadata_issuer", metadataIssuer, "response_issuer", responseIssuer));
		}

		logSuccess("Issuer in response matches issuer from transmitter metadata", args("metadata_issuer", metadataIssuer, "response_issuer", responseIssuer));

		return env;
	}

	private String removeSlashEndpointURL(String url) {
		if (url.endsWith("/")) {

			return url.substring(0, url.length() - 1);
		}

		return url;
	}
}
