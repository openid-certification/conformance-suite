package net.openid.conformance.apis.creditOperations.loans;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.ContractResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/loans/contract/contractResponseOK.json")
public class ContractResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		ContractResponseValidator condition = new ContractResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contract/contractResponseWithError.json")
	public void validateStructureWithMissingField() {
		ContractResponseValidator condition = new ContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("CET")));
	}
	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contract/contractResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ContractResponseValidator condition = new ContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("productType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contract/contractResponse(WrongRegexp).json")
	public void validateStructureWrongRegexp() {
		ContractResponseValidator condition = new ContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("settlementDate")));
	}
}
