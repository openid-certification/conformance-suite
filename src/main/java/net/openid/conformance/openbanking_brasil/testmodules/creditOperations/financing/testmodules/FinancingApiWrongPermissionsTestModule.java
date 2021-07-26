package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.testmodules;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.AddScopesForFinancingsApi;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.FinancingContractSelector;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.PrepareUrlForFetchingFinancingContractInstallmentsResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.PrepareUrlForFetchingFinancingContractPaymentsResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.PrepareUrlForFetchingFinancingContractResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.PrepareUrlForFetchingFinancingContractWarrantiesResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.PrepareUrlForFinancingRoot;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.ProvideIncorrectPermissionsForFinancingApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddOpenIdScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallProtectedResourceExpectingFailureSequence;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas403;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "financing-api-wrong-permissions-test",
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
public class FinancingApiWrongPermissionsTestModule extends AbstractPermissionsCheckingFunctionalTestModule {

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(AddScopesForFinancingsApi.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
	}

	@Override
	protected void preFetchResources() {
		callAndStopOnFailure(FinancingContractSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingFinancingContractResource.class);
		preCallProtectedResource("Fetch Contracts");
		callAndStopOnFailure(PrepareUrlForFetchingFinancingContractInstallmentsResource.class);
		preCallProtectedResource("Fetch Financing Contract list");
		callAndStopOnFailure(PrepareUrlForFetchingFinancingContractWarrantiesResource.class);
		preCallProtectedResource("Fetch Warranties");
		callAndStopOnFailure(PrepareUrlForFetchingFinancingContractPaymentsResource.class);
		preCallProtectedResource("Fetch Payments");
	}

	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForFinancingApi.class);
	}

	protected void requestResourcesWithIncorrectPermissions() {
		runInBlock("Ensure we cannot call the Financing root API", () -> {
			callAndStopOnFailure(PrepareUrlForFinancingRoot.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Financing resource API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the financing contract warranties API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractWarrantiesResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the financing contract payments API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractPaymentsResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the  financing contract instalments API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractInstallmentsResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
