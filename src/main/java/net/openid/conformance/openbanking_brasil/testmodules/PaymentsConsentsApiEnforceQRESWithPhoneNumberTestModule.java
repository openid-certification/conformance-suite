package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.SelectQRESCodeLocalInstrument;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetProxyToRealPhoneNumber;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.InjectQRCodeWithRealPhoneNumberIntoConfig;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SelectQRESCodePixLocalInstrument;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SetProxyToRealPhoneNumberOnPayment;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SetProxyToRealPhoneNumberOnPaymentConsent;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-qrdn-good-phone-number-proxy-test",
	displayName = "Payments Consents API test module for qrdn local instrument with phone number",
	summary = "Payments Consents API test module using a qr code with a phone number in it",
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
public class PaymentsConsentsApiEnforceQRESWithPhoneNumberTestModule extends AbstractOBBrasilQrCodePaymentFunctionalTestModule {

	@Override
	protected void configureDictInfo() {
		callAndStopOnFailure(SelectQRESCodeLocalInstrument.class);
		callAndStopOnFailure(SelectQRESCodePixLocalInstrument.class);
		callAndStopOnFailure(InjectQRCodeWithRealPhoneNumberIntoConfig.class);
		callAndStopOnFailure(SetProxyToRealPhoneNumber.class);
		callAndStopOnFailure(SetProxyToRealPhoneNumberOnPaymentConsent.class);
		callAndStopOnFailure(SetProxyToRealPhoneNumberOnPayment.class);
	}
}
