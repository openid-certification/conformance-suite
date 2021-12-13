package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Optional;

@PublishTestModule(
	testName = "payments-api-qrdn-with-qres-code-test",
	displayName = "Payments Consents API test module for QRDN local instrument with a QRES code",
	summary = "Payments Consents API test module using a qr code which is a QRES code",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
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
	protected Optional<Class<? extends Condition>> consentErrorMessageCondition() {
		return Optional.ofNullable(EnsureConsentErrorWasDetalhePgtoInvalido.class);
	}

	@Override
	protected Optional<Class<? extends Condition>> resourceCreationErrorMessageCondition() {
		return Optional.ofNullable(VerifyErrorIfPixPostFailsOnQresCobranca.class);
	}
}
