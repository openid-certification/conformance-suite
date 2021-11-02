package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-proxy-test-incorrect-cpf",
	displayName = "Payments API test module ensuring unknown CPF is rejected",
	summary = "Payments API test module ensuring unknown CPF is rejected" +
		"Flow:" +
		"Makes a bad DICT payment flow with an unknown CPF value - expects a 422 at either the consent or payment initiation." +
		" stage, or a 201 and status of PNDG at payment initiaton with a subsequent state of RJCT" +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Config: Debtor account must be present in the config. We manually set the local instrument to DICT, add a creditor account, add an unknown cpf value.",
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
public class PaymentsApiIncorrectCPFProxyTestModule extends AbstractDictVerifiedPaymentTestModule {

	@Override
	protected void configureDictInfo() {
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(EnforcePresenceOfDebtorAccount.class);
		callAndContinueOnFailure(SelectDICTCodeLocalInstrument.class);
		callAndContinueOnFailure(RemoveQRCodeFromConfig.class);
		callAndContinueOnFailure(InjectRealCreditorAccountToPaymentConsent.class);
		callAndContinueOnFailure(InjectRealCreditorAccountToPayment.class);
		callAndContinueOnFailure(InjectCorrectButUnknownCpfOnPaymentConsent.class);
		callAndContinueOnFailure(InjectCorrectButUnknownCpfOnPayment.class);
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
	}

}
