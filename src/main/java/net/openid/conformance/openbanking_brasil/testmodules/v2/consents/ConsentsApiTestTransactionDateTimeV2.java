package net.openid.conformance.openbanking_brasil.testmodules.v2.consents;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddConsentScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallConsentApiWithBearerToken;
import net.openid.conformance.openbanking_brasil.testmodules.support.CreateConsentWithInvalidFields;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas400;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToPostConsentRequest;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetContentTypeApplicationJson;
import net.openid.conformance.openbanking_brasil.testmodules.support.Validate400Response;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "consent-api-test-transactiondatetime-v2",
	displayName = "Ensures the server refuses a consent v2 creation request if the fields transactionFromDateTime and transactionToDateTime are present",
	summary = "Checks created consent response v2\n" +
		"\u2022 If transactionFromDateTime or transactionToDateTime are present then the server returns a 400 BAD REQUEST error\n" +
		"\u2022 If transactionFromDateTime and transactionToDateTime are not present then the consent is created\n" +
		"\u2022 Due to the data provided in this test, a 400 error should always be returned to be successful",
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
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class ConsentsApiTestTransactionDateTimeV2 extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void runTests(){
		runInBlock("Check create consent request v2", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(AddConsentScope.class);
			callAndStopOnFailure(CreateConsentWithInvalidFields.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class);
			callAndContinueOnFailure(EnsureResponseCodeWas400.class);
			callAndContinueOnFailure(Validate400Response.class, Condition.ConditionResult.FAILURE);
		});
	}

}
