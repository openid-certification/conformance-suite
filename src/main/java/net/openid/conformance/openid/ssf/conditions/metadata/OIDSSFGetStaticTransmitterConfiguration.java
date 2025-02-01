package net.openid.conformance.openid.ssf.conditions.metadata;

import net.openid.conformance.condition.util.TLSTestValueExtractor;
import net.openid.conformance.testmodule.Environment;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;

public class OIDSSFGetStaticTransmitterConfiguration extends OIDSSFGetDynamicTransmitterConfiguration {

	@NotNull
	@Override
	String extractMetadataEndpointUrl(Environment env) {

		String configMetadataEndpoint = env.getString("config", "ssf.transmitter.configuration_metadata_endpoint");
		if (configMetadataEndpoint == null) {
			throw error("Missing required ssf.transmitter.configuration_metadata_endpoint");
		}

		try {
			env.putObject("tls", TLSTestValueExtractor.extractTlsFromUrl(configMetadataEndpoint));
		} catch (MalformedURLException e) {
			throw error("Failed to parse URL", e, args("url", configMetadataEndpoint));
		}

		return configMetadataEndpoint;
	}
}
