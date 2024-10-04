package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantParameters;

@VariantParameters({
	ServerMetadata.class, // e.g. Transmitter/Receiver Configuration metadata URL
	// TODO how to define Authorization (AT / API Key?)
})

public abstract class AbstractOpenIdSsfTest extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {

	}

	@Override
	public void start() {

	}
}
