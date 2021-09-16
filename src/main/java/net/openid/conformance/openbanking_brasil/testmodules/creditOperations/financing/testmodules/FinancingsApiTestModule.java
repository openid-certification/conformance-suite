package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.InvoiceFinancingContractInstallmentsResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.FinancingContractResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.FinancingGuaranteesResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.FinancingPaymentsResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.FinancingResponseValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.AddScopesForFinancingsApi;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.FinancingContractSelector;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.PrepareUrlForFetchingFinancingContractInstallmentsResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.PrepareUrlForFetchingFinancingContractPaymentsResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.PrepareUrlForFetchingFinancingContractResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.PrepareUrlForFetchingFinancingContractWarrantiesResource;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "financings-api-test",
	displayName = "Validate structure of all financing API resources",
	summary = "Validates the structure of all financing API resources",
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
public class FinancingsApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(AddScopesForFinancingsApi.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
	}

	@Override
	protected void validateResponse() {

		runInBlock("Validate financing root response", () -> {
			callAndStopOnFailure(FinancingResponseValidator.class);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
		});

		runInBlock("Validate financing contract response", () -> {
			callAndStopOnFailure(FinancingContractSelector.class);
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(FinancingContractResponseValidator.class);
			callAndStopOnFailure(LogKnownIssue.class,"BCLOG-F02-173","BCLOG-F02-175");
		});

		runInBlock("Validate financing contract warranties response", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractWarrantiesResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(FinancingGuaranteesResponseValidator.class);
		});

		runInBlock("Validate financing contract payments response", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractPaymentsResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(FinancingPaymentsResponseValidator.class);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
		});

		runInBlock("Validate financing contract instalments response", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractInstallmentsResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(InvoiceFinancingContractInstallmentsResponseValidator.class);
			callAndStopOnFailure(LogKnownIssue.class,"BCLOG-F02-174","BCLOG-F02-176");
		});

	}

}
