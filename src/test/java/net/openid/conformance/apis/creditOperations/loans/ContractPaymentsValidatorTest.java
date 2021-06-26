package net.openid.conformance.apis.creditOperations.loans;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.ContractPaymentsValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.ContractResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/loans/contractPayments/contractPaymentsResponse.json")
public class ContractPaymentsValidatorTest extends AbstractJsonResponseConditionUnitTest {
	@Test
	public void validateStructure() {
		ContractPaymentsValidator condition = new ContractPaymentsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractPayments/contractPaymentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ContractPaymentsValidator condition = new ContractPaymentsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("currency")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractPayments/contractPaymentsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ContractPaymentsValidator condition = new ContractPaymentsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("chargeType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractPayments/contractPaymentsResponse(WrongRegexp).json")
	public void validateStructureWrongRegexp() {
		ContractPaymentsValidator condition = new ContractPaymentsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("paidDate")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractPayments/contractPaymentsResponse(WrongMaxLength).json")
	public void validateStructureWrongMaxLength() {
		ContractPaymentsValidator condition = new ContractPaymentsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("paidInstalments")));
	}
}
