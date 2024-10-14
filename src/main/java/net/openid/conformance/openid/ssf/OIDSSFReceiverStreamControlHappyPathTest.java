package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204;
import net.openid.conformance.openid.ssf.conditions.OIDSSFObtainTransmitterAccessToken;
import net.openid.conformance.openid.ssf.conditions.streams.CheckTransmitterMetadataIssuerMatchesIssuerInResponse;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamStatusCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFUpdateStreamStatusCall;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "openid-ssf-receiver-stream-control-happy-path",
	displayName = "OpenID Shared Signals Framework: Validate Receiver Stream Control (Happy Path)",
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
public class OIDSSFReceiverStreamControlHappyPathTest extends AbstractOIDSSFTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		fetchTransmitterMetadata();

		// see https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html
		// OID_CAEP_INTEROP https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html
		callAndStopOnFailure(OIDSSFObtainTransmitterAccessToken.class);

//		try {
//			callAndContinueOnFailure(OIDSSFReadStreamConfigCall.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.3.8.2");
//		} catch (Exception ignore) {
//		}
//		try {
//			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.3.8.2");
//			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
//			callAndContinueOnFailure(EnsureHttpStatusCodeIs204.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.5");
//		} catch (Exception ignore) {
//		}

		callAndStopOnFailure(OIDSSFCreateStreamConfigCall.class, "OIDSSF-7.1.1.1", "CAEPIOP-2.3.8.2");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
		callAndContinueOnFailure(CheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
		call(exec().unmapKey("endpoint_response"));

		callAndStopOnFailure(OIDSSFReadStreamConfigCall.class, "OIDSSF-7.1.1.2", "CAEPIOP-2.3.8.2");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.2");
		callAndContinueOnFailure(CheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.2");
		// TODO check: In the event that there are no Event Streams configured, the Transmitter MUST return an empty list.
		// TODO check: stream configuration response
		call(exec().unmapKey("endpoint_response"));

//		callAndStopOnFailure(OIDSSFUpdateStreamConfigCall.class, "OIDSSF-7.1.1.3", "CAEPIOP-2.3.8.2");
//		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
//		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.3");
//		// TODO check for 202 response
//		// 202	if the update request has been accepted, but not processed. Receiver MAY try the same request later to get processing result.
//		callAndContinueOnFailure(CheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.3");
//		call(exec().unmapKey("endpoint_response"));

//		callAndStopOnFailure(OIDSSFReplaceStreamConfigCall.class, "OIDSSF-7.1.1.4", "CAEPIOP-2.3.8.2");
//		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
//		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.4");
//		// TODO check for 202 responses
//		// 202	if the replace request has been accepted, but not processed. Receiver MAY try the same request later in order to get processing result.
//		callAndContinueOnFailure(CheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.4");
//		call(exec().unmapKey("endpoint_response"));

		// stream status
		callAndStopOnFailure(OIDSSFReadStreamStatusCall.class, "OIDSSF-7.1.2.1");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.2.1");
		// TODO check: status response
		call(exec().unmapKey("endpoint_response"));

		callAndStopOnFailure(OIDSSFUpdateStreamStatusCall.class, "OIDSSF-7.1.2.2");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.2.2");
		// TODO check: status response
		call(exec().unmapKey("endpoint_response"));

		callAndStopOnFailure(OIDSSFDeleteStreamConfigCall.class, "OIDSSF-7.1.1.5", "CAEPIOP-2.3.8.2");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-7.1.1.5");
		call(exec().unmapKey("endpoint_response"));

		//OID_CAEP_INTEROP-2.3.8.2 Stream control (except Status) - check for API availability
		// - Creating a Stream
		// - Reading Stream Configuration
		// - Getting the Stream Status
		// - Stream Verification

		fireTestFinished();
	}

}
