package net.openid.conformance.openbanking_brasil.paymentInitiation;

import com.google.gson.JsonObject;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalBrazilEnsureBadPaymentSignatureFails;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddOpenIdScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddPaymentScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetProtectedResourceUrlToPaymentsEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-bad-signature-test",
	displayName = "Payments API test using bad signature on request",
	summary = "This test makes a request to the payment consent endpoint request with a bad signature, which must fail.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.resourceUrl"
	}
)
public class PaymentsApiBadPaymentSignatureFails extends FAPI1AdvancedFinalBrazilEnsureBadPaymentSignatureFails {

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(AddPaymentScope.class);

		super.validateClientConfiguration();
	}

}
