package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddJtiAsUuidToRequestObject;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.fapi1advancedfinal.SetApplicationJwtContentTypeHeaderForResourceEndpointRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationConsentValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-jti-reuse-test",
	displayName = "Payments Consents API test module which attempts to reuse a jti",
	summary = "Payments Consents API test module which attempts to reuse a jti" +
		"Flow:" +
		"Makes a good consent flow - expects success. Makes a bad consent flow with a reused jti - expects 422." +
		"Required:" +
		"Consent url pointing at the consent endpoint.",
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
public class PaymentsConsentsReuseJtiTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainPaymentsAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	protected void runTests() {
		runInBlock("Create a payment consent", () -> {
			eventLog.startBlock("Setting date to today");
			callAndStopOnFailure(EnsurePaymentDateIsToday.class);
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);

			call(sequence(SignedPaymentConsentSequence.class));

			callAndStopOnFailure(PaymentInitiationConsentValidator.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(new ValidateSelfEndpoint()
				.replace(ClearContentTypeHeaderForResourceEndpointRequest.class, condition(SetApplicationJwtContentTypeHeaderForResourceEndpointRequest.class)
			));
		});

		runInBlock("Create a payment consent re-using jti", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);

			call(new SignedPaymentConsentSequence()
				.skip(AddJtiAsUuidToRequestObject.class, "Re-use previous jti")
				.replace(EnsureHttpStatusCodeIs201.class, condition(EnsureConsentResponseCodeWas422.class))
			);

		});


	}

}
