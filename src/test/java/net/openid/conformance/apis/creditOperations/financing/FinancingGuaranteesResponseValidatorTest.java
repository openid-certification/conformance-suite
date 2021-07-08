package net.openid.conformance.apis.creditOperations.financing;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.FinancingGuaranteesResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/financing/guarantees/financingGuaranteesResponse.json")
public class FinancingGuaranteesResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		FinancingGuaranteesResponseValidator condition = new FinancingGuaranteesResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/guarantees/financingGuaranteesResponseWithError.json")
	public void validateStructureWithMissingField() {
		FinancingGuaranteesResponseValidator condition = new FinancingGuaranteesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("warrantySubType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/guarantees/financingGuaranteesResponse(WrongRegexp).json")
	public void validateStructureWithWrongRegexp() {
		FinancingGuaranteesResponseValidator condition = new FinancingGuaranteesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("warrantyAmount")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/guarantees/financingGuaranteesResponse(WrongEnum).json")
	public void validateStructureWithWrongEnum() {
		FinancingGuaranteesResponseValidator condition = new FinancingGuaranteesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("warrantyType")));
	}
}
