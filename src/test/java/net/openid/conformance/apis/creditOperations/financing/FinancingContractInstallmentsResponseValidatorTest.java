package net.openid.conformance.apis.creditOperations.financing;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.FinancingContractInstallmentsResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class FinancingContractInstallmentsResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/installments/financingInstallmentsResponseOK.json")
	public void validateStructure() {
		FinancingContractInstallmentsResponseValidator condition = new FinancingContractInstallmentsResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/installments/financingInstallmentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		FinancingContractInstallmentsResponseValidator condition = new FinancingContractInstallmentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createElementNotFoundMessage("pastDueInstalments",
				condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/installments/financingInstallmentsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		FinancingContractInstallmentsResponseValidator condition = new FinancingContractInstallmentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("typeNumberOfInstalments",
			condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/installments/financingInstallmentsResponse(WrongMaxLength).json")
	public void validateStructureWrongMaxLength() {
		FinancingContractInstallmentsResponseValidator condition = new FinancingContractInstallmentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("totalNumberOfInstalments",
			condition.getApiName())));
	}
}
