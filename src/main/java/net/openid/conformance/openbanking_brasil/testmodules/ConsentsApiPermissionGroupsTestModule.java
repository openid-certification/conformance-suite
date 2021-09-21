package net.openid.conformance.openbanking_brasil.testmodules;

import java.lang.reflect.Array;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "consent-api-test-permission-groups",
	displayName = "Validate that consent API accepts the consent groups",
	summary = "Validate that consent API accepts the consent groups",
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
public class ConsentsApiPermissionGroupsTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

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
		String logMessage = String.format("Validate consent api request for '%s' permission group(s)", name);
		runInBlock(logMessage, () -> {

			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			env.putString("consent_permissions", String.join(" ", permissions));
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.SUCCESS);

			if (!env.getString("resource_endpoint_response").equals("{}")) {
				passed = true;
				callAndStopOnFailure(ValidateRequestedPermissionsAreNotWidened.class, Condition.ConditionResult.FAILURE);
			} else {
				callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.WARNING);
				callAndStopOnFailure(EnsureResponseCodeWas422.class, Condition.ConditionResult.FAILURE);
			}

		});
	}
}
