package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.testmodules;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.LoansContractSelector;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.PrepareUrlForFetchingLoanContractInstallmentsResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.PrepareUrlForFetchingLoanContractPaymentsResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.PrepareUrlForFetchingLoanContractResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.PrepareUrlForFetchingLoanContractWarrantiesResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.PrepareUrlForLoansRoot;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.ProvideIncorrectPermissionsForLoansApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddLoansScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddOpenIdScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallProtectedResourceExpectingFailureSequence;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas403;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "loans-api-wrong-permissions-test",
	displayName = "Ensures API resource cannot be called with wrong permissions",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test",
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
public class LoansApiWrongPermissionsTestModule extends AbstractPermissionsCheckingFunctionalTestModule {

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(AddLoansScope.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
	}

	@Override
	protected void preFetchResources() {
		callAndStopOnFailure(LoansContractSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingLoanContractResource.class);
		preCallProtectedResource("Fetch Loans Contracts");
		callAndStopOnFailure(PrepareUrlForFetchingLoanContractInstallmentsResource.class);
		preCallProtectedResource("Fetch Loans Contract list");
		callAndStopOnFailure(PrepareUrlForFetchingLoanContractWarrantiesResource.class);
		preCallProtectedResource("Fetch Loans Warranties");
		callAndStopOnFailure(PrepareUrlForFetchingLoanContractPaymentsResource.class);
		preCallProtectedResource("Fetch Loans Payments");
	}

	@Override
	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForLoansApi.class);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {
		runInBlock("Ensure we cannot call the Loans root API", () -> {
			callAndStopOnFailure(PrepareUrlForLoansRoot.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Loans resource API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Loans contract warranties API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractWarrantiesResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Loans contract payments API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractPaymentsResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the  Loans contract instalments API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractInstallmentsResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
