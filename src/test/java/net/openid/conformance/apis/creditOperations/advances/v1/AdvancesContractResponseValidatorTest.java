package net.openid.conformance.apis.creditOperations.advances.v1;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.v1.AdvancesContractResponseValidator;
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
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("ipocCode", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/contract/advancesContractResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		AdvancesContractResponseValidator condition = new AdvancesContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("productType", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/contract/advancesContractResponse(WrongPattern).json")
	public void validateStructureWrongRegexp() {
		AdvancesContractResponseValidator condition = new AdvancesContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("settlementDate", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/contract/advancesContractResponse(WrongMaxLength).json")
	public void validateStructureWrongMaxLength() {
		AdvancesContractResponseValidator condition = new AdvancesContractResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("instalmentPeriodicityAdditionalInfo", condition.getApiName())));
	}
}
