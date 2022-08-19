package net.openid.conformance.openinsurance.insurancePetroleum;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.insurancePetroleum.v1.OpinInsurancePetroleumClaimValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePetroleum.v1.OpinInsurancePetroleumListValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePetroleum.v1.OpinInsurancePetroleumPolicyInfoValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePetroleum.v1.OpinInsurancePetroleumPremiumValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OpinInsurancePetroleumValidatorsV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/insurancePetroleum/OpinInsurancePetroleumPolicyInfoValidatorV1OK.json")
	public void validateStructurePolicyInfo() {
		run(new OpinInsurancePetroleumPolicyInfoValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insurancePetroleum/OpinInsurancePetroleumPremiumValidatorV1OK.json")
	public void validateStructurePremium() {
		run(new OpinInsurancePetroleumPremiumValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/insurancePetroleum/OpinInsurancePetroleumClaimValidatorV1OK.json")
	public void validateStructureClaim() {
		run(new OpinInsurancePetroleumClaimValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insurancePetroleum/OpinInsurancePetroleumListValidatorV1OK.json")
	public void validateStructureList() {
		run(new OpinInsurancePetroleumListValidatorV1());
	}
}

