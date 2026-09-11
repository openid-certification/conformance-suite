package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.WaitFor5Seconds;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCallPollEndpoint;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationAuthorizationHeader;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationEventState;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationEventSubjectId;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureEventContainsStreamAudience;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureEventSignedWithRsa256;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureEventSignerRsaKeySizeAtLeast2048Bits;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenContainsSingleEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventValuesAreJsonObjects;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenDoesNotContainExpClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenDoesNotContainSubClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenIatIsNotInFuture;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenIssuerMatchesStreamConfigurationIssuer;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnSecurityEventTokenTypeNotInPreferredForm;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractReceivedSETs;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFLogAcceptedUnsolicitedVerificationEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFParseSecurityEventToken;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateSecurityEventTokenAudClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnSecurityEventTokenAudClaimMissing;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateSecurityEventTokenJtiClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenTxnClaimIsString;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnSecurityEventTokenTxnClaimMissing;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateStreamUpdatedEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnStreamUpdatedEventUnknownMembers;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFVerifySignatureOfSecurityEventToken;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureStreamDeliveryMatchesRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.openid.ssf.variant.SsfServerMetadata;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.openid.conformance.openid.ssf.SsfEvents.SSF_STREAM_UPDATED_EVENT_TYPE;

/**
 * Base class for SSF transmitter stream verification tests.
 * Handles common setup: metadata fetch, TLS validation, access token, stream creation,
 * verification trigger, and cleanup. Subclasses implement the delivery-mode-specific
 * verification event retrieval.
 */
@VariantParameters({SsfServerMetadata.class, SsfAuthMode.class, SsfDeliveryMode.class,})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "static", configurationFields = {"ssf.transmitter.configuration_metadata_endpoint",})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "discovery", configurationFields = {"ssf.transmitter.issuer", "ssf.transmitter.metadata_suffix",})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "static", configurationFields = {"ssf.transmitter.access_token"})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "dynamic", configurationFields = {})
public abstract class AbstractOIDSSFTransmitterStreamVerificationTest extends AbstractOIDSSFTransmitterTestModule {

	@Override
	public void start() {

		super.start();

		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Transmitter Metadata", () -> {
			fetchTransmitterMetadata();
			callAndStopOnFailure(FetchServerKeys.class);
		});

		String verificationEndpoint = env.getString("ssf", "transmitter_metadata.verification_endpoint");
		if (verificationEndpoint == null) {
			if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
				throw new TestFailureException(getId(), "Transmitter metadata does not include a verification_endpoint, "
					+ "which is required by the CAEP Interop Profile (CAEPIOP-2.3.6).");
			}
			fireTestSkipped("Transmitter metadata does not include a verification_endpoint. "
				+ "The SSF specification defines verification_endpoint as optional (OIDSSF-7.2.3).");
			return;
		}

		eventLog.runBlock("Validate TLS Connection", this::validateTlsConnection);

		eventLog.runBlock("Prepare Transmitter Access", this::obtainTransmitterAccessToken);

		eventLog.runBlock("Clean stream environment if necessary", this::cleanUpStreamConfigurationIfNecessary);

		eventLog.runBlock("Create Stream Configuration", () -> {

			env.putString("ssf", "delivery_method", deliveryMode.getAlias());

			JsonObject deliveryObject = new JsonObject();
			deliveryObject.addProperty("delivery_method", deliveryMode.getAlias());

			if (deliveryMode == SsfDeliveryMode.PUSH) {
				configurePushAuthorizationHeader(deliveryObject, pushAuthorizationHeader);
			}

			env.putObject("ssf", "delivery", deliveryObject);

			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			rememberSentStreamConfig();
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.1");
			// the verification cannot be delivered over a delivery the receiver did not ask for
			callAndStopOnFailure(OIDSSFEnsureStreamDeliveryMatchesRequest.class, "OIDSSF-8.1.1.1", "CAEPIOP-2.3.8.1", "OIDSSF-6.1.2");
			call(exec().unmapKey("endpoint_response"));
		});

		eventLog.runBlock("Wait after stream creation for any transmitter-initiated requests",
			() -> callAndContinueOnFailure(WaitFor5Seconds.class, Condition.ConditionResult.INFO));

		triggerVerificationEvent();

		performVerification();

		fireTestFinished();
	}

	protected void triggerVerificationEvent() {
		eventLog.runBlock("Trigger verification event", () -> {
			triggerVerificationEventAndRequireAcceptance();
			call(exec().unmapKey("endpoint_response"));

			callAndContinueOnFailure(WaitFor5Seconds.class, Condition.ConditionResult.INFO);
		});
	}

	/**
	 * Subclasses implement this to retrieve and verify the verification event
	 * using the appropriate delivery mechanism.
	 */
	protected abstract void performVerification();

	protected void verifySetInResponse() {
		parseVerificationEventInResponse();
		verifyParsedVerificationEventCommon();
		callAndContinueOnFailure(OIDSSFCheckVerificationEventState.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
	}

	/**
	 * Verifies signature and parses the verification event token into the env. Must be
	 * called before any checks that inspect the parsed claims (including
	 * {@link #currentVerificationEventHasState()} and {@link #verifyParsedVerificationEventCommon()}).
	 */
	protected void parseVerificationEventInResponse() {
		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			// CAEPIOP 2.6: "All events MUST be signed ..." - a forged signature must
			// FAIL under the interop profile (the certification target).
			callAndContinueOnFailure(OIDSSFVerifySignatureOfSecurityEventToken.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.6");
		} else {
			callAndContinueOnFailure(OIDSSFVerifySignatureOfSecurityEventToken.class, Condition.ConditionResult.WARNING);
		}
		callAndStopOnFailure(OIDSSFParseSecurityEventToken.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
	}

	/**
	 * Runs every verification-event check except the {@code state} match. This is
	 * shared between solicited events (where the caller additionally runs
	 * {@link OIDSSFCheckVerificationEventState}) and unsolicited events (where the
	 * {@code state} claim is legitimately absent — see SSF 1.0 §8.1.4-2).
	 * {@link #parseVerificationEventInResponse()} must have been called first.
	 */
	protected void verifyParsedVerificationEventCommon() {
		verifyParsedSetEnvelope();

		callAndContinueOnFailure(OIDSSFCheckVerificationEventSubjectId.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");

		if (deliveryMode == SsfDeliveryMode.PUSH) {
			callAndContinueOnFailure(OIDSSFCheckVerificationAuthorizationHeader.class, Condition.ConditionResult.FAILURE, "OIDSSF-6.1.1");
		}
	}

	/**
	 * Validates the SET envelope of the parsed token regardless of its event type: signature
	 * algorithm and key size under the interop profile, typ, single event, no sub/exp,
	 * iss, iat, jti, aud, txn. Every SET a transmitter delivers on the stream is held to this,
	 * including stream-updated events and further verification events that arrive while the
	 * suite waits for the solicited one.
	 */
	protected void verifyParsedSetEnvelope() {
		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			callAndContinueOnFailure(OIDSSFEnsureEventSignedWithRsa256.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.6");
			callAndContinueOnFailure(OIDSSFEnsureEventSignerRsaKeySizeAtLeast2048Bits.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.6");
		}

		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.1");
		callAndContinueOnFailure(OIDSSFWarnSecurityEventTokenTypeNotInPreferredForm.class, Condition.ConditionResult.WARNING, "RFC8417-2.3");
		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenContainsSingleEvent.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.8.1");
		} else {
			callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenContainsSingleEvent.class, Condition.ConditionResult.WARNING, "OIDSSF-4.2.1");
		}
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventValuesAreJsonObjects.class, Condition.ConditionResult.FAILURE, "RFC8417-2.2");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenDoesNotContainSubClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.2");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenDoesNotContainExpClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.7");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenIssuerMatchesStreamConfigurationIssuer.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.6");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenIatIsNotInFuture.class, Condition.ConditionResult.FAILURE, "RFC8417-2.2", "RFC7519-4.1.6");
		callAndContinueOnFailure(OIDSSFValidateSecurityEventTokenJtiClaim.class, Condition.ConditionResult.FAILURE, "RFC8417-2.2");

		callAndContinueOnFailure(OIDSSFWarnSecurityEventTokenAudClaimMissing.class, Condition.ConditionResult.WARNING, "RFC8417-2.2", "OIDSSF-4.1.8");
		callAndContinueOnFailure(OIDSSFValidateSecurityEventTokenAudClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.8");
		callAndContinueOnFailure(OIDSSFEnsureEventContainsStreamAudience.class, Condition.ConditionResult.WARNING, "OIDSSF-4.1.8", "OIDSSF-8.1.1");

		// SSF 1.0 4.1.9: "Transmitters SHOULD set the txn claim"; a present value is a string per RFC 8417 2.2
		callAndContinueOnFailure(OIDSSFWarnSecurityEventTokenTxnClaimMissing.class, Condition.ConditionResult.WARNING, "OIDSSF-4.1.9");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenTxnClaimIsString.class, Condition.ConditionResult.FAILURE, "RFC8417-2.2", "OIDSSF-4.1.9");
	}

	/**
	 * Validates a SET that is not a verification event: the envelope checks apply to every
	 * SET on the stream, and a stream-updated event (SSF 1.0 8.1.5) is additionally checked
	 * for its {@code status} and its opaque {@code sub_id} naming the stream. Other event
	 * types are logged only.
	 */
	protected void verifyParsedNonVerificationSet() {
		verifyParsedSetEnvelope();

		JsonElement eventsEl = env.getElementFromObject("ssf", "verification.token.claims.events");
		if (eventsEl != null && eventsEl.isJsonObject() && eventsEl.getAsJsonObject().has(SSF_STREAM_UPDATED_EVENT_TYPE)) {
			callAndContinueOnFailure(OIDSSFValidateStreamUpdatedEvent.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5");
			callAndContinueOnFailure(OIDSSFWarnStreamUpdatedEventUnknownMembers.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.5");
		} else {
			eventLog.log(getName(), args("msg", "SET carries an event type this test does not inspect beyond the SET envelope",
				"event_types", eventsEl != null && eventsEl.isJsonObject() ? eventsEl.getAsJsonObject().keySet() : null));
		}
	}

	/**
	 * Iterates through every SET returned in the current poll response (at
	 * {@code ssf_polling_response.body_json.sets}, in insertion order), parsing and
	 * validating each as a verification event.
	 * <p>
	 * For each SET:
	 * <ul>
	 *   <li>Non-verification SETs have their SET envelope validated (a stream-updated
	 *       event also its payload) and are then skipped.
	 *   <li>Verification events without a {@code state} claim are accepted as
	 *       unsolicited (SSF 1.0 §8.1.4-2) — common validations run, but the state
	 *       check is skipped and iteration continues.
	 *   <li>Verification events with a {@code state} claim have their state validated
	 *       against the states this test issued. The first one echoing the latest
	 *       request stops the iteration; an echo of an earlier request (a late delivery,
	 *       legitimate per SSF 1.0 8.1.4.2) is accepted and iteration continues.
	 * </ul>
	 *
	 * @param blockPrefix prefix used in the per-SET runBlock titles (e.g. {@code "POLL_ONLY"})
	 * @return {@code true} if a solicited (stated) verification event was found and
	 *         fully validated; {@code false} if every SET in the response was missing,
	 *         non-verification, or stateless unsolicited.
	 */
	protected boolean iterateAndValidateVerificationEventsInPollResponse(String blockPrefix) {
		JsonObject pollResponse = env.getObject("ssf_polling_response");
		if (pollResponse == null) {
			return false;
		}
		JsonObject bodyJson = pollResponse.getAsJsonObject("body_json");
		if (bodyJson == null) {
			return false;
		}
		JsonObject sets = bodyJson.getAsJsonObject("sets");
		if (sets == null || sets.isEmpty()) {
			return false;
		}

		int setIndex = 0;
		int totalSets = sets.size();
		for (Map.Entry<String, JsonElement> entry : sets.entrySet()) {
			setIndex++;
			String jti = entry.getKey();
			String jwt = OIDFJSON.getString(entry.getValue());
			env.putString("ssf", "verification.jwt", jwt);

			AtomicBoolean wasSolicited = new AtomicBoolean(false);
			int idx = setIndex;
			eventLog.runBlock(blockPrefix + " — validate SET " + idx + "/" + totalSets + " (jti=" + jti + ")", () -> {
				parseVerificationEventInResponse();

				if (!currentEventIsVerificationEvent()) {
					verifyParsedNonVerificationSet();
					return;
				}

				verifyParsedVerificationEventCommon();

				if (currentVerificationEventHasState()) {
					callAndContinueOnFailure(OIDSSFCheckVerificationEventState.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
					wasSolicited.set(currentVerificationEventIsForLatestRequest());
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

	protected static final int VERIFICATION_POLL_MAX_ATTEMPTS = 12;

	protected static final int VERIFICATION_POLL_INTERVAL_SECONDS = 5;

	/**
	 * Polls the transmitter until the solicited verification event (carrying the
	 * echoed {@code state}) appears, or ~60 seconds elapse. SSF 1.0 8.1.4.2:
	 * "Event Receivers MUST NOT depend on the Verification Event being transmitted
	 * synchronously or in any particular order relative to the current queue of
	 * events" - so a single poll right after triggering is not sufficient; the
	 * event may only become available after a delay or behind other queued SETs.
	 * The ~60-second window follows the expectation recorded by the WG on
	 * sharedsignals#339.
	 * <p>
	 * The first attempt uses {@code initialMode} (so POLL_AND_ACKNOWLEDGE callers
	 * acknowledge their previous batch exactly once); retries use POLL_ONLY.
	 *
	 * @return {@code true} once a solicited verification event was found and validated
	 */
	protected boolean pollForSolicitedVerificationEvent(String blockPrefix, OIDSSFCallPollEndpoint.PollMode initialMode) {
		for (int attempt = 1; attempt <= VERIFICATION_POLL_MAX_ATTEMPTS; attempt++) {
			OIDSSFCallPollEndpoint.PollMode pollMode = attempt == 1 ? initialMode : OIDSSFCallPollEndpoint.PollMode.POLL_ONLY;
			eventLog.runBlock("Poll for verification events via " + blockPrefix + " (attempt " + attempt + ")", () -> {
				env.putString("ssf", "poll.mode", pollMode.name());
				callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-6.1.2", "RFC8936-2.4");
				env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
				validatePollResponse();
				callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);
			});

			if (iterateAndValidateVerificationEventsInPollResponse(blockPrefix)) {
				return true;
			}

			Integer pollStatus = env.getInteger("resource_endpoint_response_full", "status");
			if (pollStatus != null && pollStatus >= 400 && pollStatus < 500) {
				// the transmitter rejected the poll request itself; repeating it cannot succeed
				eventLog.log(getName(), "The poll endpoint rejected the poll request with HTTP " + pollStatus + "; not polling again");
				return false;
			}

			if (morePollEventsAvailable()) {
				// RFC 8936 2.3: the transmitter holds further unacknowledged SETs - fetch them now
				eventLog.log(getName(), "Poll response announced more SETs (moreAvailable); polling again immediately (attempt " + attempt + "/" + VERIFICATION_POLL_MAX_ATTEMPTS + ")");
				continue;
			}

			if (attempt < VERIFICATION_POLL_MAX_ATTEMPTS) {
				eventLog.log(getName(), "Solicited verification event not yet delivered; polling again in "
					+ VERIFICATION_POLL_INTERVAL_SECONDS + "s (attempt " + attempt + "/" + VERIFICATION_POLL_MAX_ATTEMPTS + ")");
				try {
					Thread.sleep(VERIFICATION_POLL_INTERVAL_SECONDS * 1000L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new TestFailureException(getId(), "Interrupted while waiting for the solicited verification event");
				}
			}
		}
		return false;
	}

	@Override
	public void cleanup() {
		eventLog.runBlock("Cleanup", () -> {
			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO);
			super.cleanup();
		});
	}
}
