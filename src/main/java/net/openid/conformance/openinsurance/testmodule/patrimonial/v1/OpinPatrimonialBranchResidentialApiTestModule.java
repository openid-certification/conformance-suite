package net.openid.conformance.openinsurance.testmodule.patrimonial.v1;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.account.PrepareUrlForFetchingPatrimonialClaim;
import net.openid.conformance.openbanking_brasil.testmodules.account.PrepareUrlForFetchingPatrimonialPolicyInfo;
import net.openid.conformance.openbanking_brasil.testmodules.account.PrepareUrlForFetchingPatrimonialPremium;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialClaimValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialListValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialPolicyInfoValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialPremiumValidatorV1;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "opin-patrimonial-api-test",
	displayName = "Validates the structure of all Patrimonial API resources",
	summary = "Validates the structure of all Patrimonial API resources\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Patrimonial API (“DAMAGES_AND_PEOPLE_PATRIMONIAL_READ”, “DAMAGES_AND_PEOPLE_PATRIMONIAL_POLICYINFO_READ”, “DAMAGES_AND_PEOPLE_AERONAUTICAL_PREMIUM_READ”, “DAMAGES_AND_PEOPLE_AERONAUTICAL_CLAIM_READ”,  “RESOURCES_READ”)\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Patrimonial “/” API\n" +
		"\u2022 Expects 201 - Fetches one of the Policy IDs returned\n" +
		"\u2022 Calls GET Patrimonial policy-Info API \n" +
		"\u2022 Expects 200 - Validate all the fields\n" +
		"\u2022 Calls GET patrimonial premium API \n" +
		"\u2022 Expects 200- Validate all the fields\n" +
		"\u2022 Calls GET patrimonial claim API \n" +
		"\u2022 Expects 200- Validate all the fields",
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
	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class OpinPatrimonialBranchResidentialApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	private OpinConsentPermissionsBuilder permissionsBuilder;

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildPatrimonialConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddPatrimonialScope.class);

		permissionsBuilder = new OpinConsentPermissionsBuilder(env,getId(),eventLog,testInfo,executionManager);
		permissionsBuilder.resetPermissions().addPermissionsGroup(PermissionsGroup.DAMAGES_AND_PEOPLE_PATRIMONIAL);
		permissionsBuilder.build();
	}

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(OpinInsurancePatrimonialListValidatorV1.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PolicyIDSelector.class);

		callAndStopOnFailure(PrepareUrlForFetchingPatrimonialPolicyInfo.class);
		preCallProtectedResource("Fetch Patrimonial Policy Info");
		callAndContinueOnFailure(OpinInsurancePatrimonialPolicyInfoValidatorV1.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(PrepareUrlForFetchingPatrimonialPremium.class);
		preCallProtectedResource("Fetch Patrimonial Premium");
		callAndContinueOnFailure(OpinInsurancePatrimonialPremiumValidatorV1.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(PrepareUrlForFetchingPatrimonialClaim.class);
		preCallProtectedResource("Fetch Patrimonial Claim");
		callAndContinueOnFailure(OpinInsurancePatrimonialClaimValidatorV1.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);

		call(new ValidateSelfEndpoint());

	}

}
