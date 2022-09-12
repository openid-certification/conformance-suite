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
	summary = "Validates the structure of all consent API resources in scenarios where it should return a 400 error due to incomplete permissions groups:\n" +
		"\u2022 Call the POST consent API with different scenarios\n" +
		"\u2022 Only RESOURCES_READ permissions\n" +
		"\u2022 Incomplete Pension Risk permission group\n" +
		"\u2022 Incomplete Capitalization Titles permission group (no “claim” permission)\n" +
		"\u2022 Incomplete Patrimonial permission group  (no  “premium” permission)\n" +
		"\u2022 Incomplete Nuclear permission group (no “policy-info” permission)\n" +
		"\u2022 Incomplete Person permission group (no “/” permission)\n" +
		"\u2022 Non-existent permission group 2 (\"BAD_PERMISSION\")\n" +
		"\u2022 API request for DateTime greater than 1 year from now\n" +
		"\u2022 API request for DateTime in the past\n" +
		"\u2022 API request for DateTime poorly formed\n",
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
