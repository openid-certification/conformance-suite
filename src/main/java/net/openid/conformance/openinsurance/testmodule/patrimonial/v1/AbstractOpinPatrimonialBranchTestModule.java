package net.openid.conformance.openinsurance.testmodule.Patrimonial.v1;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
		callAndStopOnFailure(PolicyIDAllSelector.class);

		fetchUntilBranchIsFound();

		if(!env.getBoolean("branch_found")) {
			env.putString("warning_message", String.format("All policies ID were verified but none matched the branch %", branch.name()));
			callAndContinueOnFailure(ChuckWarning.class, Condition.ConditionResult.WARNING);
		}
	}

	void fetchUntilBranchIsFound() {

		JsonArray policies = JsonParser.parseString(env.getString("all_policies")).getAsJsonArray();

		callAndContinueOnFailure(PrepareUrlForFetchingPatrimonialPolicyInfo.class);
		call(exec().startBlock(String.format("Start looking for a policyID of type %s", branch.name())));

		env.putBoolean("branch_found", false);
		for(int i = 0; i < policies.size(); i++) {
			if (env.getBoolean("branch_found")) {
				break;
			}
			env.putString("policyId", policies.remove(i).toString());
			preCallProtectedResource();
			callAndContinueOnFailure(VerifyBranch.class, Condition.ConditionResult.INFO);

			if (i % 10 == 0) {
				call(exec().startBlock(String.format("[%i] PolicyID of type %s still not found, keep looking,", i, branch.name())));
			}
		}
	}
	void setBranch(PatrimonialBranches branch) {
		this.branch = branch;
		env.putString("branch", branch.getBranchCode());
	}

	PatrimonialBranches getBranch() {
		return this.branch;
	}
}
