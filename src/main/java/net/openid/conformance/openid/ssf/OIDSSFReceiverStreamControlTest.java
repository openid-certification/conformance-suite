package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFCreateStreamConfiguration;
import net.openid.conformance.openid.ssf.conditions.OIDSSFDeleteStreamConfiguration;
import net.openid.conformance.openid.ssf.conditions.OIDSSFObtainTransmitterAccessToken;
import net.openid.conformance.openid.ssf.conditions.OIDSSFQueryStreamConfiguration;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "openid-ssf-receiver-stream-control",
	displayName = "OpenID Shared Signals Framework: Validate Receiver Stream Control",
	summary = "This test verifies the behavior of the receiver stream control.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-6.2.1
		"ssf.transmitter.access_token"
	}
)
@VariantParameters({
	ServerMetadata.class,
	SsfDeliveryMode.class,
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
	"ssf.transmitter.configuration_metadata_endpoint",
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "discovery", configurationFields = {
	"ssf.transmitter.issuer",
	"ssf.transmitter.metadata_suffix",
})
public class OIDSSFReceiverStreamControlTest extends AbstractOIDSSFTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		fetchTransmitterMetadata();

		// see https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html
		// OID_CAEP_INTEROP https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html
		callAndStopOnFailure(OIDSSFObtainTransmitterAccessToken.class);

//		try {
//			callAndContinueOnFailure(OIDSSFQueryStreamConfiguration.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.3.8.2");
//		} catch (Exception ignore) {
//		}
//		try {
//			callAndContinueOnFailure(OIDSSFDeleteStreamConfiguration.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.3.8.2");
//		} catch (Exception ignore) {
//		}
		callAndStopOnFailure(OIDSSFCreateStreamConfiguration.class, "CAEPIOP-2.3.8.2");
		callAndStopOnFailure(OIDSSFQueryStreamConfiguration.class, "CAEPIOP-2.3.8.2");
		callAndStopOnFailure(OIDSSFDeleteStreamConfiguration.class, "CAEPIOP-2.3.8.2");

		//OID_CAEP_INTEROP-2.3.8.2 Stream control (except Status) - check for API availability
		// - Creating a Stream
		// - Reading Stream Configuration
		// - Getting the Stream Status
		// - Stream Verification

		fireTestFinished();
	}

}
