package net.openid.conformance.apis.creditOperations.loans.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v2.ContractInstallmentsResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/loans/loansV2/contractInstallments/contractInstallmentsResponse.json")
public class ContractInstallmentsResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new ContractInstallmentsResponseValidatorV2());
	}


	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contractInstallments/contractInstallmentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new ContractInstallmentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("pastDueInstalments", new ContractInstallmentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contractInstallments/contractInstallmentsResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		ConditionError error = runAndFail(new ContractInstallmentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("typeNumberOfInstalments", new ContractInstallmentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contractInstallments/contractInstallmentsResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		ConditionError error = runAndFail(new ContractInstallmentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("dueDate", new ContractInstallmentsResponseValidatorV2().getApiName())));
	}
}
