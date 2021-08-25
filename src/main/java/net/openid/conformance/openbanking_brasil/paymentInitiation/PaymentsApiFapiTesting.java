package net.openid.conformance.openbanking_brasil.paymentInitiation;

import com.google.gson.JsonObject;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinal;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddOpenIdScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddPaymentScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetProtectedResourceUrlToPaymentsEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-fapi-testing",
	displayName = "Payments API test for FAPI conformance",
	summary = "Tests primarily 'happy' flows, using two different OAuth2 clients (and hence authenticating the user twice), and uses different variations on request objects, registered redirect uri (both redirect uris must be pre-registered as shown in the instructions). It also tests that TLS Certificate-Bound access tokens (required by the FAPI spec) are correctly implemented.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class PaymentsApiFapiTesting extends FAPI1AdvancedFinal {

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(AddPaymentScope.class);

		super.validateClientConfiguration();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
	}

}
