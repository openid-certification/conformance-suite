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
		return env.getElementFromObject("transmitter_metadata", "issuer");
	}

	@Override
	protected String getConfigurationUrl(Environment env) {
		return env.getString("config", "ssf.transmitter.issuer");
	}

	@Override
	public Environment evaluate(Environment env) {
		env.mapKey("server", "transmitter_metadata");
		return super.evaluate(env);
	}

	@Override
	protected String getEndpointLabel() {
		return "transmitter metadata";
	}
}
