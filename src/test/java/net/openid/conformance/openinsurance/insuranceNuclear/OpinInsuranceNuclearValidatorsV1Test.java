package net.openid.conformance.openinsurance.insuranceNuclear;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.insuranceNuclear.v1.OpinInsuranceNuclearClaimValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceNuclear.v1.OpinInsuranceNuclearListValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceNuclear.v1.OpinInsuranceNuclearPolicyInfoValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceNuclear.v1.OpinInsuranceNuclearPremiumValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OpinInsuranceNuclearValidatorsV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/insuranceNuclear/OpinInsuranceNuclearPolicyInfoValidatorV1OK.json")
	public void validateStructurePolicyInfo() {run(new OpinInsuranceNuclearPolicyInfoValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insuranceNuclear/OpinInsuranceNuclearPremiumValidatorV1OK.json")
	public void validateStructurePremium() {
		run(new OpinInsuranceNuclearPremiumValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/insuranceNuclear/OpinInsuranceNuclearClaimValidatorV1OK.json")
	public void validateStructureClaim() {
		run(new OpinInsuranceNuclearClaimValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insuranceNuclear/OpinInsuranceNuclearListValidatorV1OK.json")
	public void validateStructureList() {
		run(new OpinInsuranceNuclearListValidatorV1());
	}
}

