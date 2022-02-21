package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentFetchPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.EnsureNoRejectionReasonIFStatusIsNotRJCT;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.InjectRealCreditorAccountPhoneToPaymentConsent;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.InjectRealCreditorAccountToPaymentPhone;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-proxy-phone-number-proxy-test",
	displayName = "Payments Consents API test module ensuring phone number is a valid proxy",
	summary = "Payments Consents API test module ensuring phone number is a valid proxy" +
		"Flow:" +
		"Makes a good consent flow with a valid phone number proxy field - expects success." +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Resource url pointing at the base url. The test appends on the required payment endpoints" +
		"Config: We manually set the local instrument for consent to DICT for this test. We manually set the proxy to a real phone number. We manually add a creditor account.",
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
public class PaymentsConsentsApiPhoneNumberProxyTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		super.validateClientConfiguration();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(EnforcePresenceOfDebtorAccount.class);

		// Setting consent to DICT / proxy to real phone number
		eventLog.startBlock("Setting payment consent payload to use real phone number + DICT");
		callAndContinueOnFailure(SelectDICTCodeLocalInstrument.class);
		callAndContinueOnFailure(RemoveQRCodeFromConfig.class);
		callAndContinueOnFailure(InjectRealCreditorAccountPhoneToPaymentConsent.class);
		callAndContinueOnFailure(SetProxyToRealPhoneNumber.class);

		// Setting payment to DICT / proxy to real phone number
		eventLog.startBlock("Setting payment payload to use real phone number + DICT");
		callAndStopOnFailure(SelectDICTCodePixLocalInstrument.class);
		callAndContinueOnFailure(InjectRealCreditorAccountToPaymentPhone.class);
		callAndStopOnFailure(RemoveTransactionIdentification.class);

		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
	}

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(PaymentInitiationPixPaymentsValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureNoRejectionReasonIFStatusIsNotRJCT.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureSelfLinkEndsInPaymentId.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		eventLog.startBlock("Checking the self endpoint - Expecting 200, validating response");
		call(new ValidateSelfEndpoint()
			.insertAfter(
				EnsureResponseCodeWas200.class, sequenceOf(
					condition(EnsureResponseWasJwt.class),
					condition(PaymentFetchPixPaymentsValidator.class)
				))
			.replace(CallProtectedResourceWithBearerToken.class, sequenceOf(
				condition(AddJWTAcceptHeader.class),
				condition(CallProtectedResourceWithBearerTokenAndCustomHeaders.class)
			)));
		ensurePaymentIsAcceptedOrRejected();

	}

	protected void ensurePaymentIsAcceptedOrRejected() {
		eventLog.startBlock("Ensuring payment is either accepted or rejected: 5 minute time limit");

		// 11 checks to be done:
		// 0s, 30s, ..., 600s
		int totalChecks;
		int checkCount = 10;
		boolean pass = false;

		for(totalChecks = 0; totalChecks <= checkCount; totalChecks++){
			pollPayment();
			callAndStopOnFailure(CheckPaymentStatus.class);
			if(Boolean.TRUE.equals(env.getBoolean("paymentStatusCorrect"))){
				callAndStopOnFailure(SuccessfulPaymentUpdate.class);
				pass = true;
				break;
			} else {
				callAndStopOnFailure(FailedPaymentUpdate.class);
			}
			callAndStopOnFailure(WaitFor30Seconds.class);
			callAndStopOnFailure(LoadOldValues.class);
		}

		if(!pass){
			callAndStopOnFailure(FailedToUpdatePaymentInFiveMinutes.class);
		}
	}

	protected void pollPayment() {
		call(new ValidateSelfEndpoint()
			.replace(CallProtectedResourceWithBearerToken.class, sequenceOf(
				condition(AddJWTAcceptHeader.class),
				condition(CallProtectedResourceWithBearerTokenAndCustomHeaders.class)
			))
			.skip(
				LoadOldValues.class, "skipping load old values - first check"
			)
		);
	}
}
