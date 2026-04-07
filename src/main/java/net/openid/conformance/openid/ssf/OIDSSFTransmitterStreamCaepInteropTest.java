package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.WaitFor5Seconds;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCallPollEndpoint;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationEventState;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationEventSubjectId;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureAllCaepInteropEventsReceived;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureEventContainsStreamAudience;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureEventSignedWithRsa256;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenContainsSingleEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenDoesNotContainExpClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenDoesNotContainSubClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenIatIsNotInFuture;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenIssuerMatchesStreamConfigurationIssuer;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractCaepEventData;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractReceivedSETs;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractVerificationEventFromPushRequest;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractVerificationEventFromReceivedSETs;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFParseVerificationEventToken;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFTriggerVerificationEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateCaepCommonOptionalFields;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateCaepCredentialChangeEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateCaepDeviceComplianceChangeEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateSecurityEventTokenAudClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateSecurityEventTokenTxnClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFVerifySignatureOfVerificationEventToken;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckStreamAudience;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckStreamDeliveryMethod;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckSupportedEventsForStream;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureAtLeastOneCaepInteropEventInStreamSupportedEvents;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamStatusCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamOptionalFieldsCheck;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamRequiredFieldsCheck;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@PublishTestModule(
	testName = "openid-ssf-transmitter-stream-caep-interop",
	displayName = "OpenID Shared Signals Framework: CAEP Interop Transmitter Test",
	summary = """
		This test exercises a transmitter against the full CAEP Interop Profile 1.0 receiver expectations.
		It performs stream creation, configuration read, status read, and stream verification.
		After successful verification, the test waits for the transmitter to deliver the following \
		CAEP events: session-revoked, credential-change, and device-compliance-change. \
		These events must be triggered on the transmitter side (e.g. via the transmitter's admin UI) \
		and can be delivered in any order. \
		Each received CAEP event is validated against the CAEP 1.0 Final specification.
		For PUSH delivery, events are received on the exposed push endpoint.
		For POLL delivery, events are retrieved and acknowledged via consecutive poll requests.""",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix",
	}
)
public class OIDSSFTransmitterStreamCaepInteropTest extends AbstractOIDSSFTransmitterTestModule {

	volatile boolean streamDeletedSuccessfully = false;

	@Override
	public void start() {
		super.start();
		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Transmitter Metadata", () -> {
			fetchTransmitterMetadata();
			callAndStopOnFailure(FetchServerKeys.class);
		});

		eventLog.runBlock("Validate TLS Connection", this::validateTlsConnection);

		eventLog.runBlock("Prepare Transmitter Access", this::obtainTransmitterAccessToken);

		eventLog.runBlock("Clean stream environment if necessary", this::cleanUpStreamConfigurationIfNecessary);

		SsfDeliveryMode deliveryMode = getVariant(SsfDeliveryMode.class);

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
			call(exec().unmapKey("endpoint_response"));
		});

		eventLog.runBlock("Read Stream Configuration", () -> {
			callAndStopOnFailure(OIDSSFReadStreamConfigCall.class, "OIDSSF-8.1.1.2", "CAEPIOP-2.3.8.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.2");
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.2");
			callAndContinueOnFailure(OIDSSFCheckSupportedEventsForStream.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.4.1", "OIDCAEP-3");
			callAndContinueOnFailure(OIDSSFEnsureAtLeastOneCaepInteropEventInStreamSupportedEvents.class, Condition.ConditionResult.FAILURE, "CAEPIOP-3");
			callAndContinueOnFailure(OIDSSFCheckStreamDeliveryMethod.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1", "CAEPIOP-2.3.8.1");
			call(exec().unmapKey("endpoint_response"));
		});

		String statusEndpoint = env.getString("ssf", "transmitter_metadata.status_endpoint");
		eventLog.runBlock("Read Stream Status", () -> {
			if (statusEndpoint != null) {
				callAndStopOnFailure(OIDSSFReadStreamStatusCall.class, "OIDSSF-8.1.2.1", "CAEPIOP-2.3.5");
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.2.1");
				call(exec().unmapKey("endpoint_response"));
			} else {
				eventLog.log("Skipping Read Stream Status: status_endpoint missing in ssf-configuration", args());
			}
		});

		String verificationEndpoint = env.getString("ssf", "transmitter_metadata.verification_endpoint");
		if (verificationEndpoint == null) {
			throw new TestFailureException(getId(), "Transmitter metadata does not include a verification_endpoint, "
				+ "which is required by the CAEP Interop Profile (CAEPIOP-2.3.6).");
		}

		eventLog.runBlock("Trigger Stream Verification", () -> {
			callAndStopOnFailure(OIDSSFTriggerVerificationEvent.class, "OIDSSF-8.1.4.2", "CAEPIOP-2.3.8.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-8.1.4.2");
			call(exec().unmapKey("endpoint_response"));

			callAndContinueOnFailure(WaitFor5Seconds.class, Condition.ConditionResult.INFO);
		});

		eventLog.log(getName(), "Stream verification successful. "
			+ "Waiting for the transmitter to deliver CAEP events: "
			+ "session-revoked, credential-change, device-compliance-change. "
			+ "Please trigger these events on the transmitter now.");

		switch (deliveryMode) {
			case PUSH:
				receiveEventsViaPush();
				break;
			case POLL:
				retrieveAndAcknowledgeEventsViaPoll();
				break;
			default:
				break;
		}

		eventLog.runBlock("Delete Stream Configuration", () -> {
			callAndStopOnFailure(OIDSSFDeleteStreamConfigCall.class, "OIDSSF-8.1.1.5", "CAEPIOP-2.3.8.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-8.1.1.5");
			call(exec().unmapKey("endpoint_response"));

			streamDeletedSuccessfully = true;
		});

		fireTestFinished();
	}

	protected void receiveEventsViaPush() {
		// Wait for push OUTSIDE runBlock — runBlock is synchronized on eventLog,
		// which would block the HTTP handler thread from storing the push request.
		waitForNextPushRequest();
		eventLog.runBlock("Validate verification event via PUSH delivery", () -> {
			callAndStopOnFailure(OIDSSFExtractVerificationEventFromPushRequest.class, "OIDSSF-8.1.4.1");

			validateSetCommon();
			callAndContinueOnFailure(OIDSSFCheckVerificationEventState.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			callAndContinueOnFailure(OIDSSFCheckVerificationEventSubjectId.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
		});

		Set<String> receivedEventTypes = new LinkedHashSet<>();

		// Expect 3 CAEP events: session-revoked, credential-change, device-compliance-change
		for (int i = 0; i < 3; i++) {
			waitForNextPushRequest();
			callAndStopOnFailure(OIDSSFExtractVerificationEventFromPushRequest.class, "OIDSSF-8.1.4.1");

			// Parse first to determine the event type for the block name
			callAndStopOnFailure(OIDSSFParseVerificationEventToken.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			callAndContinueOnFailure(OIDSSFExtractCaepEventData.class, Condition.ConditionResult.FAILURE, "OIDCAEP-3");
			String eventType = env.getString("ssf", "caep_event.type");
			String eventName = eventType != null ? eventType.substring(eventType.lastIndexOf('/') + 1) : "unknown";

			eventLog.runBlock("Validate CAEP event: " + eventName, () -> {
				validateSetCommonAfterParsing();

				if (eventType != null) {
					receivedEventTypes.add(eventType);
					callAndContinueOnFailure(OIDSSFValidateCaepCommonOptionalFields.class, Condition.ConditionResult.WARNING, "OIDCAEP-2");
					validateCaepEventFields(eventType);
				}
			});
		}

		eventLog.runBlock("Verify all expected CAEP Interop events received", () -> {
			callAndContinueOnFailure(new OIDSSFEnsureAllCaepInteropEventsReceived(receivedEventTypes),
				Condition.ConditionResult.FAILURE, "CAEPIOP-3");
		});
	}

	protected void retrieveAndAcknowledgeEventsViaPoll() {
		eventLog.runBlock("Poll for verification event", () -> {
			env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_ONLY.name());
			callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-8.1.4.1", "RFC8936-2.4");
			env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
			callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);
			callAndStopOnFailure(OIDSSFExtractVerificationEventFromReceivedSETs.class);

			validateSetCommon();
			callAndContinueOnFailure(OIDSSFCheckVerificationEventState.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			callAndContinueOnFailure(OIDSSFCheckVerificationEventSubjectId.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
		});

		Set<String> receivedEventTypes = new LinkedHashSet<>();

		// Poll repeatedly until all 3 CAEP event types are received, or timeout.
		// Each poll acknowledges previously received SETs and retrieves new ones.
		int pollIntervalSeconds = 5;
		int maxAttempts = 12; // 12 x 5s = 60 seconds
		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			eventLog.runBlock("Poll for CAEP events (attempt " + (attempt + 1) + ")", () -> {
				env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_AND_ACKNOWLEDGE.name());
				callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-8.1.4.1", "RFC8936-2.4");
				env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
				callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);
			});

			JsonElement pollSetsEl = env.getElementFromObject("ssf", "poll.sets");
			if (pollSetsEl != null && pollSetsEl.isJsonObject() && !pollSetsEl.getAsJsonObject().isEmpty()) {
				processCaepEventsFromPollResponse(pollSetsEl.getAsJsonObject(), receivedEventTypes);
			}

			if (receivedEventTypes.containsAll(SsfEvents.CAEP_INTEROP_EVENT_TYPES)) {
				break;
			}

			if (attempt < maxAttempts - 1) {
				eventLog.log(getName(), "Waiting for CAEP events... received "
					+ receivedEventTypes.size() + "/3, polling again in " + pollIntervalSeconds + "s");
				try {
					Thread.sleep(pollIntervalSeconds * 1000L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new TestFailureException(getId(), "Interrupted while waiting for CAEP events");
				}
			}
		}

		eventLog.runBlock("Verify all expected CAEP Interop events were received", () -> {
			callAndContinueOnFailure(new OIDSSFEnsureAllCaepInteropEventsReceived(receivedEventTypes),
				Condition.ConditionResult.FAILURE, "CAEPIOP-3");
		});

		// Final acknowledgment for any remaining unacked SETs
		eventLog.runBlock("Acknowledge remaining events", () -> {
			JsonElement remainingSets = env.getElementFromObject("ssf", "poll.sets");
			if (remainingSets != null && remainingSets.isJsonObject() && !remainingSets.getAsJsonObject().isEmpty()) {
				env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.ACKNOWLEDGE_ONLY.name());
				callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-8.1.4.1", "RFC8936-2.4");
			}
		});
	}

	protected void processCaepEventsFromPollResponse(JsonObject pollSets, Set<String> receivedEventTypes) {
		for (Map.Entry<String, JsonElement> entry : pollSets.entrySet()) {
			String setToken = OIDFJSON.getString(entry.getValue());
			env.putString("ssf", "verification.jwt", setToken);

			callAndStopOnFailure(OIDSSFParseVerificationEventToken.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			callAndContinueOnFailure(OIDSSFExtractCaepEventData.class, Condition.ConditionResult.FAILURE, "OIDCAEP-3");
			String eventType = env.getString("ssf", "caep_event.type");
			if (eventType == null) {
				continue;
			}
			String eventName = eventType.substring(eventType.lastIndexOf('/') + 1);

			eventLog.runBlock("Validate CAEP event: " + eventName, () -> {
				validateSetCommonAfterParsing();

				receivedEventTypes.add(eventType);
				callAndContinueOnFailure(OIDSSFValidateCaepCommonOptionalFields.class, Condition.ConditionResult.WARNING, "OIDCAEP-2");
				validateCaepEventFields(eventType);
			});
		}
	}

	/**
	 * Validates common SET (Security Event Token) properties that apply to all events:
	 * signature, JWT type, single event, no sub/exp claims, issuer, iat, aud.
	 * Includes parsing the JWT token.
	 */
	protected void validateSetCommon() {
		callAndContinueOnFailure(OIDSSFVerifySignatureOfVerificationEventToken.class, Condition.ConditionResult.WARNING);
		callAndStopOnFailure(OIDSSFParseVerificationEventToken.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
		validateSetCommonAfterParsing();
	}

	/**
	 * Validates common SET properties assuming the token has already been parsed
	 * (i.e. {@code set_token} is already in the environment).
	 */
	protected void validateSetCommonAfterParsing() {
		callAndContinueOnFailure(OIDSSFEnsureEventSignedWithRsa256.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.6");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.1");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenContainsSingleEvent.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.8.1");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenDoesNotContainSubClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.2");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenDoesNotContainExpClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.7");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenIssuerMatchesStreamConfigurationIssuer.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.6");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenIatIsNotInFuture.class, Condition.ConditionResult.FAILURE, "RFC8417-2.2", "RFC7519-4.1.6");
		callAndContinueOnFailure(OIDSSFValidateSecurityEventTokenAudClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.8");
		callAndContinueOnFailure(OIDSSFEnsureEventContainsStreamAudience.class, Condition.ConditionResult.WARNING, "RFC7519-4.1.3");
		callAndContinueOnFailure(OIDSSFValidateSecurityEventTokenTxnClaim.class, Condition.ConditionResult.INFO, "OIDSSF-4.1.9");
	}

	/**
	 * Validates event-type-specific required fields for a CAEP event.
	 */
	protected void validateCaepEventFields(String eventType) {
		switch (eventType) {
			case SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE:
				// No required event-specific fields per CAEP 1.0 Section 3.1
				eventLog.log(getName(), "Validated session-revoked event (no required event-specific fields)");
				break;
			case SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE:
				callAndContinueOnFailure(OIDSSFValidateCaepCredentialChangeEvent.class,
					Condition.ConditionResult.FAILURE, "OIDCAEP-3.3");
				break;
			case SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE:
				callAndContinueOnFailure(OIDSSFValidateCaepDeviceComplianceChangeEvent.class,
					Condition.ConditionResult.FAILURE, "OIDCAEP-3.5");
				break;
			default:
				eventLog.log(getName(), "Received CAEP event type: " + eventType);
				break;
		}
	}

	/**
	 * Waits for the next push request to arrive. Uses a generous timeout (up to 3 minutes)
	 * to allow manual event triggering on external transmitters like caep.dev.
	 * Returns immediately once a push request arrives.
	 */
	protected void waitForNextPushRequest() {
		int pollTimeoutSeconds = 10;
		int maxAttempts = 6; // 6 x 10s = 60 seconds total
		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			if (lookupNextPushRequest(pollTimeoutSeconds) != null) {
				return;
			}
			eventLog.log(getName(), "Waiting for push delivery... ("
				+ ((attempt + 1) * pollTimeoutSeconds) + "s / " + (maxAttempts * pollTimeoutSeconds) + "s)");
		}
		throw new TestFailureException(getId(), "Did not receive push request after " + (maxAttempts * pollTimeoutSeconds) + " seconds");
	}

	@Override
	public void cleanup() {
		eventLog.runBlock("Cleanup", () -> {
			if (!streamDeletedSuccessfully) {
				callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO);
			}
			super.cleanup();
		});
	}
}
