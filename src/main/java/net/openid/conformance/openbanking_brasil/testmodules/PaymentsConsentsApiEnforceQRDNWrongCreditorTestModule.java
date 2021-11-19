package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-qrdn-bad-creditor-proxy-test",
	displayName = "Payments Consents API test module for QRDN local instrument with bad creditor",
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
public class PaymentsConsentsApiEnforceQRDNWrongCreditorTestModule extends AbstractOBBrasilQrCodePaymentFunctionalTestModule {


	@Override
	protected void configureDictInfo() {

	}
}
