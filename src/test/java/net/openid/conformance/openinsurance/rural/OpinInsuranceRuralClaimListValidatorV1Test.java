package net.openid.conformance.openinsurance.rural;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.rural.v1.OpinInsuranceRuralClaimListValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class OpinInsuranceRuralClaimListValidatorV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/rural/claim/claimListStructure.json")
	public void validateStructure() {
		run(new OpinInsuranceRuralClaimListValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/rural/claim/claimListStructure(FieldNotFound).json")
	public void validateFieldNotFound() {
		OpinInsuranceRuralClaimListValidatorV1 condition = new OpinInsuranceRuralClaimListValidatorV1();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createElementNotFoundMessage("identification", condition.getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/rural/claim/claimListStructure(WrongRegexp).json")
	public void validateWrongRegexp() {
		OpinInsuranceRuralClaimListValidatorV1 condition = new OpinInsuranceRuralClaimListValidatorV1();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createFieldValueNotMatchPatternMessage("currency", condition.getApiName())));
	}
}
