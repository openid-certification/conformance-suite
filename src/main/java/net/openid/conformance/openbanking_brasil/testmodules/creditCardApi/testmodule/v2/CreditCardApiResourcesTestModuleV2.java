package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CardAccountsDataResponseResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.resourcesAPI.EnumResourcesStatus;
import net.openid.conformance.openbanking_brasil.resourcesAPI.EnumResourcesType;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v2.PrepareUrlForResourcesCallV2;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v2.ResourcesResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "credit-card-resources-api-test-v2",
	displayName = "Validate structure of Credit Card API and Resources API resources V2",
	summary = "Makes sure that the Resource API and the API that is the scope of this test plan are returning the same available IDs\n" +
		"\u2022Create a consent with all the permissions needed to access the tested API\n" +
		"\u2022 Expects server to create the consent with 201\n" +
		"\u2022 Redirect the user to authorize at the financial institution\n" +
		"\u2022 Call the tested resource API V2\n" +
		"\u2022 Expect a success - Validate the fields of the response and Make sure that an id is returned - Fetch the id provided by this API\n" +
		"\u2022 Call the resources API V2\n" +
		"\u2022 Expect a success - Validate the fields of the response that are marked as AVAILABLE are exactly the ones that have been returned by the tested API",
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
public class CreditCardApiResourcesTestModuleV2 extends CreditCardApiTestModuleV2 {


	private static final String API_RESOURCE_ID = "creditCardAccountId";
	private static final String RESOURCE_TYPE = EnumResourcesType.CREDIT_CARD_ACCOUNT.name();
	private static final String RESOURCE_STATUS = EnumResourcesStatus.AVAILABLE.name();

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildCreditCardsAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddResourcesScope.class);
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(CardAccountsDataResponseResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		env.putString("apiIdName", API_RESOURCE_ID);
		callAndStopOnFailure(ExtractAllSpecifiedApiIds.class);
		callAndStopOnFailure(PrepareUrlForResourcesCallV2.class);
		preCallProtectedResource("Call Resources API");

		runInBlock("Validate Resources response V2", () -> {
			callAndStopOnFailure(ResourcesResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(sequence(ValidateSelfEndpoint.class));
		});

		eventLog.startBlock("Compare active resourceId's with API resources V2");
		env.putString("resource_type", RESOURCE_TYPE);
		env.putString("resource_status", RESOURCE_STATUS);

		callAndStopOnFailure(ExtractResourceIdOfResourcesWithSpecifiedTypeAndStatus.class);
		callAndStopOnFailure(CompareResourceIdWithAPIResourceId.class);
		eventLog.endBlock();
	}


}
