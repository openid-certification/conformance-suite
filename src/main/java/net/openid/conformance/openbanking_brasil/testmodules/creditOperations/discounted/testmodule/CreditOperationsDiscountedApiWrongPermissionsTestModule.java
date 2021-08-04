package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.testmodule;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.*;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.CreditDiscountedCreditRightsSelector;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForDiscountedRoot;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForFetchingCreditDiscountedCreditRightsContract;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForFetchingCreditDiscountedCreditRightsContractGuarantees;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForFetchingCreditDiscountedCreditRightsContractInstalments;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForFetchingCreditDiscountedCreditRightsContractPayments;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "discounted-credit-rights-wrong-permissions-test",
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
public class CreditOperationsDiscountedApiWrongPermissionsTestModule extends AbstractPermissionsCheckingFunctionalTestModule {

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
	}

	@Override
	protected void preFetchResources() {
		callAndStopOnFailure(CreditDiscountedCreditRightsSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContract.class);
		preCallProtectedResource("Discounted Credit Rights - Contract");
		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractGuarantees.class);
		preCallProtectedResource("Discounted Credit Rights - Contract Guarantees");
		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractPayments.class);
		preCallProtectedResource("Discounted Credit Rights - Contract Payments");
		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractInstalments.class);
		preCallProtectedResource("Discounted Credit Rights - Contract Instalments");
	}

	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForCreditCardApi.class);
	}

	protected void requestResourcesWithIncorrectPermissions() {
		runInBlock("Ensure we cannot call the Discounted Credit Rights root", () -> {
			callAndStopOnFailure(PrepareUrlForDiscountedRoot.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Discounted Credit Rights - Contract", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContract.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Discounted Credit Rights - Contract Guarantees", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractGuarantees.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Discounted Credit Rights - Contract Payments", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractPayments.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Discounted Credit Rights - Contract Instalments", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractInstalments.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
