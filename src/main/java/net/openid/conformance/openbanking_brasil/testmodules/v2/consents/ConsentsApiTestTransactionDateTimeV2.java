package net.openid.conformance.openbanking_brasil.testmodules.v2.consents;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.CallConsentEndpointWithBearerTokenAnyHttpMethod;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.consent.v2.ConsentDetailsIdentifiedByConsentIdValidatorV2;
import net.openid.conformance.openbanking_brasil.consent.v2.CreateNewConsentValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.EnsureConsentResponseCodeWas201;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "consent-api-test-transactiondatetime-v2",
	displayName = "Ensures the server refuses a consent v2 creation request if the fields transactionFromDateTime and transactionToDateTime are present",
	summary = "Checks created consent response v2\n" +
		"\u2022 Call the POST Consents API request including fields transactionFromDateTime and transactionToDateTime on the request body" +
		"\u2022 Expect a 201 Created Response - Make sure that the response does not contain the fields transactionFromDateTime and transactionToDateTime\n" +
		"\u2022 Call the GET Consents API request with created Consent\n" +
		"\u2022 Expect a 200 OK Response - Make sure that the response does not contain the fields transactionFromDateTime and transactionToDateTime",
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
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			env.putString("fromTransactionDate", "2022-08-03");
			env.putString("toTransactionDate", "2022-08-04");
			callAndContinueOnFailure(AddToAndFromTransactionDateParametersToConsentUrl.class);
			callAndContinueOnFailure(CallConsentEndpointWithBearerTokenAnyHttpMethod.class);

			call(exec().mapKey("resource_endpoint_response_full", "consent_endpoint_response_full"));
			callAndStopOnFailure(ResourceEndpointResponseFromFullResponse.class);
			callAndStopOnFailure(EnsureConsentResponseCodeWas201.class);

			callAndContinueOnFailure(CreateNewConsentValidatorV2.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
			callAndStopOnFailure(EnsureConsentHasNoTransactionFromToDateTime.class);
		});

		runInBlock("Validating get consent response v2", () -> {
			callAndStopOnFailure(ConsentIdExtractor.class);
			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndContinueOnFailure(CallConsentEndpointWithBearerTokenAnyHttpMethod.class, Condition.ConditionResult.FAILURE);

			call(exec().mapKey("resource_endpoint_response_full", "consent_endpoint_response_full"));
			callAndStopOnFailure(ResourceEndpointResponseFromFullResponse.class);
			callAndStopOnFailure(EnsureResponseCodeWas200.class);

			callAndContinueOnFailure(ConsentDetailsIdentifiedByConsentIdValidatorV2.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
			callAndContinueOnFailure(EnsureConsentHasNoTransactionFromToDateTime.class);
		});
	}
}
