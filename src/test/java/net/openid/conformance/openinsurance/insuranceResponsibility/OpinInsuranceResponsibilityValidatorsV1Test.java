package net.openid.conformance.openinsurance.insuranceResponsibility;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.insuranceResponsibility.v1.OpinInsuranceResponsibilityClaimValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceResponsibility.v1.OpinInsuranceResponsibilityListValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceResponsibility.v1.OpinInsuranceResponsibilityPolicyInfoValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceResponsibility.v1.OpinInsuranceResponsibilityPremiumValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OpinInsuranceResponsibilityValidatorsV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/insuranceResponsibility/OpinInsuranceResponsibilityPolicyInfoValidatorV1OK.json")
	public void validateStructurePolicyInfo() {
		run(new OpinInsuranceResponsibilityPolicyInfoValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insuranceResponsibility/OpinInsuranceResponsibilityPremiumValidatorV1OK.json")
	public void validateStructurePremium() {
		run(new OpinInsuranceResponsibilityPremiumValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/insuranceResponsibility/OpinInsuranceResponsibilityClaimValidatorV1OK.json")
	public void validateStructureClaim() {
		run(new OpinInsuranceResponsibilityClaimValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insuranceResponsibility/OpinInsuranceResponsibilityListValidatorV1OK.json")
	public void validateStructureList() {
		run(new OpinInsuranceResponsibilityListValidatorV1());
	}
}

