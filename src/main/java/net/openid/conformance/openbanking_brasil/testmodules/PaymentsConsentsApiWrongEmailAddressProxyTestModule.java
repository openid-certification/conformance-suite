package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-proxy-fake-email-proxy-test",
	displayName = "Payments Consents API test module ensuring email address is incorrect",
	summary = "Payments Consents API test module ensuring email address is incorrect" +
		"Flow:" +
		"Makes a bad payment consent flow - expects a 422 error." +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Resource url pointing at the base url. The test appends on the required payment endpoints" +
		"Config: We manually set the local instrument for consent to DICT for this test. We manually change the proxy field to a fake email. We manually add a creditor account.",
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
public class PaymentsConsentsApiWrongEmailAddressProxyTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainPaymentsAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	protected void postConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {
		callAndContinueOnFailure(SelectDICTCodeLocalInstrument.class);
		callAndContinueOnFailure(InjectRealCreditorAccount.class);
		callAndContinueOnFailure(SetProxyToFakeEmailAddress.class);
	}

	@Override
	protected void runTests() {
		runInBlock("Initiate consent and ensure we get an error", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);

			call(sequence(PaymentConsentErrorTestingSequence.class));
			callAndStopOnFailure(EnsureConsentResponseCodeWas422.class);

		});
	}
}
