package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.testmodule.Environment;
import org.jetbrains.annotations.NotNull;

public class OIDSSFGetStaticTransmitterConfiguration extends OIDSSFGetDynamicTransmitterConfiguration{

	@NotNull
	@Override
	String extractMetadataEndpointUrl(Environment env) {

		String configMetadataEndpoint = env.getString("config", "ssf.transmitter.configuration_metadata_endpoint");
		if (configMetadataEndpoint == null) {
			throw error("Missing required ssf.transmitter.configuration_metadata_endpoint");
		}

		return configMetadataEndpoint;
	}
}
