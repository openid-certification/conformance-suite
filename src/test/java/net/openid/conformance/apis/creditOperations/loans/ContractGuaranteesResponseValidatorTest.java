package net.openid.conformance.apis.creditOperations.loans;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.ContractGuaranteesResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/loans/contractGuarantees/contractGuaranteesResponse.json")
public class ContractGuaranteesResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		ContractGuaranteesResponseValidator condition = new ContractGuaranteesResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractGuarantees/contractGuaranteesResponseWithError.json")
	public void validateStructureWithMissingField() {
		ContractGuaranteesResponseValidator condition = new ContractGuaranteesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("warrantySubType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractGuarantees/contractGuaranteesResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		ContractGuaranteesResponseValidator condition = new ContractGuaranteesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("warrantyType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractGuarantees/contractGuaranteesResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		ContractGuaranteesResponseValidator condition = new ContractGuaranteesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("currency")));
	}
}
