package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consent-api-test-negative",
	displayName = "Runs various negative tests",
	summary = "Runs various negative tests\n" +
		"\u2022 Creates a Consent with only RESOURCES_READ permissions\n" +
		"\u2022 Expects a failure with a 400 Error\n" +
		"\u2022 Creates consent with incomplete credit card permission group (\"CREDIT_CARDS_ACCOUNTS_BILLS_READ\",\"CREDIT_CARDS_ACCOUNTS_READ\",\"RESOURCES_READ\")\n" +
		"\u2022 Expects failure with a 400 Error\n" +
		"\u2022 Creates consent with incomplete account permission group 1 (\"ACCOUNTS_READ\")\n" +
		"\u2022 Expects failure with a 400 Error\n" +
		"\u2022 Creates consent with incomplete account permission group 2 (\"ACCOUNTS_READ\",\"RESOURCES_READ\")\n" +
		"\u2022 Expects failure with a 400 Error\n" +
		"\u2022 Creates consent with incomplete Limits & Credit Operations Contract Data permissions group (\"RESOURCES_READ\",\"LOANS_READ\",\"LOANS_WARRANTIES_READ\",\n" +
		"\"LOANS_SCHEDULED_INSTALMENTS_READ\",\"LOANS_PAYMENTS_READ\",\"FINANCINGS_READ\",\"FINANCINGS_WARRANTIES_READ\",\"FINANCINGS_SCHEDULED_INSTALMENTS_READ\",\n" +
		"\"FINANCINGS_PAYMENTS_READ\",\"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\",\"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\",\n" +
		"\"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\",\"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\",\"INVOICE_FINANCINGS_READ\",\n" +
		"\"INVOICE_FINANCINGS_WARRANTIES_READ\",\"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\",\"INVOICE_FINANCINGS_PAYMENTS_READ\",\"RESOURCES_READ\")\n" +
		"\u2022 Expects failure with a 400 Error\n" +
		"\u2022 Creates consent with non existente permission group 2 (\"BAD_PERMISSION\")\n" +
		"\u2022 Expects failure with a 400 Error\n" +
		"\u2022 Creates consent api request for DateTime greater than 1 year from now\n" +
		"\u2022 Expects failure with a 400 Error\n" +
		"\u2022 Creates consent api request for DateTime in the past\n" +
		"\u2022 Expects failure with a 400 Error\n" +
		"\u2022 Creates consent api request for DateTime poorly formed.\n" +
		"\u2022 Expects failure with a 400 Error",
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
public class ConsentApiNegativeTests extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void runTests() {

		validateBadPermission(SetUpResourceReadOnlyPermissions.class, "Resource read only");
		validateBadPermission(SetUpCustomerPersonalIdOnlyPermissions.class, "Customers personal identification permissions only");
		validateBadPermission(SetUpIncompleteCreditCardPermissions.class, "incomplete credit card permission group");
		validateBadPermission(SetUpIncompleteAccountPermissions.class, "incomplete account permission group");
		validateBadPermission(SetUpSlightlyLessIncompleteAccountPermissions.class, "less incomplete account permission group");
		//validateBadPermission(SetUpIncompleteCreditOperationsPermissions.class, "incomplete Credit Operations Contract Data permission group");
		validateBadPermission(SetUpIncompleteComboPermissions.class, "incomplete combination of Limits & Credit Operations Contract Data permission groups");
		validateBadPermission(SetUpNonExistentPermissions.class,"non-existent permission group");

		validateBadExpiration(ConsentExpiryDateTimeGreaterThanAYear.class, "DateTime greater than 1 year from now");
		validateBadExpiration(ConsentExpiryDateTimeInThePast.class, "DateTime in the past");
		validateBadExpiration(ConsentExpiryDateTimePoorlyFormed.class, "DateTime poorly formed");

	}

	private void validateBadPermission(Class<? extends Condition> setupClass, String description) {
		String logMessage = String.format("Check for HTTP 400 response from consent api request for %s", description);
		runInBlock(logMessage, () -> {
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			callAndStopOnFailure(AddConsentScope.class);
			call(sequenceOf(
				condition(setupClass),
				sequence(PostConsentWithBadRequestSequence.class)
			));
		});
	}

	private void validateBadExpiration(Class<? extends Condition> setupClass, String description) {
		String logMessage = String.format("Check for HTTP 400 response from consent api request for %s.", description);
		runInBlock(logMessage, () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
			callAndStopOnFailure(setupClass);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class);
			callAndStopOnFailure(EnsureResponseCodeWas400.class);
		});
	}

}
