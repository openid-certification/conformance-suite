package net.openid.conformance.vciid2issuer.condition;

import net.openid.conformance.condition.util.TLSTestValueExtractor;
import net.openid.conformance.testmodule.Environment;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;

public class VCIID2GetStaticCredentialIssuerMetadata extends VCIID2GetDynamicCredentialIssuerMetadata {

	@NotNull
	@Override
	protected String extractMetadataEndpointUrl(Environment env) {

		String configMetadataEndpoint = env.getString("config", "vci.credential_issuer_metadata_url");
		if (configMetadataEndpoint == null) {
			throw error("Missing required vci.credential_issuer_metadata_url");
		}

		try {
			env.putObject("tls", TLSTestValueExtractor.extractTlsFromUrl(configMetadataEndpoint));
		} catch (MalformedURLException e) {
			throw error("Failed to parse URL", e, args("url", configMetadataEndpoint));
		}

		return configMetadataEndpoint;
	}
}
