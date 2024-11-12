package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.CheckDiscEndpointIssuer;
import net.openid.conformance.testmodule.Environment;
import org.jetbrains.annotations.NotNull;

public class OIDSSFCheckTransmitterMetadataIssuer extends CheckDiscEndpointIssuer {

	@NotNull
	@Override
	protected String getConfigurationEndpoint() {
		return OIDSSFGetDynamicTransmitterConfiguration.WELL_KNOWN_SSF_CONFIGURATION_PATH;
	}

	@Override
	protected JsonElement getResponseIssuerElement(Environment env) {
		return env.getElementFromObject("ssf", "transmitter_metadata.issuer");
	}

	@Override
	protected String getConfigurationUrl(Environment env) {
		return env.getString("config", "ssf.transmitter.issuer");
	}

	@Override
	public Environment evaluate(Environment env) {
		// Workaround because we cannot use env.mapKey("server","ssf.transmitter_metadata")
		env.putObject("transmitter_metadata", env.getElementFromObject("ssf", "transmitter_metadata").getAsJsonObject());
		try {
			env.mapKey("server", "transmitter_metadata");
			return super.evaluate(env);
		} finally {
			env.removeObject("transmitter_metadata");
		}
	}

	@Override
	protected String getEndpointLabel() {
		return "transmitter metadata";
	}
}
