package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditCard.CardAccountsDataResponseResponseValidator;
import net.openid.conformance.openbanking_brasil.resourcesAPI.EnumResourcesType;
import net.openid.conformance.openbanking_brasil.resourcesAPI.PrepareUrlForResourcesCall;
import net.openid.conformance.openbanking_brasil.resourcesAPI.ResourcesResponseValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "credit-card-resources-api-test",
	displayName = "Validate structure of Credit Card API and Resources API resources",
	summary = "Validate structure of Credit Card API and Resources API resources\n" +
		"\u2022 Creates a Consent with the complete set of the credit cards permission group ([\"CREDIT_CARDS_ACCOUNTS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ\", \"CREDIT_CARDS_ACCOUNTS_LIMITS_READ\", \"CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ\", \"RESOURCES_READ\"])\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Credit Cards Accounts API \n" +
		"\u2022 Expects a 200 response. Fetches all resources from the response\n" +
		"\u2022 Calls GET Resources API. Expects a 200 response\n" +
		"\u2022 Fetches all active and corresponding with Credit Cards Accounts API resources from the Resources API response\n" +
		"\u2022 Compares Credit Cards Accounts API fetched resources with Resources API fetched resources",
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
		"resource.resourceUrl"
	}
)
public class CreditCardApiResourcesTestModule extends CreditCardApiTestModule {


	private static final String API_RESOURCE_ID = "creditCardAccountId";
	private static final String RESOURCE_TYPE = EnumResourcesType.CREDIT_CARD_ACCOUNT.name();


	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(CardAccountsDataResponseResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		env.putString("apiIdName", API_RESOURCE_ID);
		callAndStopOnFailure(ExtractAllSpecifiedApiIds.class);
		callAndStopOnFailure(PrepareUrlForResourcesCall.class);
		preCallProtectedResource("Call Resources API");

		runInBlock("Validate Resources response", () -> {
			callAndStopOnFailure(ResourcesResponseValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(sequence(ValidateSelfEndpoint.class));
		});

		eventLog.startBlock("Compare active resourceId's with API resources");
		env.putString("resource_type", RESOURCE_TYPE);
		callAndStopOnFailure(ExtractResourceIdOfActiveResources.class);
		callAndStopOnFailure(CompareResourceIdWithAPIResourceId.class);
		eventLog.endBlock();
	}


}
