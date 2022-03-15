package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationConsentValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SanitiseQrCodeConfig;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-jti-reuse-test",
	displayName = "Payments Consents API test module which attempts to reuse a jti",
	summary = "Payments Consents API test module which attempts to reuse a jti" +
		"Flow:" +
		"Makes a good consent flow - expects success. Makes a bad consent flow with a reused jti - expects 403." +
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
	protected void postConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {
		callAndContinueOnFailure(SanitiseQrCodeConfig.class);
	}

	@Override
	protected void runTests() {
		runInBlock("Create a payment consent", () -> {
			callAndStopOnFailure(EnsurePaymentDateIsToday.class);
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);

			call(sequence(SignedPaymentConsentSequence.class));

			callAndStopOnFailure(PaymentInitiationConsentValidator.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(SetApplicationJwtAcceptHeaderForResourceEndpointRequest.class);
			call(new ValidateSelfEndpoint());

	});

		runInBlock("Create a payment consent re-using jti", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);

			call(new SignedPaymentConsentSequence()
				.skip(AddJtiAsUuidToRequestObject.class, "Re-use previous jti")
				.replace(EnsureContentTypeApplicationJwt.class, condition(EnsureResourceResponseReturnedJsonContentType.class))
				.replace(EnsureHttpStatusCodeIs201.class, condition(EnsurePaymentConsentResponseWas403.class))
				.skip(ExtractSignedJwtFromResourceResponse.class, "403 Response JSON")
				.skip(FAPIBrazilValidateResourceResponseSigningAlg.class, "403 Response JSON")
				.skip(FAPIBrazilValidateResourceResponseTyp.class, "403 Response JSON")
				.skip(ValidateResourceResponseSignature.class, "403 Response JSON")
				.skip(ValidateResourceResponseJwtClaims.class, "403 Response JSON")
			);

		});


	}

}
