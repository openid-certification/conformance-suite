package net.openid.conformance.apis.creditOperations.financing;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.FinancingResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/financing/financingResponse.json")
public class FinancingResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		FinancingResponseValidator condition = new FinancingResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingResponseWithError.json")
	public void validateStructureWithMissingField() {
		FinancingResponseValidator condition = new FinancingResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("$.data[0].ipocCode")));
	}
}
