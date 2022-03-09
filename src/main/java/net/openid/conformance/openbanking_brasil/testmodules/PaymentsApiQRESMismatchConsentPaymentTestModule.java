package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Optional;

@PublishTestModule(
	testName = "payments-api-qres-mismatched-consent-payment-test",
	displayName = "Payments Consents API test module for QRES local instrument with mismatched QR codes",
	summary = "Payments Consents API test module using a different QR code in the consent to that in the payments. Test will set a Payload using the creditor account details related to the cliente-a00001@pix.bcb.gov.br and the debtor account details will fetched from the configuration field set by the instituion. Test will send the qr code on the POST Consents with one city and the qr code on the POST Payments with a different city, making both qrcodes different. Test will expect a failure on the POST Payments - 422 PAGAMENTO_DIVERGENTE_DO_CONSENTIMENTO ",
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
public class PaymentsApiQRESMismatchConsentPaymentTestModule extends AbstractDictVerifiedPaymentTestModule {

	@Override
	protected void configureDictInfo() {
		callAndStopOnFailure(SelectQRESCodeLocalInstrument.class);
		callAndStopOnFailure(SelectQRESCodePixLocalInstrument.class);
		callAndStopOnFailure(InjectMismatchingQRCodeIntoConfig.class);
		callAndStopOnFailure(SetProxyToRealEmailAddress.class);
		callAndStopOnFailure(SetProxyToRealEmailAddressOnPaymentConsent.class);
		callAndStopOnFailure(SetProxyToRealEmailAddressOnPayment.class);
		callAndStopOnFailure(InjectRealCreditorAccountEmailToPaymentConsent.class);
		callAndStopOnFailure(InjectRealCreditorAccountToPayment.class);

	}

	@Override
	protected void requestProtectedResource() {
		if(!validationStarted) {
			validationStarted = true;
			eventLog.startBlock("initiate payment");
			ConditionSequence pixSequence = new CallPixPaymentsEndpointSequence()
				.skip(EnsureResponseCodeWas201.class, "Not needed here");
			call(pixSequence);
			eventLog.endBlock();
			eventLog.startBlock(currentClientString() + "Validate response");
			validateResponse();
			eventLog.endBlock();
		}
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		ConditionSequence preAuth = super.createOBBPreauthSteps();
		preAuth.insertAfter(OptionallyAllow201Or422.class, condition(EnsureEndpointResponseWas201.class));
		return preAuth;
	}

	@Override
	protected Optional<Class<? extends Condition>> resourceCreationErrorMessageCondition() {
		return Optional.ofNullable(VerifyErrorIfPixPostFailsOnQres.class);
	}

	@Override
	protected ConditionSequence statusValidationSequence() {
		return sequenceOf(
			condition(PaymentsProxyCheckForRejectedStatus.class),
			condition(VerifyRejectionReasonForQrCode.class),
			condition(PaymentsProxyCheckForInvalidStatus.class));
	}
}
