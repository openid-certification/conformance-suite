package net.openid.conformance.apis.creditOperations.financing;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.FinancingContractResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/financing/financingContract/financingContractResponse.json")
public class FinancingContractResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		FinancingContractResponseValidator condition = new FinancingContractResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingContract/financingContractResponseWithError.json")
	public void validateStructureWithMissingField() {
		FinancingContractResponseValidator condition = new FinancingContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("feeRate")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingContract/financingContractResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		FinancingContractResponseValidator condition = new FinancingContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("productType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingContract/financingContractResponseWrongRegexp.json")
	public void validateStructureWrongPattern() {
		FinancingContractResponseValidator condition = new FinancingContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("currency")));
	}
}
