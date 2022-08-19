package net.openid.conformance.openinsurance.rural;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.rural.v1.OpinInsuranceRuralPolicyInfoValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class OpinInsuranceRuralPolicyInfoValidatorV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/rural/policyInfo/policyInfoResponse.json")
	public void validateStructure() {
		run(new OpinInsuranceRuralPolicyInfoValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/rural/policyInfo/policyInfoResponseWrongMinProp.json")
	public void validateStructureWrongMinProp() {
		OpinInsuranceRuralPolicyInfoValidatorV1  condition = new OpinInsuranceRuralPolicyInfoValidatorV1();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createObjectLessRequiredMinProperties("coverages", condition.getApiName());
		MatcherAssert.assertThat(error.getMessage(), Matchers.containsString(expected));

	}

	@Test
	@UseResurce("openinsuranceResponses/rural/policyInfo/policyInfoResponseWrongMinProp2.json")
	public void validateStructureWrongMinProp2() {
		OpinInsuranceRuralPolicyInfoValidatorV1  condition = new OpinInsuranceRuralPolicyInfoValidatorV1();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createObjectLessRequiredMinProperties("coverages", condition.getApiName());
		MatcherAssert.assertThat(error.getMessage(), Matchers.containsString(expected));

	}
}
