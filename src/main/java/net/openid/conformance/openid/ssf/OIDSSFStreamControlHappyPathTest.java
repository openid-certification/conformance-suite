package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204;
import net.openid.conformance.openid.federation.EnsureResponseIsJsonObject;
import net.openid.conformance.openid.ssf.SsfConstants.StreamStatus;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReplaceStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFPrepareStreamConfigObjectSetDeliveryMethod;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFPrepareStreamConfigObject;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties.Operation;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckExpectedJsonResponseContents;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckStreamAudience;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckStreamDeliveryMethod;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckSupportedEventsForStream;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureEventsDeliveredIsSubsetOfSupportedAndRequested;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureAtLeastOneCaepInteropEventInStreamSupportedEvents;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamStatusCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReplaceStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamOptionalFieldsCheck;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamRequiredFieldsCheck;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFUpdateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFUpdateStreamStatusCall;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;

@PublishTestModule(
	testName = "openid-ssf-stream-control-happy-path",
	displayName = "OpenID Shared Signals Framework: Validate Stream Control (Happy Path)",
	summary = """
		This test verifies stream configuration management (happy path).
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * read the stream configuration
		 * update the stream
		 * replace the stream
		 * read the stream status
		 * update the stream status
		 * delete the stream
		""",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-6.2.1
	}
)
public class OIDSSFStreamControlHappyPathTest extends AbstractOIDSSFTransmitterTestModule {

	@Override
	public void start() {
		super.start();
		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Transmitter Metadata", this::fetchTransmitterMetadata);

		eventLog.runBlock("Validate TLS Connection", this::validateTlsConnection);

		// see https://openid.net/specs/openid-caep-interoperability-profile-1_0-01.html
		eventLog.runBlock("Prepare Transmitter Access", this::obtainTransmitterAccessToken);

		eventLog.runBlock("Clean stream environment if necessary", () -> {
			cleanUpStreamConfigurationIfNecessary();
		});

		eventLog.runBlock("Create Stream Configuration", () -> {

			env.putString("ssf", "delivery_method", deliveryMode.getAlias());
			if (deliveryMode == SsfDeliveryMode.PUSH) {
				configurePushAuthorizationHeader(null, pushAuthorizationHeader);
			}

			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckStreamAudience.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckStreamDeliveryMethod.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1", "CAEPIOP-2.3.8.1");
			callAndContinueOnFailure(OIDSSFStreamRequiredFieldsCheck.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1");
			callAndContinueOnFailure(OIDSSFStreamOptionalFieldsCheck.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1");
			callAndContinueOnFailure(OIDSSFEnsureEventsDeliveredIsSubsetOfSupportedAndRequested.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1");

			call(exec().unmapKey("endpoint_response"));
		});

		eventLog.runBlock("Read Stream Configuration", () -> {
			callAndStopOnFailure(OIDSSFReadStreamConfigCall.class, "OIDSSF-8.1.1.2", "CAEPIOP-2.3.8.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.2");
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.2");

			callAndContinueOnFailure(OIDSSFCheckSupportedEventsForStream.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1");

			SsfProfile ssfProfile = getVariant(SsfProfile.class);
			if (SsfProfile.CAEP_INTEROP.equals(ssfProfile)) {
				callAndContinueOnFailure(OIDSSFEnsureAtLeastOneCaepInteropEventInStreamSupportedEvents.class, Condition.ConditionResult.FAILURE, "CAEPIOP-3");
			}

			callAndContinueOnFailure(OIDSSFCheckStreamDeliveryMethod.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1", "CAEPIOP-2.3.8.1");
			// TODO check: In the event that there are no Event Streams configured, the Transmitter MUST return an empty list.
			// TODO check: stream configuration response
			call(exec().unmapKey("endpoint_response"));
		});

		// Update and Replace are SSF-level operations, not required by CAEP Interop (section 2.3.8.2)
		if (!isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			// this is still not supported in the caep.dev reference env :-/
			eventLog.runBlock("Update Stream Configuration", () -> {

				env.putString("ssf", "delivery_method", deliveryMode.getAlias());

				call(sequence(OIDSSFUpdateStreamConditionSequence.class));
				rememberSentStreamConfig();
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.3");
				// TODO check for 202 response
				// 202	if the update request has been accepted, but not processed. Receiver MAY try the same request later to get processing result.
				callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.3");
				// 8.1.1.3: the response is "the entire updated stream configuration"
				callAndContinueOnFailure(new OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties(Operation.UPDATE), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.3");
				call(exec().unmapKey("endpoint_response"));
			});

			readBackStreamConfigurationAndCompare("Read Stream Configuration after update", Operation.UPDATE, "OIDSSF-8.1.1.3");

			// this is still not supported in the caep.dev reference env :-/
			eventLog.runBlock("Replace Stream Configuration", () -> {

				call(sequence(OIDSSFReplaceStreamConditionSequence.class));
				rememberSentStreamConfig();
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.4");
				// TODO check for 202 responses
				// 202	if the replace request has been accepted, but not processed. Receiver MAY try the same request later in order to get processing result.
				callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.4");
				callAndContinueOnFailure(new OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties(Operation.REPLACE), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.4");
				call(exec().unmapKey("endpoint_response"));
			});

			readBackStreamConfigurationAndCompare("Read Stream Configuration after replace", Operation.REPLACE, "OIDSSF-8.1.1.4");

			// SSF 1.0 8.1.1.4: "Missing Receiver-Supplied properties MUST be interpreted as
			// requested to be deleted" - a second PUT without the description and with a
			// narrower events_requested must drop the description and shrink events_delivered.
			eventLog.runBlock("Replace Stream Configuration omitting the description", () -> {
				callAndStopOnFailure(OIDSSFPrepareStreamConfigObject.class, "OIDSSF-8.1.1.4");
				callAndContinueOnFailure(OIDSSFPrepareStreamConfigObjectSetDeliveryMethod.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.4");
				JsonObject reduced = env.getElementFromObject("ssf", "stream.config").getAsJsonObject().deepCopy();
				reduced.remove("description");
				reduced.addProperty("stream_id", env.getString("ssf", "stream.stream_id"));
				reduced.add("events_requested", OIDFJSON.convertListToJsonArray(List.of(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE)));
				env.putObject("ssf", "stream.config_override", reduced);

				callAndStopOnFailure(OIDSSFReplaceStreamConfigCall.class, "OIDSSF-8.1.1.4");
				rememberSentStreamConfig();
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.4");
				callAndContinueOnFailure(new OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties(Operation.REPLACE), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.4");
				call(exec().unmapKey("endpoint_response"));
			});

			readBackStreamConfigurationAndCompare("Read Stream Configuration after the reducing replace", Operation.REPLACE, "OIDSSF-8.1.1.4");
		}

		String statusEndpoint = env.getString("ssf", "transmitter_metadata.status_endpoint");
		eventLog.runBlock("Read Stream Status", () -> {
			if (statusEndpoint != null) {
				// stream status
				callAndStopOnFailure(OIDSSFReadStreamStatusCall.class, "OIDSSF-8.1.2.1", "CAEPIOP-2.3.5");
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.2.1");
				// TODO check: status response
				call(exec().unmapKey("endpoint_response"));
			} else {
				eventLog.log("Skipping unsupported Read Stream Status Checks, because status_endpoint is missing in ssf-configuration", args());
			}
		});

		// Update Stream Status is an SSF-level operation, not required by CAEP Interop (section 2.3.5 only requires Read)
		if (!isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			eventLog.runBlock("Update Stream Status", () -> {
				if (statusEndpoint == null) {
					eventLog.log("Skipping unsupported Update Stream Status Checks, because status_endpoint is missing in ssf-configuration.", args());
					return;
				}

				for (StreamStatus status : StreamStatus.values()) {
					eventLog.log(getName(), "Update stream status to " + status);
					callAndContinueOnFailure(new OIDSSFUpdateStreamStatusCall(status), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.2");
					call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
					callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.2.2");
					callAndContinueOnFailure(EnsureResponseIsJsonObject.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.2.2");
					// TODO check: status response
					callAndContinueOnFailure(new OIDSSFCheckExpectedJsonResponseContents(args("status", status.name())), Condition.ConditionResult.WARNING, "OIDSSF-8.1.2.2");
				}

				call(exec().unmapKey("endpoint_response"));
			});
		}

		eventLog.runBlock("Delete Stream Configuration", () -> {
			callAndStopOnFailure(OIDSSFDeleteStreamConfigCall.class, "OIDSSF-8.1.1.5", "CAEPIOP-2.3.8.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-8.1.1.5");
			call(exec().unmapKey("endpoint_response"));
		});

		fireTestFinished();
	}

	/**
	 * Keeps the body of the PATCH or PUT just sent, so the returned and the read-back stream
	 * configuration can be compared with it after {@code ssf.stream} has been replaced by the
	 * transmitter's response.
	 */
	private void rememberSentStreamConfig() {
		env.putObjectFromJsonString("ssf", "expected_stream_config", env.getString("resource_request_entity"));
	}

	private void readBackStreamConfigurationAndCompare(String blockTitle, Operation operation, String requirement) {
		eventLog.runBlock(blockTitle, () -> {
			callAndStopOnFailure(OIDSSFReadStreamConfigCall.class, "OIDSSF-8.1.1.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.2");
			// a transmitter that answered 200 and echoed the request but did not persist it fails here
			callAndContinueOnFailure(new OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties(operation), Condition.ConditionResult.FAILURE, requirement);
			call(exec().unmapKey("endpoint_response"));
		});
	}


	@Override
	public void cleanup() {
		eventLog.runBlock("Cleanup", () -> {
			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO);
			super.cleanup();
		});
	}
}
