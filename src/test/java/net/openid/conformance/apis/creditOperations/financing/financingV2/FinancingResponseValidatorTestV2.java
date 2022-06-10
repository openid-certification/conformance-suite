package net.openid.conformance.apis.creditOperations.financing.financingV2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.financingV2.FinancingResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/financing/financingV2/financingResponse/financingResponse.json")
public class FinancingResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new FinancingResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingV2/financingResponse/financingResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		ConditionError error = runAndFail(new FinancingResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("productType",
				new FinancingResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingV2/financingResponse/financingResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		ConditionError error = runAndFail(new FinancingResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("companyCnpj",
				new FinancingResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingV2/financingResponse/financingResponseMissField.json")
	public void validateStructureMissField() {
		ConditionError error = runAndFail(new FinancingResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createElementNotFoundMessage("ipocCode", new FinancingResponseValidatorV2().getApiName())));
	}
}
