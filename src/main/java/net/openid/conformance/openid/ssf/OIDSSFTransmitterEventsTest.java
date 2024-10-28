package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204;
import net.openid.conformance.openid.ssf.conditions.OIDSSFObtainTransmitterAccessToken;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFTriggerVerificationEvent;
import net.openid.conformance.openid.ssf.conditions.streams.CheckTransmitterMetadataIssuerMatchesIssuerInResponse;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(testName = "openid-ssf-transmitter-events", displayName = "OpenID Shared Signals Framework: Validate Transmitter Events", summary = "This test verifies the structure and handling of transmitter events.", profile = "OIDSSF", configurationFields = {

})
@VariantParameters({ServerMetadata.class, SsfAuthMode.class, SsfDeliveryMode.class,})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {"ssf.transmitter.configuration_metadata_endpoint",})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "discovery", configurationFields = {"ssf.transmitter.issuer", "ssf.transmitter.metadata_suffix",})
@VariantConfigurationFields(parameter = SsfDeliveryMode.class, value = "push", configurationFields = {"ssf.transmitter.push_endpoint",})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "static", configurationFields = {
	"ssf.transmitter.access_token"
})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "dynamic", configurationFields = {
	"ssf.transmitter.auth.client_id",
	"ssf.transmitter.auth.client_secret",
	"ssf.transmitter.auth.token_endpoint",
})
public class OIDSSFTransmitterEventsTest extends AbstractOIDSSFTest {

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Transmitter Metadata", () -> {
			fetchTransmitterMetadata();
		});

		eventLog.runBlock("Validate TLS Connection", () -> {
			validateTlsConnection();
		});

		eventLog.runBlock("Prepare Transmitter Access", () -> {
			callAndStopOnFailure(OIDSSFObtainTransmitterAccessToken.class);
		});

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

		eventLog.runBlock("Validate Transmitter Events", () -> {

			// ensure stream exists
			eventLog.runBlock("Create Stream Configuration", () -> {
				callAndStopOnFailure(OIDSSFCreateStreamConfigCall.class, "OIDSSF-7.1.1.1", "CAEPIOP-2.3.8.2");
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
				callAndContinueOnFailure(CheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
				call(exec().unmapKey("endpoint_response"));
			});

			// Send verification event
			//
			callAndStopOnFailure(OIDSSFTriggerVerificationEvent.class,"OIDSSF-7.1.4.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs204.class,"OIDSSF-7.1.4.2");

			switch (getVariant(SsfDeliveryMode.class)) {
				case POLL: {
					// TODO add support for polling delivery
				}
				break;
				case PUSH:
				{
					// TODO support to read push delivery
					// wait for data received on dynamic endpoint (needs to be reachable externally!)
				}
				break;
			}


		});

		fireTestFinished();
	}

	@Override
	public void cleanup() {
		eventLog.runBlock("Cleanup", () -> {
			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO);
			super.cleanup();
		});
	}
}
