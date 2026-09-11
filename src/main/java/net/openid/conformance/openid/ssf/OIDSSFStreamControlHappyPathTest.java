package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIsAnyOf;
import net.openid.conformance.openid.ssf.SsfConstants.StreamStatus;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReplaceStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFPrepareStreamConfigObjectSetDeliveryMethod;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFPrepareStreamConfigObject;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties.Operation;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckExpectedJsonResponseContents;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckStreamAudience;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckStreamDeliveryMethod;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureStreamDeliveryMatchesRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckSupportedEventsForStream;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureStreamListContainsStream;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureUnknownRequestedEventTypeIgnored;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFListStreamConfigsCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFPrepareStreamConfigObjectAddRequestedEvents;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFPrepareStreamConfigObjectAddUnknownRequestedEvent;
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
import net.openid.conformance.testmodule.TestFailureException;

import java.util.List;
import java.util.Set;

@PublishTestModule(
	testName = "openid-ssf-stream-control-happy-path",
	displayName = "OpenID Shared Signals Framework: Validate Stream Control (Happy Path)",
	summary = """
		This test verifies stream configuration management (happy path).
		The testsuite expects to observe the following interactions:
		 * create a stream; the request also asks for an event type no transmitter knows,
		   which the transmitter must ignore (SSF 1.0 8.1.1)
		 * read the stream configuration, by stream_id and as the list of all streams
		 * update the stream (PATCH) and read it back
		 * replace the stream (PUT), also omitting properties, and read it back
		   (a 202 response is accepted; the read-back then waits for the change to apply)
		 * read the stream status and validate the status document
		 * update the stream status through every status value
		 * delete the stream and verify it is gone from the stream list
		""",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#section-7.2.1
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

			callAndStopOnFailure(OIDSSFPrepareStreamConfigObject.class, "OIDSSF-8.1.1.1");
			callAndStopOnFailure(OIDSSFPrepareStreamConfigObjectAddRequestedEvents.class, "OIDSSF-8.1.1.1");
			// SSF 1.0 8.1.1: "A Transmitter MUST ignore any array values that it does not understand"
			callAndStopOnFailure(OIDSSFPrepareStreamConfigObjectAddUnknownRequestedEvent.class, "OIDSSF-8.1.1");
			callAndContinueOnFailure(OIDSSFPrepareStreamConfigObjectSetDeliveryMethod.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
			callAndStopOnFailure(OIDSSFCreateStreamConfigCall.class, "OIDSSF-8.1.1.1", "CAEPIOP-2.3.8.2");
			rememberSentStreamConfig();
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckStreamAudience.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.1");
			callAndContinueOnFailure(OIDSSFEnsureStreamDeliveryMatchesRequest.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1", "CAEPIOP-2.3.8.1", "OIDSSF-6.1.2");
			callAndContinueOnFailure(OIDSSFStreamRequiredFieldsCheck.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1");
			callAndContinueOnFailure(OIDSSFStreamOptionalFieldsCheck.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1");
			callAndContinueOnFailure(OIDSSFEnsureEventsDeliveredIsSubsetOfSupportedAndRequested.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1");
			callAndContinueOnFailure(OIDSSFEnsureUnknownRequestedEventTypeIgnored.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1");

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
				callAndContinueOnFailure(OIDSSFEnsureAtLeastOneCaepInteropEventInStreamSupportedEvents.class, Condition.ConditionResult.FAILURE, "CAEPIOP-3", "OIDSSF-8.1.1");
			}

			callAndContinueOnFailure(OIDSSFCheckStreamDeliveryMethod.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1", "CAEPIOP-2.3.8.1");
			callAndContinueOnFailure(OIDSSFStreamRequiredFieldsCheck.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1", "OIDSSF-8.1.1.2");
			callAndContinueOnFailure(OIDSSFEnsureEventsDeliveredIsSubsetOfSupportedAndRequested.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1");
			call(exec().unmapKey("endpoint_response"));
		});

		readStreamListAndCheck("Read the list of Stream Configurations", true);

		// Update and Replace are SSF-level operations, not required by CAEP Interop (section 2.3.8.2)
		if (!isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			// this is still not supported in the caep.dev reference env :-/
			eventLog.runBlock("Update Stream Configuration", () -> {

				env.putString("ssf", "delivery_method", deliveryMode.getAlias());

				call(sequence(OIDSSFUpdateStreamConditionSequence.class));
				rememberSentStreamConfig();
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				// SSF 1.0 8.1.1.3: 200 with "the entire updated stream configuration", or 202 "if the
				// update request has been accepted, but not processed"
				callAndContinueOnFailure(new EnsureHttpStatusCodeIsAnyOf(200, 202), Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.3");
				if (!lastResponseWasAccepted()) {
					callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.3");
					callAndContinueOnFailure(new OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties(Operation.UPDATE), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.3");
				}
				call(exec().unmapKey("endpoint_response"));
			});

			readBackStreamConfigurationAndCompare("Read Stream Configuration after update", Operation.UPDATE, "OIDSSF-8.1.1.3");

			// this is still not supported in the caep.dev reference env :-/
			eventLog.runBlock("Replace Stream Configuration", () -> {

				call(sequence(OIDSSFReplaceStreamConditionSequence.class));
				rememberSentStreamConfig();
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				// SSF 1.0 8.1.1.4: 200 with the replaced configuration, or 202 "if the replace
				// request has been accepted, but not processed"
				callAndContinueOnFailure(new EnsureHttpStatusCodeIsAnyOf(200, 202), Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.4");
				if (!lastResponseWasAccepted()) {
					callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.4");
					callAndContinueOnFailure(new OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties(Operation.REPLACE), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.4");
				}
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
				callAndContinueOnFailure(new EnsureHttpStatusCodeIsAnyOf(200, 202), Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.4");
				if (!lastResponseWasAccepted()) {
					callAndContinueOnFailure(new OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties(Operation.REPLACE), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.4");
				}
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
				validateStreamStatusResponse("OIDSSF-8.1.2.1");
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
					// SSF 1.0 8.1.2.2: the response is the updated Stream Status document of 8.1.2.1
					validateStreamStatusResponse("OIDSSF-8.1.2.2", "OIDSSF-8.1.2.1");
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

		// SSF 1.0 8.1.1.2: "In the event that there are no Event Streams configured, the
		// Transmitter MUST return an empty list."
		readStreamListAndCheck("Read the list of Stream Configurations after the delete", false);

		fireTestFinished();
	}

	/**
	 * SSF 1.0 8.1.1.2: a GET without {@code stream_id} returns the list of stream
	 * configurations available to this receiver. Checks that the module's stream is listed
	 * while it exists and no longer listed once deleted.
	 */
	private void readStreamListAndCheck(String blockTitle, boolean expectStreamListed) {
		eventLog.runBlock(blockTitle, () -> {
			callAndStopOnFailure(OIDSSFListStreamConfigsCall.class, "OIDSSF-8.1.1.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.2");
			callAndContinueOnFailure(new OIDSSFEnsureStreamListContainsStream(expectStreamListed), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.2");
			call(exec().unmapKey("endpoint_response"));
		});
	}

	/**
	 * Whether the last stream configuration request was answered with 202 Accepted, i.e.
	 * accepted but not processed yet (SSF 1.0 8.1.1.3 / 8.1.1.4).
	 */
	private boolean lastResponseWasAccepted() {
		Integer status = env.getInteger("resource_endpoint_response_full", "status");
		return status != null && status == 202;
	}

	/** Attempts to observe an accepted-but-not-yet-processed change (202) before grading it. */
	private static final int READ_BACK_ATTEMPTS_AFTER_ACCEPTED = 5;

	private static final int READ_BACK_INTERVAL_SECONDS = 2;

	private void readBackStreamConfigurationAndCompare(String blockTitle, Operation operation, String requirement) {
		boolean accepted = lastResponseWasAccepted();
		eventLog.runBlock(blockTitle, () -> {
			int attempts = accepted ? READ_BACK_ATTEMPTS_AFTER_ACCEPTED : 1;
			for (int attempt = 1; attempt <= attempts; attempt++) {
				callAndStopOnFailure(OIDSSFReadStreamConfigCall.class, "OIDSSF-8.1.1.2");
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.2");
				call(exec().unmapKey("endpoint_response"));
				if (attempt == attempts || readBackReflectsSentConfig(operation)) {
					break;
				}
				// 8.1.1.3 / 8.1.1.4: after a 202 the receiver "MAY try the same request later to
				// get processing result" - give the transmitter a moment to apply the change
				eventLog.log(getName(), "Transmitter answered 202 and the change is not visible yet; reading the stream configuration again in "
					+ READ_BACK_INTERVAL_SECONDS + "s (attempt " + attempt + "/" + attempts + ")");
				try {
					Thread.sleep(READ_BACK_INTERVAL_SECONDS * 1000L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new TestFailureException(getId(), "Interrupted while waiting for the transmitter to process the accepted request");
				}
			}
			// a transmitter that answered 200 and echoed the request but did not persist it fails here
			callAndContinueOnFailure(new OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties(operation), Condition.ConditionResult.FAILURE, requirement);
		});
	}

	private boolean readBackReflectsSentConfig(Operation operation) {
		JsonElement sentEl = env.getElementFromObject("ssf", "expected_stream_config");
		JsonElement actualEl = env.getElementFromObject("ssf", "stream");
		if (sentEl == null || !sentEl.isJsonObject() || actualEl == null || !actualEl.isJsonObject()) {
			return false;
		}
		Set<String> mismatches = OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties.computeMismatches(sentEl.getAsJsonObject(), actualEl.getAsJsonObject(), operation);
		return mismatches.isEmpty();
	}


	@Override
	public void cleanup() {
		eventLog.runBlock("Cleanup", () -> {
			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO);
			super.cleanup();
		});
	}
}
