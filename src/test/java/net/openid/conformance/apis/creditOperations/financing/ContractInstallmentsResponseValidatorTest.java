package net.openid.conformance.apis.creditOperations.financing;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.ContractInstallmentsResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class ContractInstallmentsResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/installments/financingInstallmentsResponseOK.json")
	public void validateStructure() {
		ContractInstallmentsResponseValidator condition = new ContractInstallmentsResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/installments/financingInstallmentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ContractInstallmentsResponseValidator condition = new ContractInstallmentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("pastDueInstalments")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/installments/financingInstallmentsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ContractInstallmentsResponseValidator condition = new ContractInstallmentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("typeNumberOfInstalments")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/installments/financingInstallmentsResponse(WrongMaxLength).json")
	public void validateStructureWrongMaxLength() {
		ContractInstallmentsResponseValidator condition = new ContractInstallmentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("totalNumberOfInstalments")));
	}
}
