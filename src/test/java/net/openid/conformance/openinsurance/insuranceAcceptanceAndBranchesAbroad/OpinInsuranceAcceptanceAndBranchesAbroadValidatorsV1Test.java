package net.openid.conformance.openinsurance.insuranceAcceptanceAndBranchesAbroad;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.insuranceAcceptanceAndBranchesAbroad.v1.OpinInsuranceAcceptanceAndBranchesAbroadClaimValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceAcceptanceAndBranchesAbroad.v1.OpinInsuranceAcceptanceAndBranchesAbroadListValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceAcceptanceAndBranchesAbroad.v1.OpinInsuranceAcceptanceAndBranchesAbroadPolicyInfoValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceAcceptanceAndBranchesAbroad.v1.OpinInsuranceAcceptanceAndBranchesAbroadPremiumValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OpinInsuranceAcceptanceAndBranchesAbroadValidatorsV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/insuranceAcceptanceAndBranchesAbroad/OpinInsuranceAcceptanceAndBranchesAbroadPolicyInfoValidatorV1OK.json")
	public void validateStructurePolicyInfo() {
		run(new OpinInsuranceAcceptanceAndBranchesAbroadPolicyInfoValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insuranceAcceptanceAndBranchesAbroad/OpinInsuranceAcceptanceAndBranchesAbroadPremiumValidatorV1OK.json")
	public void validateStructurePremium() {
		run(new OpinInsuranceAcceptanceAndBranchesAbroadPremiumValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/insuranceAcceptanceAndBranchesAbroad/OpinInsuranceAcceptanceAndBranchesAbroadClaimValidatorV1OK.json")
	public void validateStructureClaim() {
		run(new OpinInsuranceAcceptanceAndBranchesAbroadClaimValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insuranceAcceptanceAndBranchesAbroad/OpinInsuranceAcceptanceAndBranchesAbroadListValidatorV1OK.json")
	public void validateStructureList() {
		run(new OpinInsuranceAcceptanceAndBranchesAbroadListValidatorV1());
	}
}

