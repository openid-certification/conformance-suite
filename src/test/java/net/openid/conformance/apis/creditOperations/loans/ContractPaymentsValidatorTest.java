package net.openid.conformance.apis.creditOperations.loans;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.ContractPaymentsValidator;
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
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("currency", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractPayments/contractPaymentsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ContractPaymentsValidator condition = new ContractPaymentsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("chargeType", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractPayments/contractPaymentsResponse(WrongRegexp).json")
	public void validateStructureWrongRegexp() {
		ContractPaymentsValidator condition = new ContractPaymentsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("paidDate", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractPayments/contractPaymentsResponse(WrongMaxLength).json")
	public void validateStructureWrongMaxLength() {
		ContractPaymentsValidator condition = new ContractPaymentsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("paidInstalments", condition.getApiName())));
	}
}
