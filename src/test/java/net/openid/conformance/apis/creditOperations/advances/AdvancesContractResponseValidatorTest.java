package net.openid.conformance.apis.creditOperations.advances;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.AdvancesContractInstallmentsResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.AdvancesContractResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/advances/advancesContractResponse.json")
public class AdvancesContractResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		AdvancesContractResponseValidator condition = new AdvancesContractResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesContractResponseWithError.json")
	public void validateStructureWithMissingField() {
		AdvancesContractResponseValidator condition = new AdvancesContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("$.data.ipocCode")));
	}
}
