package net.openid.conformance.openinsurance.rural;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.rural.v1.OpinInsuranceRuralPremiumValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class OpinInsuranceRuralPremiumValidatorV1Test extends AbstractJsonResponseConditionUnitTest {
	@Test
	@UseResurce("openinsuranceResponses/rural/premium/premiumResponseStructure.json")
	public void validateStructure() {
		run(new OpinInsuranceRuralPremiumValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/rural/premium/premiumResponseStructure(FieldNotFound).json")
	public void validateFieldNotFound() {
		OpinInsuranceRuralPremiumValidatorV1 condition = new OpinInsuranceRuralPremiumValidatorV1();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createElementNotFoundMessage("paymentsQuantity", condition.getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/rural/premium/premiumResponseStructure(IncorrectRegexp).json")
	public void validateWrongRegexp() {
		OpinInsuranceRuralPremiumValidatorV1 condition = new OpinInsuranceRuralPremiumValidatorV1();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createFieldValueNotMatchPatternMessage("currency", condition.getApiName())));
	}
}
