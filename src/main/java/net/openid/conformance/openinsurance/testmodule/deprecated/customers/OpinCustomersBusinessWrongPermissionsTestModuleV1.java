package net.openid.conformance.openinsurance.testmodule.deprecated.customers;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddDummyPersonalProductTypeToConfig;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildBusinessCustomersConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallProtectedResourceExpectingFailureSequence;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas403;
import net.openid.conformance.openinsurance.testmodule.support.AddScopesForCustomerApi;
import net.openid.conformance.openinsurance.testmodule.support.PrepareAllCustomerBusinessRelatedConsentsForHappyPathTest;
import net.openid.conformance.openinsurance.testmodule.support.PrepareToGetBusinessComplimentaryInformation;
import net.openid.conformance.openinsurance.testmodule.support.PrepareToGetBusinessIdentifications;
import net.openid.conformance.openinsurance.testmodule.support.PrepareToGetBusinessQualifications;
import net.openid.conformance.openinsurance.testmodule.support.ProvideIncorrectPermissionsForCustomerBusinessApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "opin-customers-business-api-wrong-permissions-test-v1",
	displayName = "Ensures API resource cannot be called with wrong permissions V1",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test\n" +
		"\u2022 Creates a Consent with the customer business permissions (\"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\",\"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ\",\"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Check all of the fields sent on the consent API is spec compliant \n" +
		"\u2022 Calls GET Personal Qualifications resource V1\n" +
		"\u2022 Expects a success 200\n" +
		"\u2022 Creates a Consent with all the permissions but the Customer Business ones - ( \"ACCOUNTS_READ\", \"ACCOUNTS_BALANCES_READ\", \"RESOURCES_READ\", \"ACCOUNTS_OVERDRAFT_LIMITS_READ\", \"ACCOUNTS_TRANSACTIONS_READ\", \"CREDIT_CARDS_ACCOUNTS_READ\", \"CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ\", \"CREDIT_CARDS_ACCOUNTS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ\", \"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\")\n" +
		"\u2022 Calls the GET Customer Business Qualifications Resource V1\n" +
		"\u2022 Expects a 403\n" +
		"\u2022 Calls the GET Customer Business Complimentary-Information Resource V1\n" +
		"\u2022 Expects a 403",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)
public class OpinCustomersBusinessWrongPermissionsTestModuleV1 extends AbstractPermissionsCheckingFunctionalTestModule {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildBusinessCustomersConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		//Simple UI fix
		callAndStopOnFailure(AddDummyPersonalProductTypeToConfig.class);
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected void preFetchResources() {
		callAndStopOnFailure(AddScopesForCustomerApi.class);
	}

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(PrepareAllCustomerBusinessRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(PrepareToGetBusinessQualifications.class);
	}

	@Override
	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForCustomerBusinessApi.class);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {
		runInBlock("Ensure we cannot call the Customer Business Qualifications V1", () -> {
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Customer Business Identifications V1", () -> {
			callAndStopOnFailure(PrepareToGetBusinessIdentifications.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Customer Business Complimentary-Information V1", () -> {
			callAndStopOnFailure(PrepareToGetBusinessComplimentaryInformation.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}