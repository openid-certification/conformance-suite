package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.WaitFor5Seconds;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationAuthorizationHeader;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationEventState;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationEventSubjectId;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureEventContainsStreamAudience;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureEventSignedWithRsa256;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenContainsSingleEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenDoesNotContainExpClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenDoesNotContainSubClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenIatIsNotInFuture;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenIssuerMatchesStreamConfigurationIssuer;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFLogAcceptedUnsolicitedVerificationEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFParseVerificationEventToken;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFTriggerVerificationEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateSecurityEventTokenAudClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateSecurityEventTokenTxnClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFVerifySignatureOfVerificationEventToken;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWaitForMinVerificationInterval;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
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
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
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
			callAndContinueOnFailure(OIDSSFWaitForMinVerificationInterval.class, Condition.ConditionResult.INFO, "OIDSSF-8.1.4.1");
			callAndStopOnFailure(OIDSSFTriggerVerificationEvent.class, "OIDSSF-8.1.4.2", "CAEPIOP-2.3.8.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-8.1.4.2");

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
		callAndContinueOnFailure(OIDSSFVerifySignatureOfVerificationEventToken.class, Condition.ConditionResult.WARNING);
		callAndStopOnFailure(OIDSSFParseVerificationEventToken.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
	}

	/**
	 * Runs every verification-event check except the {@code state} match. This is
	 * shared between solicited events (where the caller additionally runs
	 * {@link OIDSSFCheckVerificationEventState}) and unsolicited events (where the
	 * {@code state} claim is legitimately absent — see SSF 1.0 §8.1.4-2).
	 * {@link #parseVerificationEventInResponse()} must have been called first.
	 */
	protected void verifyParsedVerificationEventCommon() {
		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			callAndContinueOnFailure(OIDSSFEnsureEventSignedWithRsa256.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.6");
		}

		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.1");
		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenContainsSingleEvent.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.8.1");
		} else {
			callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenContainsSingleEvent.class, Condition.ConditionResult.WARNING, "OIDSSF-4.2.1");
		}
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenDoesNotContainSubClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.2");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenDoesNotContainExpClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.7");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenIssuerMatchesStreamConfigurationIssuer.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.6");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenIatIsNotInFuture.class, Condition.ConditionResult.FAILURE, "RFC8417-2.2", "RFC7519-4.1.6");

		callAndContinueOnFailure(OIDSSFValidateSecurityEventTokenAudClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.8");
		callAndContinueOnFailure(OIDSSFEnsureEventContainsStreamAudience.class, Condition.ConditionResult.WARNING, "RFC7519-4.1.3");

		callAndContinueOnFailure(OIDSSFValidateSecurityEventTokenTxnClaim.class, Condition.ConditionResult.INFO, "OIDSSF-4.1.9");

		callAndContinueOnFailure(OIDSSFCheckVerificationEventSubjectId.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");

		if (deliveryMode == SsfDeliveryMode.PUSH) {
			callAndContinueOnFailure(OIDSSFCheckVerificationAuthorizationHeader.class, Condition.ConditionResult.FAILURE, "OIDSSF-6.1.1");
		}
	}

	/**
	 * Iterates through every SET returned in the current poll response (at
	 * {@code ssf_polling_response.body_json.sets}, in insertion order), parsing and
	 * validating each as a verification event.
	 * <p>
	 * For each SET:
	 * <ul>
	 *   <li>Non-verification SETs are logged and skipped.
	 *   <li>Verification events without a {@code state} claim are accepted as
	 *       unsolicited (SSF 1.0 §8.1.4-2) — common validations run, but the state
	 *       check is skipped and iteration continues.
	 *   <li>The first verification event with a {@code state} claim has its state
	 *       validated against {@code ssf.verification.state}, and iteration stops.
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
					eventLog.log(getName(),
						args("msg", "Skipping non-verification SET in poll response",
							"jti", jti));
					return;
				}

				verifyParsedVerificationEventCommon();

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

	@Override
	public void cleanup() {
		eventLog.runBlock("Cleanup", () -> {
			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO);
			super.cleanup();
		});
	}
}
