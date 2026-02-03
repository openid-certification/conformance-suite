package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.WaitFor5Seconds;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCallPollEndpoint;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationAuthorizationHeader;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationEventState;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationEventSubjectId;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureEventContainsStreamAudience;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureEventSignedWithRsa256;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenDoesNotContainExpClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenDoesNotContainSubClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenIatIsNotInFuture;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenIssuerMatchesStreamConfigurationIssuer;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractReceivedSETs;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractVerificationEventFromPushRequest;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractVerificationEventFromReceivedSETs;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFGetOrWaitForPushRequest;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFParseVerificationEventToken;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFTriggerVerificationEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateSecurityEventTokenAudClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidateSecurityEventTokenTxnClaim;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFVerifySignatureOfVerificationEventToken;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.openid.ssf.variant.SsfServerMetadata;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "openid-ssf-transmitter-stream-verification-events",
	displayName = "OpenID Shared Signals Framework: Stream Verification",
	summary = """
		This test verifies the structure and handling of transmitter events via the configured SET delivery mechanism.
		If PUSH delivery is configured, the test triggers a single verification event and awaits a SET delivered to the exposed push endpoint.
		If POLL delivery is configured, the test attempt to trigger three verification events and sends consecutive POLL requests to obtain the verification events via POLL_ONLY, ACKNOWLEDGE_ONLY, POLL_AND_ACKNOWLEDGE.
		The test succeeds of the verification event(s) can be successfully received/retrieved.""",
	profile = "OIDSSF"
)
@VariantParameters({SsfServerMetadata.class, SsfAuthMode.class, SsfDeliveryMode.class,})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "static", configurationFields = {"ssf.transmitter.configuration_metadata_endpoint",})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "discovery", configurationFields = {"ssf.transmitter.issuer", "ssf.transmitter.metadata_suffix",})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "static", configurationFields = {"ssf.transmitter.access_token"})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "dynamic", configurationFields = {})
public class OIDSSFTransmitterStreamVerificationEventsTest extends AbstractOIDSSFTransmitterTestModule {

	@Override
	public void start() {

		super.start();

		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Transmitter Metadata", () -> {
			fetchTransmitterMetadata();

			callAndStopOnFailure(FetchServerKeys.class);
		});

		eventLog.runBlock("Validate TLS Connection", () -> {
			validateTlsConnection();
		});

		eventLog.runBlock("Prepare Transmitter Access", () -> {
			obtainTransmitterAccessToken();
		});

		eventLog.runBlock("Clean stream environment if necessary", () -> {
			cleanUpStreamConfigurationIfNecessary();
		});

		SsfDeliveryMode deliveryMode = getVariant(SsfDeliveryMode.class);
		// ensure stream exists

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

		switch (deliveryMode) {
			case PUSH: {
				eventLog.runBlock("Trigger verification event", () -> {

					// Trigger verification event
					callAndStopOnFailure(OIDSSFTriggerVerificationEvent.class, "OIDSSF-8.1.4.2", "CAEPIOP-2.3.8.2");
					call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
					callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-8.1.4.2");

					callAndContinueOnFailure(WaitFor5Seconds.class, Condition.ConditionResult.INFO);
				});

				eventLog.runBlock("Verify verification event received via PUSH delivery mode", () -> {

					lookupNextPushRequest();

					// use current push request or wait for it
					callAndStopOnFailure(OIDSSFGetOrWaitForPushRequest.class, "OIDSSF-8.1.4.1");

					// wait for data received on dynamic endpoint (needs to be reachable externally!)
					callAndStopOnFailure(OIDSSFExtractVerificationEventFromPushRequest.class, "OIDSSF-8.1.4.1");

					verifySetInResponse();
				});
			}
			break;
			case POLL: {
				// we need to test 3 poll variants: POLL_ONLY, ACKNOWLEDGE_ONLY, POLL_AND_ACKNOWLEDGE

				eventLog.runBlock("Trigger verification event 1", () -> {

					// Trigger verification event
					callAndStopOnFailure(OIDSSFTriggerVerificationEvent.class, "OIDSSF-8.1.4.2", "CAEPIOP-2.3.8.2");
					call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
					callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-8.1.4.2");

					callAndContinueOnFailure(WaitFor5Seconds.class, Condition.ConditionResult.INFO);
				});

				eventLog.runBlock("Verify verification event 1 received via POLL delivery mode with POLL_ONLY", () -> {

					// poll verification endpoint with POLL_ONLY
					env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_ONLY.name());
					callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-8.1.4.1", "RFC8936-2.4");
					env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
					callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);
					callAndStopOnFailure(OIDSSFExtractVerificationEventFromReceivedSETs.class);

					verifySetInResponse();

					callAndContinueOnFailure(WaitFor5Seconds.class, Condition.ConditionResult.INFO);
				});

				eventLog.runBlock("Trigger verification event 2", () -> {
					// Trigger verification event
					callAndStopOnFailure(OIDSSFTriggerVerificationEvent.class, "OIDSSF-8.1.4.2", "CAEPIOP-2.3.8.2");
					call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
					callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-8.1.4.2");

					callAndContinueOnFailure(WaitFor5Seconds.class, Condition.ConditionResult.INFO);
				});

				eventLog.runBlock("Verify verification event 2 received via POLL delivery mode with ACKNOWLEDGE_ONLY", () -> {
					// poll verification endpoint with ACKNOWLEDGE_ONLY
					env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.ACKNOWLEDGE_ONLY.name());
					callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-8.1.4.1", "RFC8936-2.4");
					env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
					// we don't get any new sets back with acknowledge_only mode
//					callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);
//					callAndStopOnFailure(OIDSSFExtractVerificationEventFromReceivedSETs.class);
				});

				eventLog.runBlock("Trigger verification event 3", () -> {

					// Trigger verification event
					callAndStopOnFailure(OIDSSFTriggerVerificationEvent.class, "OIDSSF-8.1.4.2", "CAEPIOP-2.3.8.2");
					call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
					callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-8.1.4.2");

					callAndContinueOnFailure(WaitFor5Seconds.class, Condition.ConditionResult.INFO);
				});

				eventLog.runBlock("Verify verification event 3 received via POLL delivery mode with POLL_AND_ACKNOWLEDGE", () -> {
					// poll verification endpoint with POLL_AND_ACKNOWLEDGE
					env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_AND_ACKNOWLEDGE.name());
					callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-8.1.4.1", "RFC8936-2.4");
					env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
					callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);
					callAndStopOnFailure(OIDSSFExtractVerificationEventFromReceivedSETs.class);

					verifySetInResponse();
				});

			}
			break;

			default:
				// cannot happen
				break;
		}

		fireTestFinished();
	}

	protected void verifySetInResponse() {

		callAndContinueOnFailure(OIDSSFVerifySignatureOfVerificationEventToken.class, Condition.ConditionResult.WARNING);
		callAndStopOnFailure(OIDSSFParseVerificationEventToken.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			callAndContinueOnFailure(OIDSSFEnsureEventSignedWithRsa256.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.6");
		}

		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.1");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenDoesNotContainSubClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.2");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenDoesNotContainExpClaim.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.7");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenIssuerMatchesStreamConfigurationIssuer.class, Condition.ConditionResult.FAILURE, "OIDSSF-4.1.6");
		callAndContinueOnFailure(OIDSSFEnsureSecurityEventTokenIatIsNotInFuture.class, Condition.ConditionResult.FAILURE, "RFC8417-2.2", "RFC7519-4.1.6");

		callAndContinueOnFailure(OIDSSFValidateSecurityEventTokenAudClaim.class,Condition.ConditionResult.FAILURE, "OIDSSF-4.1.8");
		callAndContinueOnFailure(OIDSSFEnsureEventContainsStreamAudience.class, Condition.ConditionResult.WARNING, "RFC7519-4.1.3");

		callAndContinueOnFailure(OIDSSFValidateSecurityEventTokenTxnClaim.class, Condition.ConditionResult.INFO, "OIDSSF-4.1.9");

		callAndContinueOnFailure(OIDSSFCheckVerificationEventState.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
		callAndContinueOnFailure(OIDSSFCheckVerificationEventSubjectId.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
		callAndContinueOnFailure(OIDSSFCheckVerificationAuthorizationHeader.class, Condition.ConditionResult.FAILURE, "OIDSSF-6.1.1");
	}

	@Override
	public void cleanup() {
		eventLog.runBlock("Cleanup", () -> {
			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO);
			super.cleanup();
		});
	}
}
