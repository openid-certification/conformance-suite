package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.GetLoansResponseValidator;
import net.openid.conformance.openbanking_brasil.resourcesAPI.EnumResourcesType;
import net.openid.conformance.openbanking_brasil.resourcesAPI.ResourcesResponseValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "loans-resources-api-test",
	displayName = "Validate structure of loans API and Resources API resources",
	summary = "Validate structure of loans API and Resources API resources\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Credit Operations API  (\"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Loans Contracts API\n" +
		"\u2022 Expects 201 - Fetches all returned ID's\n" +
		"\u2022 Calls GET Resources API. Expects a 200 response\n" +
		"\u2022 Fetches all active and corresponding with Loans Contracts API resources from the Resources API response\n" +
		"\u2022 Compares Loans Contracts API fetched resources with Resources API fetched resources",
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
public class LoansApiResourcesTestModule extends LoansApiTestModule {

	private static final String API_RESOURCE_ID = "contractId";
	private static final String RESOURCES_URL = "https://matls-api.mockbank.poc.raidiam.io/open-banking/resources/v1/resources";
	private static final String RESOURCE_TYPE = EnumResourcesType.LOAN.name();

	@Override
	protected void validateResponse() {

		runInBlock("Validate loans root response", () -> {
			callAndStopOnFailure(GetLoansResponseValidator.class);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(sequence(ValidateSelfEndpoint.class));
		});

		env.putString("apiIdName", API_RESOURCE_ID);
		callAndStopOnFailure(ExtractAllSpecifiedApiIds.class);
		env.putString("protected_resource_url", RESOURCES_URL);
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
