package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204;
import net.openid.conformance.openid.federation.EnsureResponseIsJsonObject;
import net.openid.conformance.openid.ssf.SsfConstants.StreamStatus;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckExpectedJsonResponseContents;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckStreamAudience;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckStreamDeliveryMethod;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckSupportedEventsForStream;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamStatusCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReplaceStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFUpdateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFUpdateStreamStatusCall;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-stream-control-happy-path",
	displayName = "OpenID Shared Signals Framework: Validate Stream Control (Happy Path)",
	summary = "This test verifies the behavior of the stream control. It performs stream create, read, update, replace, delete operations and attempts to update the stream status.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-6.2.1
	}
)
public class OIDSSFStreamControlHappyPathTest extends AbstractOIDSSFTransmitterTestModule {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Transmitter Metadata", this::fetchTransmitterMetadata);

		eventLog.runBlock("Validate TLS Connection", this::validateTlsConnection);

		// see https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html
		// OID_CAEP_INTEROP https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html
		eventLog.runBlock("Prepare Transmitter Access", this::obtainTransmitterAccessToken);

		eventLog.runBlock("Clean stream environment if necessary", () -> {
			cleanUpStreamConfigurationIfNecessary();
		});

		eventLog.runBlock("Create Stream Configuration", () -> {

			SsfDeliveryMode deliveryMode = getVariant(SsfDeliveryMode.class);
			env.putString("ssf", "delivery_method", deliveryMode.getAlias());

			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckStreamAudience.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckStreamDeliveryMethod.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1", "CAEPIOP-2.3.8.1");

			call(exec().unmapKey("endpoint_response"));
		});

		eventLog.runBlock("Read Stream Configuration", () -> {
			callAndStopOnFailure(OIDSSFReadStreamConfigCall.class, "OIDSSF-7.1.1.2", "CAEPIOP-2.3.8.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.2");
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.2");

			callAndContinueOnFailure(OIDSSFCheckSupportedEventsForStream.class, Condition.ConditionResult.WARNING,"OIDSSF-7.1.4.1", "OIDCAEP-3");
			callAndContinueOnFailure(OIDSSFCheckStreamDeliveryMethod.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1", "CAEPIOP-2.3.8.1");
			// TODO check: In the event that there are no Event Streams configured, the Transmitter MUST return an empty list.
			// TODO check: stream configuration response
			call(exec().unmapKey("endpoint_response"));
		});

		// this is still not supported in the caep.dev reference env :-/
		eventLog.runBlock("Update Stream Configuration", () -> {

			SsfDeliveryMode deliveryMode = getVariant(SsfDeliveryMode.class);
			env.putString("ssf", "delivery_method", deliveryMode.getAlias());

			call(sequence(OIDSSFUpdateStreamConditionSequence.class));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.3");
			// TODO check for 202 response
			// 202	if the update request has been accepted, but not processed. Receiver MAY try the same request later to get processing result.
			// TODO check for changed value
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.3");
			call(exec().unmapKey("endpoint_response"));
		});

		// this is still not supported in the caep.dev reference env :-/
		eventLog.runBlock("Replace Stream Configuration", () -> {

			call(sequence(OIDSSFReplaceStreamConditionSequence.class));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.4");
			// TODO check for 202 responses
			// 202	if the replace request has been accepted, but not processed. Receiver MAY try the same request later in order to get processing result.
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.4");
			call(exec().unmapKey("endpoint_response"));
		});

		String statusEndpoint = env.getString("ssf", "transmitter_metadata.status_endpoint");
		eventLog.runBlock("Read Stream Status", () -> {
			if (statusEndpoint != null) {
				// stream status
				callAndStopOnFailure(OIDSSFReadStreamStatusCall.class, "OIDSSF-7.1.2.1");
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.2.1");
				// TODO check: status response
				call(exec().unmapKey("endpoint_response"));
			} else {
				eventLog.log("Skipping unsupported Read Stream Status Checks, because status_endpoint is missing in ssf-configuration", args());
			}
		});

		eventLog.runBlock("Update Stream Status", () -> {
			if (statusEndpoint == null) {
				eventLog.log("Skipping unsupported Update Stream Status Checks, because status_endpoint is missing in ssf-configuration.", args());
				return;
			}

			for (StreamStatus status : StreamStatus.values()) {
				eventLog.log(getName(), "Update stream status to " + status);
				callAndContinueOnFailure(new OIDSSFUpdateStreamStatusCall(status), Condition.ConditionResult.FAILURE, "OIDSSF-7.1.2.1", "OIDSSF-7.1.2.2", "CAEPIOP-2.3.5");
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.2.2");
				callAndContinueOnFailure(EnsureResponseIsJsonObject.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.2.2");
				// TODO check: status response
				callAndContinueOnFailure(new OIDSSFCheckExpectedJsonResponseContents(args("status", status.name())), Condition.ConditionResult.WARNING, "OIDSSF-7.1.2.2");
			}

			call(exec().unmapKey("endpoint_response"));
		});

		eventLog.runBlock("Delete Stream Configuration", () -> {
			callAndStopOnFailure(OIDSSFDeleteStreamConfigCall.class, "OIDSSF-7.1.1.5", "CAEPIOP-2.3.8.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-7.1.1.5");
			call(exec().unmapKey("endpoint_response"));
		});

		//OID_CAEP_INTEROP-2.3.8.2 Stream control (except Status) - check for API availability
		// - Creating a Stream
		// - Reading Stream Configuration
		// - Getting the Stream Status
		// - Stream Verification

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
