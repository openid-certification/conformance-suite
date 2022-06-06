package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentFetchPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.PaymentsProxyCheckForInvalidStatus;
import net.openid.conformance.openbanking_brasil.testmodules.support.SelectQRESCodeLocalInstrument;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetProxyToRealPhoneNumber;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-qres-good-phone-number-proxy-test",
	displayName = "Payments Consents API test module for QRES local instrument with phone number",
	summary = "Payments Consents API test module for qres using a valid phone number proxy key (+5561990010001). Test will set a Payload with the creditor account related to a valid proxy key and will use the debtor account provided on the configuration field. The QRcode used is static and will also be generated by the conformance suite. Test will execute a POST Consent, POST Payment and will Poll a GET on the created Payment to ensure that the payment will reach any valid a Accepted state ",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl"
	}
)
public class PaymentsConsentsApiEnforceQRESWithPhoneNumberTestModule extends AbstractOBBrasilQrCodePaymentFunctionalTestModule {

	@Override
	protected void configureDictInfo() {
		callAndStopOnFailure(SelectQRESCodeLocalInstrument.class);
		callAndStopOnFailure(SelectQRESCodePixLocalInstrument.class);
		callAndStopOnFailure(SetPaymentAmountToKnownValueOnConsent.class);
		callAndStopOnFailure(SetPaymentAmountToKnownValueOnPaymentInitiation.class);
		callAndStopOnFailure(InjectQRCodeWithRealPhoneNumberIntoConfig.class);
		callAndStopOnFailure(SetProxyToRealPhoneNumber.class);
		callAndStopOnFailure(SetProxyToRealPhoneNumberOnPaymentConsent.class);
		callAndStopOnFailure(SetProxyToRealPhoneNumberOnPayment.class);
		callAndStopOnFailure(InjectRealCreditorAccountToPaymentPhone.class);
		callAndStopOnFailure(InjectRealCreditorAccountPhoneToPaymentConsent.class);
	}

	@Override
	protected ConditionSequence statusValidationSequence() {
		return sequenceOf(
			condition(PaymentsProxyCheckForAcceptedStatus.class),
			condition(PaymentsProxyCheckForInvalidStatus.class),
			condition(PaymentFetchPixPaymentsValidator.class));
	}

}
