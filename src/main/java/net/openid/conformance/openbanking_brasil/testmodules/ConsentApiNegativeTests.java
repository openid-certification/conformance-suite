package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consent-api-test-negative",
	displayName = "Runs various negative tests",
	summary = "Runs various negative tests",
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
		validateBadPermission(SetUpIncompleteCreditOperationsPermissions.class, "incomplete Credit Operations Contract Data permission group");
		validateBadPermission(SetUpIncompleteComboPermissions.class, "incomplete combination of Limits & Credit Operations Contract Data permission groups");
		validateBadPermission(SetUpNonExistentPermissions.class,"non-existent permission group");

		validateBadExpiration(ConsentExpiryDateTimeGreaterThanAYear.class, "DateTime greater than 1 year from now");
		validateBadExpiration(ConsentExpiryDateTimeInThePast.class, "DateTime in the past");
		validateBadExpiration(ConsentExpiryDateTimePoorlyFormed.class, "DateTime poorly formed");

	}

	private void validateBadPermission(Class<? extends Condition> setupClass, String description) {
		String logMessage = String.format("Check for HTTP 400 response from consent api request for %s", description);
		runInBlock(logMessage, () -> {

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
