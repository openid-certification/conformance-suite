package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.ContractInstallmentsResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.ContractResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.ContractGuaranteesResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.ContractPaymentsValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.GetLoansResponseValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestModule;

@PublishTestModule(
	testName = "loans-api-test",
	displayName = "Validate structure of all loans API resources",
	summary = "Validates the structure of all loans API resources",
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
public class LoansApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddOpenIdScope.class);
		//callAndStopOnFailure(AddScopesForLoansApi.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
	}

	@Override
	protected void validateResponse() {

		runInBlock("Validate loans root response", () -> {
			callAndStopOnFailure(GetLoansResponseValidator.class);
		});

		runInBlock("Validate loans contract response", () -> {
			callAndStopOnFailure(LoansContractSelector.class);
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(ContractResponseValidator.class);
		});

		runInBlock("Validate loans contract warranties response", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractWarrantiesResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(ContractGuaranteesResponseValidator.class);
		});

		runInBlock("Validate loans contract payments response", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractPaymentsResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(ContractPaymentsValidator.class);
		});

		runInBlock("Validate loans contract instalments response", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractInstallmentsResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(ContractInstallmentsResponseValidator.class);
		});

	}

}
