package net.openid.conformance.openinsurance.testmodule.v1.consents;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "opin-consent-api-test-negative",
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
		"\u2022 Creates consent with incomplete Limits & Credit Operations Contract Data permissions group (\"RESOURCES_READ\", \"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects failure with a 400 Error\n" +
		"\u2022 Creates consent with non-existent permission group 2 (\"BAD_PERMISSION\")\n" +
		"\u2022 Expects failure with a 400 Error\n" +
		"\u2022 Creates consent api request for DateTime greater than 1 year from now\n" +
		"\u2022 Expects failure with a 400 Error\n" +
		"\u2022 Creates consent api request for DateTime in the past\n" +
		"\u2022 Expects failure with a 400 Error\n" +
		"\u2022 Creates consent api request for DateTime poorly formed.\n" +
		"\u2022 Expects failure with a 400 Error",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"consent.productType"
	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class OpinConsentApiNegativeTests extends AbstractClientCredentialsGrantFunctionalTestModule {

	private OpinConsentPermissionsBuilder permissionsBuilder;
	@Override
	protected void runTests() {
		permissionsBuilder = new OpinConsentPermissionsBuilder(env,getId(),eventLog,testInfo,executionManager);

		permissionsBuilder.addPermissionsGroup(PermissionsGroup.RESOURCES).build();
		validateBadPermission("Resource read only");

		permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.PENSION_RISK)
				.removePermission("PENSION_RISK_READ").build();
		validateBadPermission("incomplete Pension Risk  permission group");

		permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.CAPITALIZATION_TITLES)
				.removePermission("CAPITALIZATION_TITLES_CLAIM_READ").build();
		validateBadPermission("incomplete Capitalization Titles permission group");


		permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.DAMAGES_AND_PEOPLE_PATRIMONIAL)
				.removePermission("DAMAGES_AND_PEOPLE_PATRIMONIAL_PREMIUM_READ").build();
		validateBadPermission( "incomplete Damages and People Patrimonial permission group");

		permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.DAMAGES_AND_PEOPLE_NUCLEAR)
			.removePermission("DAMAGES_AND_PEOPLE_NUCLEAR_POLICYINFO_READ").build();
		validateBadPermission( "incomplete Damages and People Nuclear permission group");

		permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.DAMAGES_AND_PEOPLE_PERSON)
			.removePermission("DAMAGES_AND_PEOPLE_PERSON_POLICYINFO_READ").build();
		validateBadPermission( "incomplete Damages and People Person permission group");

		permissionsBuilder.resetPermissions().set("BAD_PERMISSION").build();
		validateBadPermission("non-existent permission group");

		permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.ALL).build();
		validateBadExpiration(ConsentExpiryDateTimeGreaterThanAYear.class, "DateTime greater than 1 year from now");
		validateBadExpiration(ConsentExpiryDateTimeInThePast.class, "DateTime in the past");
		validateBadExpiration(ConsentExpiryDateTimePoorlyFormed.class, "DateTime poorly formed");

	}

	private void validateBadPermission(String description) {
		String logMessage = String.format("Check for HTTP 400 response from consent api request for %s", description);
		runInBlock(logMessage, () -> {
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			callAndStopOnFailure(AddConsentScope.class);
			call(sequence(PostConsentWithBadRequestSequence.class));
		});
	}

	private void validateBadExpiration(Class<? extends Condition> setupClass, String description) {
		String logMessage = String.format("Check for HTTP 400 response from consent api request for %s.", description);
		runInBlock(logMessage, () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(setupClass);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class);
			callAndStopOnFailure(EnsureResponseCodeWas400.class);
		});
	}

}
