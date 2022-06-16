package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Optional;

@PublishTestModule(
	testName = "payments-api-qres-wrong-amount-proxy-test",
	displayName = "Payments Consents API test module for QRES local instrument with divergent amount",
	summary = "Payments Consents API test module using a qr code with an amount in it which differs to the consent and payment initiation. Test will set a Payload using the creditor account details related to the cliente-a00001@pix.bcb.gov.br and the debtor account details will fetched from the configuration field set by the instituion. The qrcode will be a static QRcode provided by the Conformance Suite that will have an amount that differs from the amount sent on the payload of both the POST Payments and POST Consents request. The test will expect a failure that may happen on any point during the payment flow. For a failure on the POST Consents, test will expect a 422 - DETALHE_PGTO_INVALIDO. For a Failure on the POST Payments, test will expect a 422 - COBRANCA_INVALIDA. For a Rejection on the created Payment, test will expect a payment with RJCT status and RejectionReason equal to ELEMENT_CONTENT_FORMALLY_INCORRECT",
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
public class PaymentsConsentsApiEnforceQRESWrongAmountTestModule extends AbstractDictVerifiedPaymentTestModule {

	@Override
	protected void configureDictInfo() {
		callAndStopOnFailure(SelectQRESCodeLocalInstrument.class);
		callAndStopOnFailure(SelectQRESCodePixLocalInstrument.class);
		callAndStopOnFailure(SetPaymentAmountToKnownValueOnConsent.class);
		callAndStopOnFailure(SetPaymentAmountToKnownValueOnPaymentInitiation.class);
		//callAndStopOnFailure(InjectQRCodeWithWrongAmountIntoConfig.class);
		callAndStopOnFailure(SetProxyToRealEmailAddress.class);
		callAndStopOnFailure(SetProxyToRealEmailAddressOnPaymentConsent.class);
		callAndStopOnFailure(SetProxyToRealEmailAddressOnPayment.class);
		callAndStopOnFailure(InjectRealCreditorAccountEmailToPaymentConsent.class);
		callAndStopOnFailure(InjectRealCreditorAccountToPayment.class);
		callAndStopOnFailure(InjectQRCodeWithWrongAmountIntoConfig.class);
	}

	@Override
	protected ConditionSequence statusValidationSequence() {
		return sequenceOf(
			condition(PaymentsProxyCheckForRejectedStatus.class),
			condition(VerifyRejectionReasonForQrCode.class),
			condition(PaymentsProxyCheckForInvalidStatus.class));
	}

	@Override
	protected Optional<Class<? extends Condition>> consentErrorMessageCondition() {
		return Optional.ofNullable(EnsureConsentErrorWasDetalhePgtoInvalido.class);
	}

	@Override
	protected Optional<Class<? extends Condition>> resourceCreationErrorMessageCondition() {
		return Optional.ofNullable(VerifyErrorIfPixPostFailsOnQresCobranca.class);
	}

}
