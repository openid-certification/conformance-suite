package net.openid.conformance.apis.creditOperations.loans;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.ContractInstallmentsResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/loans/contractInstallments/contractInstallmentsResponse.json")
public class ContractInstallmentsResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		ContractInstallmentsResponseValidator condition = new ContractInstallmentsResponseValidator();
		run(condition);
	}


	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractInstallments/contractInstallmentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ContractInstallmentsResponseValidator condition = new ContractInstallmentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("pastDueInstalments")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractInstallments/contractInstallmentsResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		ContractInstallmentsResponseValidator condition = new ContractInstallmentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("typeNumberOfInstalments")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/contractInstallments/contractInstallmentsResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		ContractInstallmentsResponseValidator condition = new ContractInstallmentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("dueDate")));
	}
}
