package net.openid.conformance.apis.creditOperations.loans;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.GetLoansResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/loans/getLoansResponse.json")
public class GetLoansResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		GetLoansResponseValidator condition = new GetLoansResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/getLoansResponseWithError.json")
	public void validateStructureWithMissingField() {
		GetLoansResponseValidator condition = new GetLoansResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("$.data[0].ipocCode")));
	}
}
