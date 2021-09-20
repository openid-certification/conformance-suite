package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.testmodules;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.CreditAdvanceSelector;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.PrepareUrlForFetchingCreditAdvanceContractGuarantees;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.PrepareUrlForFetchingCreditAdvanceContractInstallments;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.PrepareUrlForFetchingCreditAdvanceContractPayments;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.PrepareUrlForFetchingCreditAdvanceContracts;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.PrepareUrlForFetchingCreditAdvanceRoot;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.ProvideIncorrectPermissionsForAdvancesApi;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.ProvideIncorrectPermissionsForLoansApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallProtectedResourceExpectingFailureSequence;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas403;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.condition.Condition;

@PublishTestModule(
	testName = "unarranged-overdraft-api-wrong-permissions-test",
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
public class CreditOperationsAdvancesApiWrongPermissionsTestModule extends AbstractPermissionsCheckingFunctionalTestModule {

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
	}

	@Override
	protected void preFetchResources() {
		callAndStopOnFailure(CreditAdvanceSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContracts.class);
		preCallProtectedResource("Fetch Loans Contracts");
		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractInstallments.class);
		preCallProtectedResource("Fetch Loans Contract list");
		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractGuarantees.class);
		preCallProtectedResource("Fetch Loans Warranties");
		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractPayments.class);
		preCallProtectedResource("Fetch Loans Payments");
	}

	@Override
	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForAdvancesApi.class);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {
		runInBlock("Ensure we cannot call the CreditOperation Advances root API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceRoot.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditOperation Advances Contracts API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContracts.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditOperation Advances warranties API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractGuarantees.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditOperation Advances payments API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractPayments.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the  CreditOperation Advances instalments API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractInstallments.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
