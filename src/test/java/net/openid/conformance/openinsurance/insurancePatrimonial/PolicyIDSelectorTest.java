package net.openid.conformance.openinsurance.insurancePatrimonial;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.testmodules.support.PolicyIDSelector;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialClaimValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialListValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialPolicyInfoValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialPremiumValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class PolicyIDSelectorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/insurancePatrimonial/OpinInsurancePatrimonialListValidatorV1OK.json")
	public void validateStructurePolicyInfo() {
		run(new PolicyIDSelector());
	}
}

