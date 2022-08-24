package net.openid.conformance.openinsurance.testmodule.v1.consents;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetCustomCustomerIdentifications;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "opin-consent-api-status-test",
	displayName = "Validate that consents are actually authorised on redirect",
	summary = "Validates that consents are actually authorised on redirect\n" +
		"\u2022 Call the POST Consents API with either the customer business or the customer personal PERMISSIONS, depending on what option has been selected by the user on the configuration field\n" +
		"\u2022 Expect a 201 - Validate all of the fields of response_body of the POST Consents API response\n" +
		"\u2022 Redirect the user to authorize the created ConsentID - Request all the OPIN Phase 2 scopes\n" +
		"\u2022 Calls the GET Resources endpoint\n" +
		"\u2022 Expects either a 200 or an error\n" +
		"\u2022 Calls the GET Consents endpoint\n" +
		"\u2022 Expects a 200 with the Consent being authorised",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.customerUrl",
		"consent.productType"
	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class OpinConsentsApiConsentStatusTestModule extends AbstractOBBrasilFunctionalTestModuleOptionalErrors {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildCustomCustomersConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(PrepareAllCustomerRelatedConsentsForResource404HappyPathTest.class);
		callAndStopOnFailure(PrepareToGetCustomCustomerIdentifications.class);
	}

	@Override
	protected void validateResponse() {
		String responseError = env.getString("resource_endpoint_error_code");
		if (Strings.isNullOrEmpty(responseError)) {
			runInBlock("Validating get consent response", () -> {
				callAndStopOnFailure(PrepareToFetchConsentRequest.class);
				callAndStopOnFailure(TransformConsentRequestForProtectedResource.class);
				call(createGetAccessTokenWithClientCredentialsSequence(addTokenEndpointClientAuthentication));
				preCallProtectedResource("Fetch consent");
				callAndStopOnFailure(EnsureConsentWasAuthorised.class);
			});
		} else {
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas404.class);
			String logMessage = String.format("Call personal endpoint");
			runInBlock(logMessage, () -> {
				callAndStopOnFailure(PrepareToCallCustomerDataEndpoint.class);
				callAndStopOnFailure(CallProtectedResource.class);
				callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.WARNING);
			});
		}
	}

	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainAccessTokenWithClientCredentials(clientAuthSequence);
	}

}
