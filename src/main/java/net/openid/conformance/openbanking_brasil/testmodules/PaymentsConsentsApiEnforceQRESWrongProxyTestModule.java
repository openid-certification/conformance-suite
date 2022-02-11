package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Optional;

@PublishTestModule(
	testName = "payments-api-qres-mismatched-proxy-test",
	displayName = "Payments Consents API test module for QRES local instrument with mismatched proxy",
	summary = "Payments Consents API test module using a qres code with a PIX key that differs from the proxy key sent. Test will set a Payload using the creditor account details related to the cliente-a00001@pix.bcb.gov.br and the debtor account details will fetched from the configuration field set by the instituion. The qrcode will be a static QRcode provided by the Conformance Suite that will have a proxy value that differs from cliente-a00001@pix.bcb.gov.br. The test will expect a failure that may happen on any point during the payment flow. For a failure on the POST Consents, test will expect a 422 - DETALHE_PGTO_INVALIDO. For a Failure on the POST Payments, test will expect a 422 - COBRANCA_INVALIDA. For a Rejection on the created Payment, test will expect a payment with RJCT status and RejectionReason equal to ELEMENT_CONTENT_FORMALLY_INCORRECT",
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
public class PaymentsConsentsApiEnforceQRESWrongProxyTestModule extends AbstractDictVerifiedPaymentTestModule {

	@Override
	protected void configureDictInfo() {
		callAndStopOnFailure(SelectQRESCodeLocalInstrument.class);
		callAndStopOnFailure(SelectQRESCodePixLocalInstrument.class);
		callAndStopOnFailure(InjectQRCodeWithWrongEmailAddressProxyIntoConfig.class);
		callAndStopOnFailure(SetProxyToRealEmailAddress.class);
		callAndStopOnFailure(SetProxyToRealEmailAddressOnPaymentConsent.class);
		callAndStopOnFailure(SetProxyToRealEmailAddressOnPayment.class);
		callAndStopOnFailure(InjectRealCreditorAccountEmailToPaymentConsent.class);
		callAndStopOnFailure(InjectRealCreditorAccountToPayment.class);
		// callAndContinueOnFailure(ValidateErrorAndMetaFieldNames.class, Condition.ConditionResult.FAILURE);
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		ConditionSequence preAuth = super.createOBBPreauthSteps();
		preAuth.insertAfter(OptionallyAllow201Or422.class, condition(EnforcePaymentConsentFailureForInvalidDetails.class));
		return preAuth;
	}

	@Override
	protected ConditionSequence statusValidationSequence() {
		return sequenceOf(
			condition(PaymentsProxyCheckForRejectedStatus.class),
			condition(VerifyRejectionReasonForQrCode.class),
			condition(PaymentsProxyCheckForInvalidStatus.class));
	}

	@Override
	protected Optional<Class<? extends Condition>> resourceCreationErrorMessageCondition() {
		return Optional.of(VerifyErrorIfPixPostFailsOnQresCobranca.class);
	}

	@Override
	protected Optional<Class<? extends Condition>> consentErrorMessageCondition() {
		return Optional.ofNullable(EnsureConsentErrorWasDetalhePgtoInvalido.class);
	}

}
