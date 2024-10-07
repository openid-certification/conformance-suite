package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.SsfCheckTransmitterMetadata;
import net.openid.conformance.condition.client.SsfGetDynamicTransmitterConfiguration;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.SsfDeliveryMode;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "openid-ssf-transmitter-metadata",
	displayName = "OpenID Shared Signals Framework: Validate Transmitter Metadata",
	summary = "This test verifies the behavior of the transmitter metadata.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix",
	}
)
@VariantParameters({
	SsfDeliveryMode.class,
})
public class OpenIdSsfTransmitterMetadataTest extends AbstractOpenIdSsfTest {

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		// fetch transmitter metadata
		callAndStopOnFailure(SsfGetDynamicTransmitterConfiguration.class, "OIDSSF-6.2");

		// validate transmitter metadata
		callAndStopOnFailure(SsfCheckTransmitterMetadata.class,"OIDSSF-6.1", "OIDCAEPIOP-2.3.7");

		// populate server jwks
		env.putObject("server", env.getObject("transmitter_metadata"));

		callAndStopOnFailure(FetchServerKeys.class);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");

		fireTestFinished();
	}
}
