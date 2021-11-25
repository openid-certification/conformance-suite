package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.InjectInvalidCreditorAccountToPayment;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.InjectInvalidCreditorAccountToPaymentConsent;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SetProxyToRealEmailAddressOnPayment;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SetProxyToRealEmailAddressOnPaymentConsent;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-proxy-real-email-invalid-credior-proxy-test",
	displayName = "Payments API test module ensuring invalid creditor accounts are rejected",
	summary = "Payments API test module ensuring invalid creditor accounts are rejected" +
		"Flow:" +
		"Makes a bad DICT payment flow with a correct email but invalid creditor - expects a 422 at either consent or payment" +
		" initiation stage, or a 201 with a PNDG status at the payment initiation stage but a subsequent status of RJCT" +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Config: Debtor account must be present in the config. We manually set the local instrument to DICT, add a creditor account, add email address.",
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
public class PaymentsApiRealEmailAddressWrongCreditorProxyTestModule extends AbstractDictVerifiedPaymentTestModule {

	@Override
	protected void configureDictInfo() {
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndContinueOnFailure(SelectDICTCodeLocalInstrument.class);
		callAndContinueOnFailure(SelectDICTCodePixLocalInstrument.class);
		callAndContinueOnFailure(RemoveQRCodeFromConfig.class);
		callAndContinueOnFailure(RemoveTransactionIdentification.class);
		callAndContinueOnFailure(InjectInvalidCreditorAccountToPaymentConsent.class);
		callAndContinueOnFailure(InjectInvalidCreditorAccountToPayment.class);
		callAndContinueOnFailure(SetProxyToRealEmailAddressOnPaymentConsent.class);
		callAndContinueOnFailure(SetProxyToRealEmailAddressOnPayment.class);

		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
	}

}