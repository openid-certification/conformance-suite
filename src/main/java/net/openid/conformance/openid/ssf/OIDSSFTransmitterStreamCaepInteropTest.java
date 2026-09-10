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
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureCaepInteropEventReasonAdminPresent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureCaepInteropEventSubjectFormat;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureEventSignerRsaKeySizeAtLeast2048Bits;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnCaepInteropEventUsesComplexSubject;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureUnsolicitedVerificationEventHasNoState;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureEventContainsStreamAudience;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureEventSignedWithRsa256;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenContainsSingleEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenDoesNotContainExpClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenDoesNotContainSubClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenIatIsNotInFuture;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenIssuerMatchesStreamConfigurationIssuer;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnSecurityEventTokenTypeNotInPreferredForm;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractCaepEventData;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractReceivedSETs;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractVerificationEventFromPushRequest;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFLogAcceptedUnsolicitedVerificationEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFLogObservedSubjectFormats;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFParseSecurityEventToken;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFRecordSecurityEventTokenSubjectFormat;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFTriggerVerificationEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateCaepCommonOptionalFields;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateCaepCredentialChangeEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateCaepDeviceComplianceChangeEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateCaepRiskLevelChangeEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateCaepAssuranceLevelChangeEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateCaepSessionEstablishedEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateCaepSessionPresentedEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateCaepTokenClaimsChangeEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateStreamUpdatedEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnCaepAssuranceLevelChangeDirectionMissing;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnStreamUpdatedEventUnknownMembers;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnNonStandardCaepCredentialChangeValues;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateSecurityEventTokenAudClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateSecurityEventTokenJtiClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateSecurityEventTokenSubIdClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnSecurityEventTokenSubIdUnknownMembers;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateSecurityEventTokenTxnClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFVerifySignatureOfSecurityEventToken;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWaitForMinVerificationInterval;
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
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamOptionalFieldsCheck;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamRequiredFieldsCheck;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.openid.conformance.openid.ssf.SsfEvents.SSF_STREAM_UPDATED_EVENT_TYPE;

@PublishTestModule(
	testName = "openid-ssf-transmitter-stream-caep-interop",
	displayName = "OpenID Shared Signals Framework: CAEP Interop Transmitter Test",
	summary = """
		This test verifies a transmitter against the CAEP Interop Profile 1.0.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * read the stream configuration
		 * read the stream status
		 * trigger and receive the stream verification event
		 * receive the expected CAEP events (session-revoked, credential-change,
		   device-compliance-change — as advertised in events_delivered)
		 * delete the stream

		The expected CAEP events are the qualifying use cases of the CAEP Interop Profile
		draft-01 (sections 3.1-3.3) found in the stream's events_delivered field. Other
		advertised CAEP events (e.g. risk-level-change, which the profile's working-group
		draft adds as a use case) are validated and acknowledged if the transmitter delivers
		them, but are not required.
		These events must be triggered on the transmitter side (e.g. via the transmitter's
		admin UI) and may be delivered in any order. Each received CAEP event is validated
		against the CAEP 1.0 Final specification. For PUSH delivery, events are received on
		the exposed push endpoint; for POLL delivery, events are retrieved and acknowledged
		via consecutive poll requests.
		""",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix",
	}
)
public class OIDSSFTransmitterStreamCaepInteropTest extends AbstractOIDSSFTransmitterTestModule {

	/**
	 * Maximum number of unsolicited (stateless) verification events to tolerate
	 * while waiting for the solicited response. Per SSF 1.0 §8.1.4 a transmitter
	 * MAY deliver unsolicited verification events at any time; per §8.1.4.2 the
	 * transmitter MUST echo the state of a verification request, so after
	 * tolerating some unsolicited events we still require the solicited echo.
	 */
	protected static final int MAX_UNSOLICITED_VERIFICATION_EVENTS = 10;

	protected static final int VERIFICATION_POLL_MAX_ATTEMPTS = 12;

	protected static final int VERIFICATION_POLL_INTERVAL_SECONDS = 5;

	volatile boolean streamDeletedSuccessfully = false;

	/**
	 * The CAEP event types that the transmitter confirmed it will deliver,
	 * extracted from the stream's {@code events_delivered} field after creation.
	 */
	volatile Set<String> expectedCaepEventTypes;

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

		eventLog.runBlock("Create Stream Configuration", () -> {
			env.putString("ssf", "delivery_method", deliveryMode.getAlias());
			if (deliveryMode == SsfDeliveryMode.PUSH) {
				configurePushAuthorizationHeader(null, pushAuthorizationHeader);
			}

			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			rememberSentStreamConfig();
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckStreamAudience.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckStreamDeliveryMethod.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1", "CAEPIOP-2.3.8.1");
			callAndContinueOnFailure(OIDSSFStreamRequiredFieldsCheck.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1");
			callAndContinueOnFailure(OIDSSFStreamOptionalFieldsCheck.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1");
			callAndContinueOnFailure(OIDSSFEnsureEventsDeliveredIsSubsetOfSupportedAndRequested.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1");
			call(exec().unmapKey("endpoint_response"));

			// Determine which CAEP events the transmitter will actually deliver
			expectedCaepEventTypes = extractCaepEventsDelivered();
		});

		eventLog.runBlock("Read Stream Configuration", () -> {
			callAndStopOnFailure(OIDSSFReadStreamConfigCall.class, "OIDSSF-8.1.1.2", "CAEPIOP-2.3.8.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.2");
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.2");
			callAndContinueOnFailure(OIDSSFCheckSupportedEventsForStream.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1");
			callAndContinueOnFailure(OIDSSFEnsureAtLeastOneCaepInteropEventInStreamSupportedEvents.class, Condition.ConditionResult.FAILURE, "CAEPIOP-3", "OIDSSF-8.1.1");
			callAndContinueOnFailure(OIDSSFCheckStreamDeliveryMethod.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1", "CAEPIOP-2.3.8.1");
			call(exec().unmapKey("endpoint_response"));
		});

		String statusEndpoint = env.getString("ssf", "transmitter_metadata.status_endpoint");
		eventLog.runBlock("Read Stream Status", () -> {
			if (statusEndpoint != null) {
				callAndStopOnFailure(OIDSSFReadStreamStatusCall.class, "OIDSSF-8.1.2.1", "CAEPIOP-2.3.5");
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.1");
				validateStreamStatusResponse("OIDSSF-8.1.2.1", "CAEPIOP-2.3.5");
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
			callAndContinueOnFailure(OIDSSFWaitForMinVerificationInterval.class, Condition.ConditionResult.INFO, "OIDSSF-8.1.1", "OIDSSF-8.1.4.2");
			callAndStopOnFailure(OIDSSFTriggerVerificationEvent.class, "OIDSSF-8.1.4.2", "CAEPIOP-2.3.8.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-8.1.4.2");
			call(exec().unmapKey("endpoint_response"));

			callAndContinueOnFailure(WaitFor5Seconds.class, Condition.ConditionResult.INFO);
		});

		eventLog.log(getName(), args(
			"msg", "Stream verification successful. Waiting for the transmitter to deliver CAEP events. Please trigger these events on the transmitter now.",
			"expected_caep_event_types", expectedCaepEventTypes
		));

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

		eventLog.runBlock("Subject identifier formats used by the transmitter", () -> {
			// One reviewer-friendly summary of the sub_id formats seen across all received SETs
			// (recorded per SET by OIDSSFRecordSecurityEventTokenSubjectFormat).
			callAndContinueOnFailure(OIDSSFLogObservedSubjectFormats.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.5");
		});

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
		//
		// Per SSF 1.0 §8.1.4 a transmitter MAY deliver unsolicited verification
		// events (no 'state') at any time, and 8.1.4.2 says receivers MUST NOT
		// depend on the verification event being delivered in any particular
		// order relative to other queued events — CAEP events may therefore
		// arrive before the solicited verification echo. We loop tolerating
		// both unsolicited verification events and pre-echo CAEP events, and
		// stop when we see the solicited verification event whose 'state' echoes
		// the one we sent (8.1.4.2). CAEP events received in this window are
		// preserved into receivedEventTypes so the second loop does not have
		// to wait for them to be redelivered.
		int unsolicitedSeen = 0;
		boolean solicitedVerificationReceived = false;
		Set<String> receivedEventTypes = new LinkedHashSet<>();

		while (unsolicitedSeen < MAX_UNSOLICITED_VERIFICATION_EVENTS && !solicitedVerificationReceived) {
			waitForNextPushRequest();
			callAndStopOnFailure(OIDSSFExtractVerificationEventFromPushRequest.class, "OIDSSF-8.1.4.1");
			callAndStopOnFailure(OIDSSFParseSecurityEventToken.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");

			if (!currentEventIsVerificationEvent()) {
				// Pre-echo CAEP event — preserve for later validation rather than discard.
				processPushedCaepEvent(receivedEventTypes);
				continue;
			}

			AtomicBoolean wasSolicited = new AtomicBoolean(false);
			String blockTitle = unsolicitedSeen == 0
				? "Validate verification event via PUSH delivery"
				: "Validate verification event via PUSH delivery (after " + unsolicitedSeen + " transmitter-initiated)";
			eventLog.runBlock(blockTitle, () -> {
				validateSetCommonAfterParsing();
				callAndContinueOnFailure(OIDSSFCheckVerificationEventSubjectId.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");

				callAndContinueOnFailure(OIDSSFEnsureUnsolicitedVerificationEventHasNoState.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.2");

				if (currentVerificationEventHasState()) {
					callAndContinueOnFailure(OIDSSFCheckVerificationEventState.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
					wasSolicited.set(true);
				} else {
					callAndContinueOnFailure(OIDSSFLogAcceptedUnsolicitedVerificationEvent.class, Condition.ConditionResult.INFO, "OIDSSF-8.1.4");
				}
			});

			if (wasSolicited.get()) {
				solicitedVerificationReceived = true;
			} else {
				unsolicitedSeen++;
			}
		}

		if (!solicitedVerificationReceived) {
			throw new TestFailureException(getId(),
				"Received " + unsolicitedSeen + " transmitter-initiated verification events without a solicited one — "
					+ "transmitter never echoed the state from the verification request (SSF 1.0 §8.1.4.2)");
		}

		// Keep receiving push events until all expected CAEP event types arrive, or timeout.
		// Non-CAEP events (e.g. further verification events) are skipped without counting.
		while (!receivedEventTypes.containsAll(expectedCaepEventTypes)) {
			waitForNextPushRequest();
			callAndStopOnFailure(OIDSSFExtractVerificationEventFromPushRequest.class, "OIDSSF-8.1.4.1");
			callAndStopOnFailure(OIDSSFParseSecurityEventToken.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			processPushedCaepEvent(receivedEventTypes);
		}

		eventLog.runBlock("Verify all expected CAEP Interop events received", () -> {
			callAndContinueOnFailure(new OIDSSFEnsureAllCaepInteropEventsReceived(expectedCaepEventTypes, receivedEventTypes),
				Condition.ConditionResult.FAILURE, "CAEPIOP-3");
		});
	}

	/**
	 * Validates an already-parsed pushed SET as a CAEP event. If the SET is not
	 * a CAEP event (e.g. it is a verification event), this method is a no-op —
	 * callers branch on event type separately. On a CAEP event the type is
	 * recorded in {@code receivedEventTypes} and the event-type-specific checks
	 * are run. Assumes {@link OIDSSFParseSecurityEventToken} has already
	 * populated {@code set_token} / {@code ssf.verification.token}.
	 */
	protected void processPushedCaepEvent(Set<String> receivedEventTypes) {
		// Event types outside the CAEP/SSF families (e.g. RISC or proprietary) are not
		// forbidden stream content (SSF 1.0 8.1.1 places no such restriction), so an
		// unrecognised type is a WARNING, not a FAILURE.
		callAndContinueOnFailure(OIDSSFExtractCaepEventData.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1");
		String eventType = env.getString("ssf", "caep_event.type");
		if (eventType == null) {
			validateNonCaepSet();
			return;
		}
		String eventName = eventType.substring(eventType.lastIndexOf('/') + 1);

		eventLog.runBlock("Validate CAEP event: " + eventName, () -> {
			validateSetCommonAfterParsing();

			receivedEventTypes.add(eventType);
			validateCaepEventSubject();
			callAndContinueOnFailure(OIDSSFValidateCaepCommonOptionalFields.class, Condition.ConditionResult.FAILURE, "OIDCAEP-2");
			validateCaepEventFields(eventType);
		});
	}

	/**
	 * jtis of the SETs without a CAEP event whose envelope was already validated - the
	 * verification events graded during the verification phase and the non-CAEP SETs seen in
	 * earlier poll responses - so re-delivered or restored SETs are not graded twice.
	 */
	private final Set<String> validatedNonCaepJtis = new LinkedHashSet<>();

	/**
	 * Validates a SET that carries no CAEP event: CAEPIOP 2.6 "All events MUST be signed" and
	 * the SET envelope rules apply to every SET on the stream. A further verification event is
	 * checked like the solicited one (subject, and the echoed state when present - SSF 1.0
	 * 8.1.4.2); a stream-updated event (SSF 1.0 8.1.5) is checked for its status and subject.
	 * Assumes {@link OIDSSFParseSecurityEventToken} has already populated {@code set_token}.
	 */
	protected void validateNonCaepSet() {
		JsonElement eventsEl = env.getElementFromObject("set_token", "claims.events");
		Set<String> eventTypes = eventsEl != null && eventsEl.isJsonObject() ? eventsEl.getAsJsonObject().keySet() : Set.of();
		String blockTitle = currentEventIsVerificationEvent() ? "Validate verification event"
			: eventTypes.contains(SSF_STREAM_UPDATED_EVENT_TYPE) ? "Validate stream-updated event"
			: "Validate SET envelope of non-CAEP event";
		eventLog.runBlock(blockTitle, () -> {
			validateSetCommonAfterParsing();
			if (currentEventIsVerificationEvent()) {
				callAndContinueOnFailure(OIDSSFCheckVerificationEventSubjectId.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
				if (currentVerificationEventHasState()) {
					callAndContinueOnFailure(OIDSSFCheckVerificationEventState.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
				} else {
					callAndContinueOnFailure(OIDSSFLogAcceptedUnsolicitedVerificationEvent.class, Condition.ConditionResult.INFO, "OIDSSF-8.1.4");
				}
			} else if (eventTypes.contains(SSF_STREAM_UPDATED_EVENT_TYPE)) {
				callAndContinueOnFailure(OIDSSFValidateStreamUpdatedEvent.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5");
				callAndContinueOnFailure(OIDSSFWarnStreamUpdatedEventUnknownMembers.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.5");
			} else {
				eventLog.log(getName(), args("msg", "SET carries an event type this test does not inspect beyond the SET envelope", "event_types", eventTypes));
			}
		});
	}

	/**
	 * Validates the {@code sub_id} of the current CAEP event against CAEP Interop Profile §2.5:
	 * CAEP events must use the {@code email} or {@code iss_sub} format ({@code opaque} is reserved
	 * for the Verification event). Complex Subjects (SSF 1.0 §3.3) are accepted and structurally
	 * validated, but reported as a WARNING since section 2.5 does not list them yet — the working
	 * group is expected to permit them (openid/sharedsignals#351); CAEP's own
	 * device-compliance-change example uses one.
	 */
	protected void validateCaepEventSubject() {
		callAndContinueOnFailure(OIDSSFEnsureCaepInteropEventSubjectFormat.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.5");
		callAndContinueOnFailure(OIDSSFWarnCaepInteropEventUsesComplexSubject.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.5");
	}

	protected void retrieveAndAcknowledgeEventsViaPoll() {
		// Poll repeatedly for the solicited verification event. SSF 1.0 8.1.4.2:
		// receivers MUST NOT depend on the verification event being transmitted
		// synchronously or in any particular order relative to the queue - a single
		// poll right after triggering is not sufficient. The ~60s window follows the
		// WG expectation recorded on sharedsignals#339.
		boolean solicitedVerificationFound = false;
		// SETs retrieved across all verification poll attempts: each poll response overwrites
		// ssf.poll.sets, and RFC 8936 §2.4 makes re-delivery of un-acknowledged SETs OPTIONAL,
		// so CAEP events retrieved in an earlier attempt must be kept here or they are lost.
		JsonObject pendingSets = new JsonObject();
		for (int attempt = 1; attempt <= VERIFICATION_POLL_MAX_ATTEMPTS && !solicitedVerificationFound; attempt++) {
			int attemptNr = attempt;
			eventLog.runBlock("Poll for verification event (attempt " + attemptNr + ")", () -> {
				env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_ONLY.name());
				callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-6.1.2", "RFC8936-2.4");
				env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
				validatePollResponse();
				callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);
			});

			JsonElement batchEl = env.getElementFromObject("ssf", "poll.sets");
			if (batchEl != null && batchEl.isJsonObject()) {
				for (var setEntry : batchEl.getAsJsonObject().entrySet()) {
					pendingSets.add(setEntry.getKey(), setEntry.getValue());
				}
			}

			solicitedVerificationFound = iterateAndValidatePolledVerificationEvents();

			if (!solicitedVerificationFound && morePollEventsAvailable()) {
				// RFC 8936 2.3: the transmitter holds further unacknowledged SETs - fetch them now
				eventLog.log(getName(), "Poll response announced more SETs (moreAvailable); polling again immediately (attempt " + attemptNr + "/" + VERIFICATION_POLL_MAX_ATTEMPTS + ")");
				continue;
			}

			if (!solicitedVerificationFound && attempt < VERIFICATION_POLL_MAX_ATTEMPTS) {
				eventLog.log(getName(), "Solicited verification event not yet delivered; polling again in "
					+ VERIFICATION_POLL_INTERVAL_SECONDS + "s (attempt " + attemptNr + "/" + VERIFICATION_POLL_MAX_ATTEMPTS + ")");
				try {
					Thread.sleep(VERIFICATION_POLL_INTERVAL_SECONDS * 1000L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new TestFailureException(getId(), "Interrupted while waiting for the solicited verification event");
				}
			}
		}

		if (!solicitedVerificationFound) {
			throw new TestFailureException(getId(),
				"POLL responses did not contain a solicited verification event (with matching 'state') within the polling window - "
					+ "transmitter did not echo the state from the verification request (SSF 1.0 8.1.4.2)");
		}

		Set<String> receivedEventTypes = new LinkedHashSet<>();

		// Per SSF 1.0 8.1.4.2 receivers MUST NOT depend on the verification
		// event being delivered in any particular order relative to other
		// queued events. Process the CAEP events retrieved across ALL of the
		// verification poll attempts (not just the final response), and restore
		// them to poll.sets so the first POLL_AND_ACKNOWLEDGE below acknowledges
		// every retrieved SET — RFC 8936 §2.4 makes re-delivery of
		// un-acknowledged SETs OPTIONAL, so nothing retrieved may be dropped.
		if (!pendingSets.isEmpty()) {
			env.putObject("ssf", "poll.sets", pendingSets.deepCopy());
			processCaepEventsFromPollResponse(pendingSets, receivedEventTypes);
		}

		// Poll repeatedly until all 3 CAEP event types are received, or timeout.
		// Each poll acknowledges previously received SETs and retrieves new ones.
		int pollIntervalSeconds = 5;
		int maxAttempts = 12; // 12 x 5s = 60 seconds
		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			eventLog.runBlock("Poll for CAEP events (attempt " + (attempt + 1) + ")", () -> {
				env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_AND_ACKNOWLEDGE.name());
				callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-6.1.2", "RFC8936-2.4");
				env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
				validatePollResponse();
				callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);
			});

			JsonElement pollSetsEl = env.getElementFromObject("ssf", "poll.sets");
			if (pollSetsEl != null && pollSetsEl.isJsonObject() && !pollSetsEl.getAsJsonObject().isEmpty()) {
				processCaepEventsFromPollResponse(pollSetsEl.getAsJsonObject(), receivedEventTypes);
			}

			if (receivedEventTypes.containsAll(expectedCaepEventTypes)) {
				break;
			}

			if (morePollEventsAvailable()) {
				eventLog.log(getName(), "Poll response announced more SETs (moreAvailable); polling again immediately");
				continue;
			}

			if (attempt < maxAttempts - 1) {
				eventLog.log(getName(), "Waiting for CAEP events... received "
					+ receivedEventTypes.size() + "/" + expectedCaepEventTypes.size() + ", polling again in " + pollIntervalSeconds + "s");
				try {
					Thread.sleep(pollIntervalSeconds * 1000L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new TestFailureException(getId(), "Interrupted while waiting for CAEP events");
				}
			}
		}

		eventLog.runBlock("Verify all expected CAEP Interop events were received", () -> {
			callAndContinueOnFailure(new OIDSSFEnsureAllCaepInteropEventsReceived(expectedCaepEventTypes, receivedEventTypes),
				Condition.ConditionResult.FAILURE, "CAEPIOP-3");
		});

		// Final acknowledgment for any remaining unacked SETs
		eventLog.runBlock("Acknowledge remaining events", () -> {
			JsonElement remainingSets = env.getElementFromObject("ssf", "poll.sets");
			if (remainingSets != null && remainingSets.isJsonObject() && !remainingSets.getAsJsonObject().isEmpty()) {
				env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.ACKNOWLEDGE_ONLY.name());
				callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-6.1.2", "RFC8936-2.4");
				env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
				validatePollResponse();
			}
		});
	}

	/**
	 * Iterates every SET in the current {@code ssf.poll.sets} object. For each
	 * verification-type SET, runs the CAEP-profile common validation and the
	 * subject-id check. If the verification event carries a {@code state} it
	 * MUST match {@code ssf.verification.state}, and iteration stops; if it
	 * carries no state it is accepted as transmitter-initiated (unsolicited)
	 * per SSF 1.0 §8.1.4.2 and iteration continues. Non-verification SETs are
	 * logged and skipped (they'll be processed by {@link #processCaepEventsFromPollResponse}
	 * in later iterations).
	 *
	 * @return {@code true} if a solicited (stated) verification event was found
	 *         and fully validated; {@code false} otherwise.
	 */
	protected boolean iterateAndValidatePolledVerificationEvents() {
		JsonElement pollSetsEl = env.getElementFromObject("ssf", "poll.sets");
		if (pollSetsEl == null || !pollSetsEl.isJsonObject()) {
			return false;
		}
		JsonObject pollSets = pollSetsEl.getAsJsonObject();
		if (pollSets.isEmpty()) {
			return false;
		}

		int setIndex = 0;
		int totalSets = pollSets.size();
		for (Map.Entry<String, JsonElement> entry : pollSets.entrySet()) {
			setIndex++;
			String jti = entry.getKey();
			String setJwt = OIDFJSON.getString(entry.getValue());
			env.putString("ssf", "verification.jwt", setJwt);

			AtomicBoolean wasSolicited = new AtomicBoolean(false);
			int idx = setIndex;
			eventLog.runBlock("Validate polled SET " + idx + "/" + totalSets + " (jti=" + jti + ")", () -> {
				validateSetCommon();

				if (!currentEventIsVerificationEvent()) {
					// CAEP events retrieved during the verification phase are validated by
					// processCaepEventsFromPollResponse once the verification event has arrived
					eventLog.log(getName(),
						args("msg", "Skipping non-verification SET during verification phase",
							"jti", jti));
					return;
				}
				validatedNonCaepJtis.add(jti);

				callAndContinueOnFailure(OIDSSFCheckVerificationEventSubjectId.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");

				if (currentVerificationEventHasState()) {
					callAndContinueOnFailure(OIDSSFCheckVerificationEventState.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
					wasSolicited.set(true);
				} else {
					callAndContinueOnFailure(OIDSSFLogAcceptedUnsolicitedVerificationEvent.class, Condition.ConditionResult.INFO, "OIDSSF-8.1.4");
				}
			});

			if (wasSolicited.get()) {
				return true;
			}
		}
		return false;
	}

	protected void processCaepEventsFromPollResponse(JsonObject pollSets, Set<String> receivedEventTypes) {
		for (Map.Entry<String, JsonElement> entry : pollSets.entrySet()) {
			String setToken = OIDFJSON.getString(entry.getValue());
			env.putString("ssf", "verification.jwt", setToken);

			callAndStopOnFailure(OIDSSFParseSecurityEventToken.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			// Event types outside the CAEP/SSF families (e.g. RISC or proprietary) are not
			// forbidden stream content (SSF 1.0 8.1.1 places no such restriction), so an
			// unrecognised type is a WARNING, not a FAILURE.
			callAndContinueOnFailure(OIDSSFExtractCaepEventData.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1");
			String eventType = env.getString("ssf", "caep_event.type");
			if (eventType == null) {
				if (validatedNonCaepJtis.add(entry.getKey())) {
					validateNonCaepSet();
				}
				continue;
			}
			String eventName = eventType.substring(eventType.lastIndexOf('/') + 1);

			eventLog.runBlock("Validate CAEP event: " + eventName, () -> {
				validateSetCommonAfterParsing();

				receivedEventTypes.add(eventType);
				validateCaepEventSubject();
				callAndContinueOnFailure(OIDSSFValidateCaepCommonOptionalFields.class, Condition.ConditionResult.FAILURE, "OIDCAEP-2");
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
		callAndStopOnFailure(OIDSSFParseSecurityEventToken.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
		validateSetCommonAfterParsing();
	}

	/**
	 * Validates common SET properties assuming the token has already been parsed
	 * (i.e. {@code set_token} is already in the environment).
	 */
	protected void validateSetCommonAfterParsing() {
		// CAEPIOP 2.6: "All events MUST be signed using the RS256 algorithm with a
		// minimum of 2048-bit keys" - a forged signature or a weak key must FAIL.
		callAndContinueOnFailure(OIDSSFVerifySignatureOfSecurityEventToken.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.6");
		callAndContinueOnFailure(OIDSSFEnsureEventSignedWithRsa256.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.6");
		callAndContinueOnFailure(OIDSSFEnsureEventSignerRsaKeySizeAtLeast2048Bits.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.6");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.1");
		callAndContinueOnFailure(OIDSSFWarnSecurityEventTokenTypeNotInPreferredForm.class, Condition.ConditionResult.WARNING, "RFC8417-2.3");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenContainsSingleEvent.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.8.1");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenDoesNotContainSubClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.2");
		callAndContinueOnFailure(OIDSSFValidateSecurityEventTokenSubIdClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-3.1", "RFC9493-3");
		// RFC 9493 §3 prohibits members not described by the format; per the suite's
		// unknown-property convention this is surfaced as a warning (typo-catcher).
		callAndContinueOnFailure(OIDSSFWarnSecurityEventTokenSubIdUnknownMembers.class, Condition.ConditionResult.WARNING, "RFC9493-3");
		callAndContinueOnFailure(OIDSSFRecordSecurityEventTokenSubjectFormat.class, Condition.ConditionResult.INFO, "CAEPIOP-2.5");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenDoesNotContainExpClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.7");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenIssuerMatchesStreamConfigurationIssuer.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.6");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenIatIsNotInFuture.class, Condition.ConditionResult.FAILURE, "RFC8417-2.2", "RFC7519-4.1.6");
		callAndContinueOnFailure(OIDSSFValidateSecurityEventTokenJtiClaim.class, Condition.ConditionResult.FAILURE, "RFC8417-2.2");
		callAndContinueOnFailure(OIDSSFValidateSecurityEventTokenAudClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.8");
		callAndContinueOnFailure(OIDSSFEnsureEventContainsStreamAudience.class, Condition.ConditionResult.WARNING, "OIDSSF-4.1.8", "OIDSSF-8.1.1");
		// SSF 1.0 4.1.9: "Transmitters SHOULD set the txn claim"
		callAndContinueOnFailure(OIDSSFValidateSecurityEventTokenTxnClaim.class, Condition.ConditionResult.WARNING, "OIDSSF-4.1.9");
	}

	/**
	 * Validates event-type-specific required fields for a CAEP event.
	 */
	protected void validateCaepEventFields(String eventType) {
		switch (eventType) {
			case SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE:
				// CAEP 1.0 3.1 defines no required event-specific fields, but the CAEP
				// Interop Profile (3.1) requires reason_admin to be a non-empty object.
				callAndContinueOnFailure(OIDSSFEnsureCaepInteropEventReasonAdminPresent.class,
					Condition.ConditionResult.FAILURE, "CAEPIOP-3.1");
				break;
			case SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE:
				callAndContinueOnFailure(OIDSSFValidateCaepCredentialChangeEvent.class,
					Condition.ConditionResult.FAILURE, "OIDCAEP-3.3");
				callAndContinueOnFailure(OIDSSFWarnNonStandardCaepCredentialChangeValues.class,
					Condition.ConditionResult.WARNING, "OIDCAEP-3.3");
				callAndContinueOnFailure(OIDSSFEnsureCaepInteropEventReasonAdminPresent.class,
					Condition.ConditionResult.FAILURE, "CAEPIOP-3.2");
				break;
			case SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE:
				callAndContinueOnFailure(OIDSSFValidateCaepDeviceComplianceChangeEvent.class,
					Condition.ConditionResult.FAILURE, "OIDCAEP-3.5");
				callAndContinueOnFailure(OIDSSFEnsureCaepInteropEventReasonAdminPresent.class,
					Condition.ConditionResult.FAILURE, "CAEPIOP-3.3");
				break;
			case SsfEvents.CAEP_RISK_LEVEL_CHANGE_EVENT_TYPE:
				// Structural requirements come from CAEP 1.0 Final (3.8) and apply to any
				// delivered risk-level-change event. The reason_admin requirement only
				// exists in the WG head's section 3.4 - the published draft-01 of the
				// interop profile, which the CAEPIOP- links point at, does not define this
				// use case - so it is graded as a WARNING anchored at the CAEP definition
				// until the profile vote lands (see SsfEvents.CAEP_INTEROP_EVENT_TYPES).
				callAndContinueOnFailure(OIDSSFValidateCaepRiskLevelChangeEvent.class,
					Condition.ConditionResult.FAILURE, "OIDCAEP-3.8");
				callAndContinueOnFailure(OIDSSFEnsureCaepInteropEventReasonAdminPresent.class,
					Condition.ConditionResult.WARNING, "OIDCAEP-3.8");
				break;
			case SsfEvents.CAEP_TOKEN_CLAIMS_CHANGE_EVENT_TYPE:
				callAndContinueOnFailure(OIDSSFValidateCaepTokenClaimsChangeEvent.class,
					Condition.ConditionResult.FAILURE, "OIDCAEP-3.2");
				break;
			case SsfEvents.CAEP_ASSURANCE_LEVEL_CHANGE_EVENT_TYPE:
				callAndContinueOnFailure(OIDSSFValidateCaepAssuranceLevelChangeEvent.class,
					Condition.ConditionResult.FAILURE, "OIDCAEP-3.4");
				callAndContinueOnFailure(OIDSSFWarnCaepAssuranceLevelChangeDirectionMissing.class,
					Condition.ConditionResult.WARNING, "OIDCAEP-3.4");
				break;
			case SsfEvents.CAEP_SESSION_ESTABLISHED_EVENT_TYPE:
				callAndContinueOnFailure(OIDSSFValidateCaepSessionEstablishedEvent.class,
					Condition.ConditionResult.FAILURE, "OIDCAEP-3.6");
				break;
			case SsfEvents.CAEP_SESSION_PRESENTED_EVENT_TYPE:
				callAndContinueOnFailure(OIDSSFValidateCaepSessionPresentedEvent.class,
					Condition.ConditionResult.FAILURE, "OIDCAEP-3.7");
				break;
			default:
				eventLog.log(getName(), "Received CAEP event type: " + eventType);
				break;
		}
	}

	/**
	 * Waits for the next push request to arrive. Uses a generous timeout (up to 60 seconds)
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

	/**
	 * Extracts the CAEP event types from the stream's {@code events_delivered} field.
	 * The transmitter confirms which events it will actually deliver in the stream
	 * creation response. Per CAEPIOP Section 3, implementations MAY support one or
	 * more use cases — we only expect the events the transmitter confirmed.
	 */
	protected Set<String> extractCaepEventsDelivered() {
		JsonElement eventsDeliveredEl = env.getElementFromObject("ssf", "stream.events_delivered");
		if (eventsDeliveredEl == null || !eventsDeliveredEl.isJsonArray()) {
			throw new TestFailureException(getId(),
				"Stream configuration response does not contain a valid events_delivered field, "
					+ "which is required per OIDSSF-8.1.1.");
		}

		List<String> eventsDelivered = OIDFJSON.convertJsonArrayToList(eventsDeliveredEl.getAsJsonArray());
		// Only the qualifying use cases of the published CAEP Interop Profile draft-01
		// (3.1-3.3) are REQUIRED to be demonstrated during the run. Other advertised CAEP
		// event types (e.g. risk-level-change, which only the WG head defines as a use case)
		// are still validated and acknowledged if the transmitter delivers them, but a
		// transmitter must not fail merely because it never triggered one.
		Set<String> caepEvents = new LinkedHashSet<>();
		Set<String> nonQualifyingCaepEvents = new LinkedHashSet<>();
		for (String eventType : eventsDelivered) {
			if (SsfEvents.CAEP_INTEROP_QUALIFYING_EVENT_TYPES.contains(eventType)) {
				caepEvents.add(eventType);
			} else if (SsfEvents.CAEP_EVENT_TYPES.contains(eventType)) {
				nonQualifyingCaepEvents.add(eventType);
			}
		}

		if (caepEvents.isEmpty()) {
			throw new TestFailureException(getId(),
				"Stream events_delivered does not contain any CAEP Interop qualifying event type. "
					+ "The transmitter must support at least one of the use cases 'session-revoked', "
					+ "'credential-change' or 'device-compliance-change' (CAEPIOP-3.1, 3.2, 3.3).");
		}

		eventLog.log(getName(),
			 args("msg", "Transmitter must deliver " + caepEvents.size() + " qualifying CAEP event type(s) during this run"
					 + (nonQualifyingCaepEvents.isEmpty() ? "" : "; other advertised CAEP event types are validated if delivered but not required"),
				 "required_caep_events", caepEvents,
				 "optional_caep_events", nonQualifyingCaepEvents));

		return caepEvents;
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
