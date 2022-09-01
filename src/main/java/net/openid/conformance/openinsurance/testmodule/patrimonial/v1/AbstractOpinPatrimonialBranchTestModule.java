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
import net.openid.conformance.variant.VariantNotApplicable;

@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = {"openbanking_uk", "plain_fapi", "consumerdataright_au"})
public class AbstractOpinPatrimonialBranchTestModule extends AbstractOBBrasilFunctionalTestModule {

	private OpinConsentPermissionsBuilder permissionsBuilder;

	private PatrimonialBranches branch;

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

		repeatSequence(() -> new PollForPatrimonialBranches());
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

	void setBranch(PatrimonialBranches branch) {
		this.branch = branch;
	}

	PatrimonialBranches getBranch() {
		return this.branch;
	}
}
