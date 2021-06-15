package net.openid.conformance.apis.creditOperations.loans;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.ContractResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/loans/contractResponse.json")
public class ContractResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		ContractResponseValidator condition = new ContractResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractResponseWithError.json")
	public void validateStructureWithMissingField() {
		ContractResponseValidator condition = new ContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("$.data.CET")));
	}
}
