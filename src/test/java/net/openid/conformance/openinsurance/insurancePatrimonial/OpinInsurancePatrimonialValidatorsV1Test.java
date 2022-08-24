package net.openid.conformance.openinsurance.insurancePatrimonial;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialClaimValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialListValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialPolicyInfoValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialPremiumValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OpinInsurancePatrimonialValidatorsV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/insurancePatrimonial/OpinInsurancePatrimonialPolicyInfoValidatorV1OK.json")
	public void validateStructurePolicyInfo() {run(new OpinInsurancePatrimonialPolicyInfoValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insurancePatrimonial/OpinInsurancePatrimonialPremiumValidatorV1OK.json")
	public void validateStructurePremium() {
		run(new OpinInsurancePatrimonialPremiumValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/insurancePatrimonial/OpinInsurancePatrimonialClaimValidatorV1OK.json")
	public void validateStructureClaim() {
		run(new OpinInsurancePatrimonialClaimValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insurancePatrimonial/OpinInsurancePatrimonialListValidatorV1OK.json")
	public void validateStructureList() {
		run(new OpinInsurancePatrimonialListValidatorV1());
	}
}

