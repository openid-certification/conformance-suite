package net.openid.conformance.openinsurance.testmodule.consents.v1;


import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetCustomCustomerIdentifications;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.consent.v1.OpenBankingBrazilPreAuthorizationConsentApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.GenerateRefreshTokenRequest;
import net.openid.conformance.openbanking_brasil.testmodules.v2.GenerateRefreshAccessTokenSteps;
import net.openid.conformance.openinsurance.testmodule.support.EnsureTokenResponseWas403;
import net.openid.conformance.openinsurance.testmodule.support.OpinPreAuthorizationConsentApi;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;


@PublishTestModule(
	testName = "opin-consents-api-delete-test",
	displayName = "Makes sure that after consent has been deleted no more tokens can be issued with the related refresh token ",
	summary = "Makes sure that after consent has been deleted no more tokens can be issued with the related refresh token \n" +
		"\u2022 Creates a Consent with all of the existing permissions, selecting either business or customer, depending on what was selected on the test configuration \n" +
		"\u2022 Redirects the user\n" +
		"\u2022 Calls the Token endpoint using the authorization code flow\n" +
		"\u2022 Makes sure a valid access token has been created\n" +
		"\u2022 Calls the Protected Resource endpoint to make sure a valid access token has been created\n" +
		"\u2022 Call the DELETE Consents API\n" +
		"\u2022 Calls the Token endpoint to issue a new access token\n" +
		"\u2022 Expects the test to return a 403 - Forbidden\n",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
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
		"resource.consentUrl",
		"resource.brazilCpf",
		"consent.productType"
	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class OpinConsentsApiDeleteTestModule extends AbstractFunctionalTestModule {
	protected ClientAuthType clientAuthType;

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildCustomCustomersConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void validateClientConfiguration() {
		super.validateClientConfiguration();
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(AddConsentScope.class);
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		OpinConsentPermissionsBuilder permissionsBuilder = new OpinConsentPermissionsBuilder(env,getId(),eventLog,testInfo,executionManager);
		permissionsBuilder.addPermissionsGroup(PermissionsGroup.ALL);

		String productType = env.getString("config", "consent.productType");
		if (!Strings.isNullOrEmpty(productType) && productType.equals("business")) {
			permissionsBuilder.removePermissionsGroups(PermissionsGroup.CUSTOMERS_PERSONAL);
		}
		if (!Strings.isNullOrEmpty(productType) && productType.equals("personal")) {
			permissionsBuilder.removePermissionsGroups(PermissionsGroup.CUSTOMERS_BUSINESS);
		}

		permissionsBuilder.build();
		clientAuthType = getVariant(ClientAuthType.class);
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		env.putString("proceed_with_test", "true");
		ConditionSequence preauthSteps  = new OpinPreAuthorizationConsentApi(addTokenEndpointClientAuthentication);

		return preauthSteps;
	}

	@Override
	protected void requestProtectedResource(){

		eventLog.startBlock("Try calling protected resource after user authentication");
		callAndStopOnFailure(PrepareToGetCustomCustomerIdentifications.class);
		callAndStopOnFailure(SaveProtectedResourceAccessToken.class);
		callAndStopOnFailure(CallProtectedResource.class);
		callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
		eventLog.endBlock();

		eventLog.startBlock("Deleting consent");
		callAndContinueOnFailure(LoadConsentsAccessToken.class);
		callAndContinueOnFailure(PrepareToDeleteConsent.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareToFetchConsentRequest.class);
		callAndStopOnFailure(IgnoreResponseError.class);
		callAndStopOnFailure(SetResponseBodyOptional.class);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(ConsentWasRejectedOrDeleted.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureConsentWasRejected.class, Condition.ConditionResult.WARNING);
		callAndStopOnFailure(LoadProtectedResourceAccessToken.class);

		eventLog.startBlock("Try calling protected resource after consent is deleted");
		callAndStopOnFailure(CallProtectedResource.class);
		callAndContinueOnFailure(EnsureResponseCodeWas401.class, Condition.ConditionResult.FAILURE);

		eventLog.startBlock("Trying issuing a refresh token");
		callTokenEndpointRefreshToken();
	}

	private void callTokenEndpointRefreshToken(){
		GenerateRefreshAccessTokenSteps refreshAccessTokenSteps = new GenerateRefreshAccessTokenSteps(clientAuthType);
		call(refreshAccessTokenSteps);
	}

	@Override
	protected void validateResponse() {}
}
