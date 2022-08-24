package net.openid.conformance.openinsurance.insuranceAuto;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.insuranceAuto.v1.OpinInsuranceAutoClaimValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceAuto.v1.OpinInsuranceAutoListValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceAuto.v1.OpinInsuranceAutoPolicyInfoValidatorV1;
import net.openid.conformance.openinsurance.validator.insuranceAuto.v1.OpinInsuranceAutoPremiumValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OpinInsuranceAutoValidatorsV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/insuranceAuto/OpinInsuranceAutoPolicyInfoValidatorV1OK.json")
	public void validateStructurePolicyInfo() {
		run(new OpinInsuranceAutoPolicyInfoValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insuranceAuto/OpinInsuranceAutoPremiumValidatorV1OK.json")
	public void validateStructurePremium() {
		run(new OpinInsuranceAutoPremiumValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/insuranceAuto/OpinInsuranceAutoClaimValidatorV1OK.json")
	public void validateStructureClaim() {
		run(new OpinInsuranceAutoClaimValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/insuranceAuto/OpinInsuranceAutoListValidatorV1OK.json")
	public void validateStructureList() {
		run(new OpinInsuranceAutoListValidatorV1());
	}
}

