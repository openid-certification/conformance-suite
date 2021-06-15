package net.openid.conformance.apis.creditOperations.financing;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.FinancingContractResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/financing/financingContractResponse.json")
public class FinancingContractResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {
	@Test
	public void validateStructure() {
		FinancingContractResponseValidator condition = new FinancingContractResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingContractResponseWithError.json")
	public void validateStructureWithMissingField() {
		FinancingContractResponseValidator condition = new FinancingContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("$.data.contractedFees[0].feeRate")));
	}
}
