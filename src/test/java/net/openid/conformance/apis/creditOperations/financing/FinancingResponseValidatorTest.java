package net.openid.conformance.apis.creditOperations.financing;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.FinancingResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/financing/financingResponse/financingResponse.json")
public class FinancingResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		FinancingResponseValidator condition = new FinancingResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingResponse/financingResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		FinancingResponseValidator condition = new FinancingResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("productType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingResponse/financingResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		FinancingResponseValidator condition = new FinancingResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("companyCnpj")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingResponse/financingResponseMissField.json")
	public void validateStructureMissField() {
		FinancingResponseValidator condition = new FinancingResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("ipocCode")));
	}
}
