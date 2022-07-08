package net.openid.conformance.openbanking_brasil.testmodules.v2.consents;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallConsentApiWithBearerToken;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas422;
import net.openid.conformance.openbanking_brasil.testmodules.support.IgnoreResponseError;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToPostConsentRequest;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetContentTypeApplicationJson;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateRequestedPermissionsAreNotWidened;
import net.openid.conformance.openbanking_brasil.testmodules.support.arrayUtils;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "consent-api-test-permission-groups-v2",
	displayName = "Validate that consent API V2 accepts the consent groups",
	summary = "Validates that consent API V2 accepts the consent groups\n" +
		"\u2022 Creates a series of consent requests with valid permissions group and expect for each of them a 201 to be returned by the server\n" +
		"\u2022 Validates consent API V2 request for 'Personal Registration Data' permission group(s)\n" +
		"\u2022 Validates consent API V2 request for 'Personal Additional Information' permission group(s)\n" +
		"\u2022 Validates consent API V2 request for 'Business Registration Data' permission group(s)\n" +
		"\u2022 Validates consent API V2 request for 'Business Additional Information' permission group(s)\n" +
		"\u2022 Validates consent API V2 request for 'Accounts Balances' permission group(s)\n" +
		"\u2022 Validates consent API V2 request for 'Accounts Limits' permission group(s)\n" +
		"\u2022 Validates consent API V2 request for 'Credit Card Invoices' permission group(s)\n" +
		"\u2022 Validates consent API V2 request for 'Credit Operations' permission group(s)\n" +
		"\u2022 Validates consent API V2 request for 'Balances & Credit Card Limits' permission group(s)\n" +
		"\u2022 Validates consent API V2 request for 'Accounts Transactions' permission group(s)\n" +
		"\u2022 Validates consent API V2 request for 'Credit Card Limits' permission group(s)\n" +
		"\u2022 Validates consent API V2 request for 'Credit Card Transactions' permission group(s)\n",
	profile = OBBProfile.OBB_PROFILE,
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
public class ConsentsApiPermissionGroupsTestModuleV2 extends AbstractClientCredentialsGrantFunctionalTestModule {

	private boolean passed = false;

	@Override
	protected void runTests() {
		passed = false;

		String[] personalRegistrationData = {"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ","RESOURCES_READ"};
		String[] personalAdditionalInfo = {"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ", "RESOURCES_READ"};
		String[] businessRegistrationData = {"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ", "RESOURCES_READ"};
		String[] businessAdditionalInfo = {"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ", "RESOURCES_READ"};
		String[] balances = {"ACCOUNTS_READ", "ACCOUNTS_BALANCES_READ", "RESOURCES_READ"};
		String[] limits = {"ACCOUNTS_READ", "ACCOUNTS_OVERDRAFT_LIMITS_READ", "RESOURCES_READ"};
		String[] extras = {"ACCOUNTS_READ", "ACCOUNTS_TRANSACTIONS_READ", "RESOURCES_READ"};
		String[] creditCardLimits = {"CREDIT_CARDS_ACCOUNTS_READ", "CREDIT_CARDS_ACCOUNTS_LIMITS_READ", "RESOURCES_READ"};
		String[] creditCardTransactions = {"CREDIT_CARDS_ACCOUNTS_READ", "CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ", "RESOURCES_READ"};
		String[] creditCardInvoices = {"CREDIT_CARDS_ACCOUNTS_READ", "CREDIT_CARDS_ACCOUNTS_BILLS_READ", "CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ", "RESOURCES_READ"};
		String[] creditOperationsContractData = {"LOANS_READ", "LOANS_WARRANTIES_READ", "LOANS_SCHEDULED_INSTALMENTS_READ", "LOANS_PAYMENTS_READ", "FINANCINGS_READ", "FINANCINGS_WARRANTIES_READ", "FINANCINGS_SCHEDULED_INSTALMENTS_READ", "FINANCINGS_PAYMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ", "INVOICE_FINANCINGS_READ", "INVOICE_FINANCINGS_WARRANTIES_READ", "INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ", "INVOICE_FINANCINGS_PAYMENTS_READ", "RESOURCES_READ"};
		String[] combo = arrayUtils.concatArrays(balances, creditCardLimits);

		validatePermissions(personalRegistrationData, "Personal Registration Data");
		validatePermissions(personalAdditionalInfo, "Personal Additional Information");
		validatePermissions(businessRegistrationData, "Business Registration Data");
		validatePermissions(businessAdditionalInfo, "Business Additional Information");
		validatePermissions(balances, "Balances");
		validatePermissions(limits, "Limits");
		validatePermissions(extras, "Extras");
		validatePermissions(creditCardLimits, "Credit Card Limits");
		validatePermissions(creditCardTransactions, "Credit Card Transactions");
		validatePermissions(creditCardInvoices, "Credit Card Invoices");
		validatePermissions(creditOperationsContractData, "Credit Operations");
		validatePermissions(combo, "Balances & Credit Card Limits");

		//If all validates returned a 422
		if (!passed) {
			throw new TestFailureException(getId(), "All resources returned a 422 when at least one set of permissions should have passed");
		}
	}


	private void validatePermissions(String[] permissions, String name) {
		String logMessage = String.format("Validate consent api v2 request for '%s' permission group(s)", name);
		runInBlock(logMessage, () -> {

			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			env.putString("consent_permissions", String.join(" ", permissions));
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndStopOnFailure(IgnoreResponseError.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.SUCCESS);

			if (!env.getString("resource_endpoint_response").equals("{}")) {
				passed = true;
				callAndStopOnFailure(ValidateRequestedPermissionsAreNotWidened.class, Condition.ConditionResult.FAILURE);
			} else {
				callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
				callAndStopOnFailure(EnsureResponseCodeWas422.class, Condition.ConditionResult.FAILURE);
			}

		});
	}
}
