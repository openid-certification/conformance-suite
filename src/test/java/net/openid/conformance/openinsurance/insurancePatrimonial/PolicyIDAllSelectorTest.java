package net.openid.conformance.openinsurance.insurancePatrimonial;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.testmodules.support.PolicyIDSelector;
import net.openid.conformance.openinsurance.testmodule.patrimonial.v1.PolicyIDAllSelector;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class PolicyIDAllSelectorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/insurancePatrimonial/branches/OpinInsurancePatrimonialPoliciesOK.json")
	public void validateStructurePolicyInfo() {
		run(new PolicyIDAllSelector());
	}
}

