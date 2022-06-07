package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Optional;

@PublishTestModule(
	testName = "payments-api-qrdn-with-qres-code-test",
	displayName = "Payments Consents API test module for QRDN local instrument with a QRES code",
	summary = "Payments Consents API test module using a qrdn localInstrument but a qres qrCode . Test will set a Payload using the creditor account details related to the cliente-a00001@pix.bcb.gov.br and the debtor account details will fetched from the configuration field set by the instituion. Test will set the local instrument to be qrdn while the qrcode will be a valid static QRcode provided by the Conformance Suite for proxy cliente-a00001@pix.bcb.gov.br. The test will expect a failure that may happen on any point during the payment flow. For a failure on the POST Consents, test will expect a 422 - DETALHE_PGTO_INVALIDO. For a Failure on the POST Payments, test will expect a 422 - COBRANCA_INVALIDA. For a Rejection on the created Payment, test will expect a payment with RJCT status and RejectionReason equal to ELEMENT_CONTENT_FORMALLY_INCORRECT.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilPaymentConsent",
		"resource.brazilPixPayment",
	}
)
public class PaymentsConsentsApiEnforceQRDNWithQRESCodeTestModule extends AbstractDictVerifiedPaymentTestModule {

	@Override
	protected void configureDictInfo() {
		callAndStopOnFailure(SelectQRDNCodeLocalInstrument.class);
		callAndStopOnFailure(SelectQRDNCodePixLocalInstrument.class);
		callAndStopOnFailure(SetPaymentAmountToKnownValueOnConsent.class);
		callAndStopOnFailure(SetPaymentAmountToKnownValueOnPaymentInitiation.class);
		callAndStopOnFailure(InjectQRCodeWithRealEmailIntoConfig.class);
		callAndStopOnFailure(SetProxyToRealEmailAddress.class);
		callAndStopOnFailure(SetProxyToRealEmailAddressOnPaymentConsent.class);
		callAndStopOnFailure(SetProxyToRealEmailAddressOnPayment.class);
		callAndStopOnFailure(InjectRealCreditorAccountEmailToPaymentConsent.class);
		callAndStopOnFailure(InjectRealCreditorAccountToPayment.class);

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
