package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.AddAudValueAsArrayToPaymentsConsentResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-payment-consent-response-valid-aud-as-array",
	displayName = "FAPI1-Advanced-Final: client test - valid aud in payment consent response as data type array",
	summary = "This test should be successful. The value of aud within the payment consent response will be represented as array with one value",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)
// Only applicable to Brazil OpenBanking.
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = {
		"plain_fapi",
		"openbanking_uk",
		"consumerdataright_au",
		"openinsurance_brazil",
		"openbanking_ksa" })
public class FAPI1AdvancedFinalClientTestPaymentConsentRepsonseValidAudAsArray extends AbstractFAPI1AdvancedFinalClientTest {

	@Override
	protected void addCustomValuesToIdToken(){
	}

	@Override
	protected void addCustomAudToPaymentsConsentResponse(){
		callAndStopOnFailure(AddAudValueAsArrayToPaymentsConsentResponse.class,"RFC7519-4.1.3");
	}
}
