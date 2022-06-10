package net.openid.conformance.apis.creditOperations.loans.loansV2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.loansV2.ContractPaymentsValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/loans/loansV2/contractPayments/contractPaymentsResponse.json")
public class ContractPaymentsValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {
	@Test
	public void validateStructure() {
		run(new ContractPaymentsValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contractPayments/contractPaymentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new ContractPaymentsValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("currency",
			new ContractPaymentsValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contractPayments/contractPaymentsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new ContractPaymentsValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("chargeType",
			new ContractPaymentsValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contractPayments/contractPaymentsResponse(WrongRegexp).json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new ContractPaymentsValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("paidDate",
			new ContractPaymentsValidatorV2().getApiName())));
	}

}
