package net.openid.conformance.openinsurance.financialFisk;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openinsurance.validator.financialFisk.v1.OpinInsuranceFinancialRiskClaimValidatorV1;
import net.openid.conformance.openinsurance.validator.financialFisk.v1.OpinInsuranceFinancialRiskListValidatorV1;
import net.openid.conformance.openinsurance.validator.financialFisk.v1.OpinInsuranceFinancialRiskPolicyInfoValidatorV1;
import net.openid.conformance.openinsurance.validator.financialFisk.v1.OpinInsuranceFinancialRiskPremiumValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OpinInsuranceFinancialRiskValidatorsV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/financialRisk/OpinInsuranceFinancialRiskPolicyInfoValidatorV1OK.json")
	public void validateStructurePolicyInfo() {
		run(new OpinInsuranceFinancialRiskPolicyInfoValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/financialRisk/OpinInsuranceFinancialRiskPremiumValidatorV1OK.json")
	public void validateStructurePremium() {
		run(new OpinInsuranceFinancialRiskPremiumValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/financialRisk/OpinInsuranceFinancialRiskClaimValidatorV1OK.json")
	public void validateStructureClaim() {
		run(new OpinInsuranceFinancialRiskClaimValidatorV1());
	}


	@Test
	@UseResurce("openinsuranceResponses/financialRisk/OpinInsuranceFinancialRiskListValidatorV1OK.json")
	public void validateStructureList() {
		run(new OpinInsuranceFinancialRiskListValidatorV1());
	}
}

