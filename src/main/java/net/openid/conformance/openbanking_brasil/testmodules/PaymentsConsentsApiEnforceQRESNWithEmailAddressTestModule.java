package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.SelectQRESCodeLocalInstrument;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetProxyToRealEmailAddress;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-qres-good-email-proxy-test",
	displayName = "Payments Consents API test module for QRES local instrument with email address",
	summary = "Payments Consents API test module using a qr code with an email address in it",
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
public class PaymentsConsentsApiEnforceQRESNWithEmailAddressTestModule extends AbstractOBBrasilQrCodePaymentFunctionalTestModule {

	@Override
	protected void configureDictInfo() {
		callAndStopOnFailure(SelectQRESCodeLocalInstrument.class);
		callAndStopOnFailure(SelectQRESCodePixLocalInstrument.class);
		callAndStopOnFailure(InjectQRCodeWithRealEmailIntoConfig.class);
		callAndStopOnFailure(SetProxyToRealEmailAddress.class);
		callAndStopOnFailure(SetProxyToRealEmailAddressOnPaymentConsent.class);
		callAndStopOnFailure(SetProxyToRealEmailAddressOnPayment.class);
	}
}
