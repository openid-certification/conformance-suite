package net.openid.conformance.openinsurance.insuranceNautical;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.insuranceNautical.v1.OpinInsuranceNauticalClaimValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceNautical.v1.OpinInsuranceNauticalListValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceNautical.v1.OpinInsuranceNauticalPolicyInfoValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceNautical.v1.OpinInsuranceNauticalPremiumValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OpinInsuranceNauticalValidatorsV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/insuranceNautical/OpinInsuranceNauticalPolicyInfoValidatorV1OK.json")
	public void validateStructurePolicyInfo() {run(new OpinInsuranceNauticalPolicyInfoValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/insuranceNautical/OpinInsuranceNauticalPremiumValidatorV1OK.json")
	public void validateStructurePremium() {
		run(new OpinInsuranceNauticalPremiumValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/insuranceNautical/OpinInsuranceNauticalClaimValidatorV1OK.json")
	public void validateStructureClaim() {
		run(new OpinInsuranceNauticalClaimValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/insuranceNautical/OpinInsuranceNauticalListValidatorV1OK.json")
	public void validateStructureList() {
		run(new OpinInsuranceNauticalListValidatorV1());
	}
}

