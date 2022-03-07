package net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareAllCustomerPersonalRelatedConsentsForHappyPathTest;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetPersonalFinancialRelationships;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetPersonalIdentifications;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetPersonalQualifications;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.ProvideIncorrectPermissionsForCustomerPersonalApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallProtectedResourceExpectingFailureSequence;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas403;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.condition.Condition;

@PublishTestModule(
	testName = " Customer-Personal-api-wrong-permissions-test",
	displayName = "Ensures API resource cannot be called with wrong permissions",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test\n" +
		"\u2022 Creates a Consent will the customer personal permissions ( \"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\",\"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ\",\"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Checks all of the fields sent on the consent API are specification compliant\n" +
		"\u2022 Calls GET Personal Qualifications resources\n" +
		"\u2022 Expects a success 200\n" +
		"\u2022 Creates a Consent with all the permissions but the Customer Personal ones - (\"ACCOUNTS_READ\", \"ACCOUNTS_BALANCES_READ\", \"RESOURCES_READ\", \"ACCOUNTS_OVERDRAFT_LIMITS_READ\",  \"ACCOUNTS_TRANSACTIONS_READ\", \"CREDIT_CARDS_ACCOUNTS_READ\", \"CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ\", \"CREDIT_CARDS_ACCOUNTS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ\", \"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ\")\n" +
		"\u2022 Calls the GET Customer Personal Qualification Resource \n" +
		"\u2022 Expects a 403\n" +
		"\u2022 Calls the GET Customer Personal Financial Relations Resource \n" +
		"\u2022 Expects a 403\n",
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
public class CustomerPersonalWrongPermissionsTestModule extends AbstractPermissionsCheckingFunctionalTestModule {

	@Override
	protected void preFetchResources() {
		callAndStopOnFailure(AddScopesForCustomerApi.class);
	}

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(PrepareAllCustomerPersonalRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(PrepareToGetPersonalQualifications.class);
	}

	@Override
	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForCustomerPersonalApi.class);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {
		runInBlock("Ensure we cannot call the  Customer Personal Qualification", () -> {
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Customer Personal Identifications", () -> {
			callAndStopOnFailure(PrepareToGetPersonalIdentifications.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Customer Personal Financial-relations", () -> {
			callAndStopOnFailure(PrepareToGetPersonalFinancialRelationships.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
