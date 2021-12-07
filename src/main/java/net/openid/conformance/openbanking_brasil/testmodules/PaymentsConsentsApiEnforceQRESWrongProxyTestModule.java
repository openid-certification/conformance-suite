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
	summary = "Payments Consents API test module using a qres code with an email address in it which is different from the proxy",
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
			condition(VerifyRejectionReasonForQres.class),
			condition(PaymentsProxyCheckForInvalidStatus.class));
	}

	@Override
	protected Optional<Class<? extends Condition>> resourceCreationErrorMessageCondition() {
		return Optional.of(VerifyErrorIfPixPostFailsOnQres.class);
	}
}
