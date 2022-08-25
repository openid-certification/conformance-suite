package net.openid.conformance.openinsurance.testmodule.v1.consents;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallConsentEndpointWithBearerTokenAnyHttpMethod;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.OpinAddScopesForAll;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetCustomCustomerIdentifications;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openinsurance.testmodule.support.EnsureStatusAuthorised;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.openinsurance.validator.consents.v1.OpinConsentDetailsIdentifiedByConsentIdValidatorV1;
import net.openid.conformance.openinsurance.validator.consents.v1.OpinCreateNewConsentValidatorV1;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
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
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
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

	private OpinConsentPermissionsBuilder permissionsBuilder;
	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildCustomCustomersConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		permissionsBuilder = new OpinConsentPermissionsBuilder(env,getId(),eventLog,testInfo,executionManager);
		callAndStopOnFailure(OpinAddScopesForAll.class);

		String productType = env.getString("config", "consent.productType");
		if (productType.equals("business")) {
			permissionsBuilder.addPermissionsGroup(PermissionsGroup.CUSTOMERS_BUSINESS).build();
		} else {
			permissionsBuilder.addPermissionsGroup(PermissionsGroup.CUSTOMERS_PERSONAL).build();
		}
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		OpenBankingBrazilPreAuthorizationSteps steps = new OpenBankingBrazilPreAuthorizationSteps(false,
			false, addTokenEndpointClientAuthentication, false, false);
		steps.then(exec().mapKey("resource_endpoint_response_full", "consent_endpoint_response_full"),
			condition(ResourceEndpointResponseFromFullResponse.class),
			condition(OpinCreateNewConsentValidatorV1.class).dontStopOnFailure());
		return steps;
	}
	@Override
	protected void requestProtectedResource() {
		runInBlock("Validating get consent response V1", () -> {
			callAndStopOnFailure(ConsentIdExtractor.class);
			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndContinueOnFailure(CallConsentEndpointWithBearerTokenAnyHttpMethod.class, Condition.ConditionResult.FAILURE);

			exec().mapKey("resource_endpoint_response_full", "consent_endpoint_response_full");
			callAndStopOnFailure(ResourceEndpointResponseFromFullResponse.class);

			callAndContinueOnFailure(OpinConsentDetailsIdentifiedByConsentIdValidatorV1.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
			callAndContinueOnFailure(EnsureStatusAuthorised.class);
		});
	}

	@Override
	protected void validateResponse() {
		//Not needed for this test
	}
	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainAccessTokenWithClientCredentials(clientAuthSequence);
	}

}
