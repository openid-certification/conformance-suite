package net.openid.conformance.apis.creditOperations.loans;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.ContractPaymentsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/loans/contractPaymentsResponse.json")
public class ContractPaymentsValidatorTest extends AbstractJsonResponseConditionUnitTest {
	@Test
	public void validateStructure() {
		ContractPaymentsValidator condition = new ContractPaymentsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractPaymentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ContractPaymentsValidator condition = new ContractPaymentsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("$.data.releases[0].overParcel.charges[0].chargeAmount")));
	}
}
