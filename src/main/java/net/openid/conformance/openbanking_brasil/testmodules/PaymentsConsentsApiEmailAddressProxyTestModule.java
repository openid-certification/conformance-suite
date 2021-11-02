package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.fapi1advancedfinal.SetApplicationJwtAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationConsentValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-proxy-email-proxy-test",
	displayName = "Payments Consents API test module ensuring email address is a valid proxy",
	summary = "Payments Consents API test module ensuring email address is a valid proxy" +
		"Flow:" +
		"Makes a good payment consent flow - expects success." +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Config: We manually set the local instrument for consent DICT for this test. We manually add a real creditor account for this test. We manually set the proxy field for this test.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)
public class PaymentsConsentsApiEmailAddressProxyTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainPaymentsAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	protected void postConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndContinueOnFailure(SelectDICTCodeLocalInstrument.class);
		callAndContinueOnFailure(RemoveQRCodeFromConfig.class);
		callAndContinueOnFailure(InjectRealCreditorAccountToPaymentConsent.class);
		callAndContinueOnFailure(InjectRealCreditorAccountToPayment.class);
		callAndContinueOnFailure(SetProxyToRealEmailAddress.class);
		callAndStopOnFailure(RemoveTransactionIdentification.class);
	}

	@Override
	protected void runTests() {
		runInBlock("Validate payment initiation consent", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);

			call(sequence(SignedPaymentConsentSequence.class));

			callAndStopOnFailure(PaymentInitiationConsentValidator.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureSelfLinkEndsInConsentId.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(new ValidateSelfEndpoint()
				.insertAfter(ClearContentTypeHeaderForResourceEndpointRequest.class, condition(SetApplicationJwtAcceptHeaderForResourceEndpointRequest.class)
				));
		});
	}
}
