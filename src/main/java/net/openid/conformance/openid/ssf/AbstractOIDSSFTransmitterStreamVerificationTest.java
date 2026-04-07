package net.openid.conformance.openid.ssf;

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
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

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

		eventLog.runBlock("Validate TLS Connection", this::validateTlsConnection);

		eventLog.runBlock("Prepare Transmitter Access", this::obtainTransmitterAccessToken);

		eventLog.runBlock("Clean stream environment if necessary", this::cleanUpStreamConfigurationIfNecessary);

		SsfDeliveryMode deliveryMode = getVariant(SsfDeliveryMode.class);

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

		triggerVerificationEvent();

		performVerification();

		fireTestFinished();
	}

	protected void triggerVerificationEvent() {
		eventLog.runBlock("Trigger verification event", () -> {
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

		callAndContinueOnFailure(OIDSSFVerifySignatureOfVerificationEventToken.class, Condition.ConditionResult.WARNING);
		callAndStopOnFailure(OIDSSFParseVerificationEventToken.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
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
