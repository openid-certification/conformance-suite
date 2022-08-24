package net.openid.conformance.openinsurance.insuranceAviation;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.insuranceAviation.v1.OpinInsuranceAviationClaimValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceAviation.v1.OpinInsuranceAviationListValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceAviation.v1.OpinInsuranceAviationPolicyInfoValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceAviation.v1.OpinInsuranceAviationPremiumValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OpinInsuranceAviationValidatorsV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/insuranceAviation/OpinInsuranceAviationPolicyInfoValidatorV1OK.json")
	public void validateStructurePolicyInfo() {
		run(new OpinInsuranceAviationPolicyInfoValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insuranceAviation/OpinInsuranceAviationPremiumValidatorV1OK.json")
	public void validateStructurePremium() {
		run(new OpinInsuranceAviationPremiumValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/insuranceAviation/OpinInsuranceAviationClaimValidatorV1OK.json")
	public void validateStructureClaim() {
		run(new OpinInsuranceAviationClaimValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insuranceAviation/OpinInsuranceAviationListValidatorV1OK.json")
	public void validateStructureList() {
		run(new OpinInsuranceAviationListValidatorV1());
	}
}

