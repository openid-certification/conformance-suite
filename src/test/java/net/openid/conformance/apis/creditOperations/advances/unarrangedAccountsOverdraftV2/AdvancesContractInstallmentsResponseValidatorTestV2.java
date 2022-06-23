package net.openid.conformance.apis.creditOperations.advances.unarrangedAccountsOverdraftV2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.advancesV2.AdvancesContractInstallmentsResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/advances/advancesV2/contractInstallments/advanceContractInstallmentsResponse.json")
public class AdvancesContractInstallmentsResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new AdvancesContractInstallmentsResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/contractInstallments/advanceContractInstallmentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new AdvancesContractInstallmentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("typeNumberOfInstalments",
			new AdvancesContractInstallmentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/contractInstallments/advanceContractInstallmentsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new AdvancesContractInstallmentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("typeNumberOfInstalments",
			new AdvancesContractInstallmentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/contractInstallments/advanceContractInstallmentsResponse(WrongRegexp).json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new AdvancesContractInstallmentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("dueDate",
			new AdvancesContractInstallmentsResponseValidatorV2().getApiName())));
	}
}
