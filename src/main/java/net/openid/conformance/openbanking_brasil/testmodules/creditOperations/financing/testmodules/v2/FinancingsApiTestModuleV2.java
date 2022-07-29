package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.testmodules.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.v2.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "financings-api-test-v2",
	displayName = "Validate structure of all financing API resources V2",
	summary = "Validates the structure of all financing API resources V2\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Credit Operations API  (\"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Financings Contracts API V2\n" +
		"\u2022 Expects 200 - Fetches one of the IDs returned\n" +
		"\u2022 Calls GET Financings Contracts API V2 with ID \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Financings Warranties API V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Financings Payments API V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Financings Contracts Instalments API V2\n" +
		"\u2022 Expects 200\n",
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
		"consent.productType"

	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class FinancingsApiTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildFinancingsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(AddScopesForFinancingsApi.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
	}

	@Override
	protected void validateResponse() {

		runInBlock("Validate financing root response V2", () -> {
			callAndStopOnFailure(FinancingResponseValidatorV2.class);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(sequence(ValidateSelfEndpoint.class));
		});

		runInBlock("Validate financing contract response V2", () -> {
			callAndStopOnFailure(FinancingContractSelector.class);
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(FinancingContractResponseValidatorV2.class);
			callAndStopOnFailure(LogKnownIssue.class,"BCLOG-F02-173","BCLOG-F02-175");
		});

		runInBlock("Validate financing contract warranties response V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractWarrantiesResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(FinancingGuaranteesResponseValidatorV2.class);
		});

		runInBlock("Validate financing contract payments response V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractPaymentsResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(FinancingPaymentsResponseValidatorV2.class);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(sequence(ValidateSelfEndpoint.class));
		});

		runInBlock("Validate financing contract instalments response V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractInstallmentsResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(FinancingContractInstallmentsResponseValidatorV2.class);
			callAndStopOnFailure(LogKnownIssue.class,"BCLOG-F02-174","BCLOG-F02-176");
		});

	}

}
