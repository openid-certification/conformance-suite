package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddOpenIdScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallProtectedResourceExpectingFailureSequence;
import net.openid.conformance.openbanking_brasil.testmodules.support.CardAccountSelector;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas403;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareAllCreditCardRelatedConsentsForHappyPathTest;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareUrlForFetchingAccountResource;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "CreditCard-Api-wrong-permissions-test",
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
public class CreditCardApiWrongPermissionsTestModule extends AbstractPermissionsCheckingFunctionalTestModule {

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(PrepareAllCreditCardRelatedConsentsForHappyPathTest.class);
	}

	@Override
	protected void preFetchResources() {
		callAndStopOnFailure(CardAccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
		preCallProtectedResource("Fetch card bill Account");
		callAndStopOnFailure(PrepareUrlForFetchingCardLimits.class);
		preCallProtectedResource("Fetch CreditCard Limits");
		callAndStopOnFailure(PrepareUrlForFetchingCardTransactions.class);
		preCallProtectedResource("Fetch CreditCard Transactions");
		callAndStopOnFailure(CardBillSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingCardBills.class);
		preCallProtectedResource("Fetch CreditCard Bills");
		callAndStopOnFailure(PrepareUrlForFetchingBillTransactionResource.class);
		preCallProtectedResource("Fetch CreditCard Bills Transaction");
	}

	@Override
	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForCreditCardApi.class);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {
		//TODO: need to check why CreditCardRoot not returning 403 on a mock bank
		runInBlock("Ensure we cannot call the CreditCard Root API", () -> {
			callAndStopOnFailure(PrepareUrlForCreditCardRoot.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditCard Account API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditCard Bill API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCardBills.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditCard Bill Transaction API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingBillTransactionResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditCard Limits API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCardLimits.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the  CreditCard Transactions API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCardTransactions.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
