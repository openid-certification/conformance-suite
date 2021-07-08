package net.openid.conformance.apis.creditOperations.advances;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.AdvancesContractResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class AdvancesContractResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/contract/advancesContractResponse.json")
	public void validateStructure() {
		AdvancesContractResponseValidator condition = new AdvancesContractResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/contract/advancesContractResponseWithError.json")
	public void validateStructureWithMissingField() {
		AdvancesContractResponseValidator condition = new AdvancesContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("ipocCode")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/contract/advancesContractResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		AdvancesContractResponseValidator condition = new AdvancesContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("productType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/contract/advancesContractResponse(WrongPattern).json")
	public void validateStructureWrongRegexp() {
		AdvancesContractResponseValidator condition = new AdvancesContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("settlementDate")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/contract/advancesContractResponse(WrongMaxLength).json")
	public void validateStructureWrongMaxLength() {
		AdvancesContractResponseValidator condition = new AdvancesContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("instalmentPeriodicityAdditionalInfo")));
	}
}
