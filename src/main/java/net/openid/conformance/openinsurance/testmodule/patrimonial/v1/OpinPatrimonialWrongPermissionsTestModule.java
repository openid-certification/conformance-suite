package net.openid.conformance.openinsurance.testmodule.patrimonial.v1;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.account.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialClaimValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialPremiumValidatorV1;
import net.openid.conformance.testmodule.PublishTestModule;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@PublishTestModule(
	testName = "opin-patrimonial-api-wrong-permissions-test",
	displayName = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Patrimonial API (“DAMAGES_AND_PEOPLE_PATRIMONIAL_READ”, “DAMAGES_AND_PEOPLE_PATRIMONIAL_POLICYINFO_READ”, “DAMAGES_AND_PEOPLE_PATRIMONIAL_PREMIUM_READ”, “DAMAGES_AND_PEOPLE_PATRIMONIAL_CLAIM_READ”,  “RESOURCES_READ”)\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consent API \n" +
		"\u2022 Calls GET Patrimonial “/” API\n" +
		"\u2022 Expects 200 - Fetches one of the Policy IDs returned \n" +
		"\u2022 Calls GET Patrimonial policy-Info API specifying n Policy ID\n" +
		"\u2022 Expects 200 - Validate all the fields \n" +
		"\u2022 Calls GET patrimonial premium API specifying n Policy ID\n" +
		"\u2022 Expects 200- Validate all the fields\n" +
		"\u2022 Calls GET patrimonial claim API specifying n Policy ID\n" +
		"\u2022 Expects 200- Validate all the fields \n" +
		"\u2022 Call the POST Consents API with only either the customer’s business or the customer’s personal PERMISSIONS, depending on what option has been selected by the user on the configuration field\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well\n" +
		"\u2022 Calls GET Patrimonial “/” API\n"+
		"\u2022 Expects a 403 response  - Validate error response \n" +
		"\u2022 Calls GET Patrimonial policy-Info API specifying n Policy ID\n" +
		"\u2022 Expects a 403 response  - Validate error response \n" +
		"\u2022 Calls GET patrimonial premium API specifying an Policy ID\n" +
		"\u2022 Expects a 403 response  - Validate error response \n" +
		"\u2022 Calls GET patrimonial claim API specifying an Policy ID\n" +
		"\u2022 Expects a 403 response  - Validate error response",
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
public class OpinPatrimonialWrongPermissionsTestModule extends AbstractPermissionsCheckingFunctionalTestModule {

	private OpinConsentPermissionsBuilder permissionsBuilder;

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildPatrimonialConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(AddPatrimonialScope.class);

		permissionsBuilder = new OpinConsentPermissionsBuilder(env,getId(),eventLog,testInfo,executionManager);
		permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.DAMAGES_AND_PEOPLE_PATRIMONIAL);
		permissionsBuilder.build();
	}

	@Override
	protected void preFetchResources() {

		callAndStopOnFailure(PolicyIDSelector.class);

		callAndStopOnFailure(PrepareUrlForFetchingPatrimonialPolicyInfo.class);
		preCallProtectedResource("Fetch Patrimonial Policy Info");

		callAndStopOnFailure(PrepareUrlForFetchingPatrimonialPremium.class);
		preCallProtectedResource("Fetch Patrimonial Premium");

		callAndStopOnFailure(PrepareUrlForFetchingPatrimonialClaim.class);
		preCallProtectedResource("Fetch Patrimonial Claim");

	}

	@Override
	protected void prepareIncorrectPermissions() {
		String productType = env.getString("config", "consent.productType");

		if(Strings.isNullOrEmpty(productType) && productType.equals("business")) {
			permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.CUSTOMERS_BUSINESS);
		} else {
			permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.CUSTOMERS_PERSONAL);
		}
		permissionsBuilder.build();
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {

		callAndStopOnFailure(BuildPatrimonialConfigResourceUrlFromConsentUrl.class);

		runInBlock("Ensure we cannot call the Patrimonial root API", () -> {
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Patrimonial policy-Info API specifying a Policy ID", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingPatrimonialPolicyInfo.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Patrimonial premium API specifying an Policy ID", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingPatrimonialPremium.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Patrimonial claim API specifying an Policy ID", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingPatrimonialClaim.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

	}

}
