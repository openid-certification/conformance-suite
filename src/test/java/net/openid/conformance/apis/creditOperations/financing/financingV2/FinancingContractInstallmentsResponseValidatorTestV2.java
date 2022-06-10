package net.openid.conformance.apis.creditOperations.financing.financingV2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.financingV2.FinancingContractInstallmentsResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class FinancingContractInstallmentsResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingV2/installments/financingInstallmentsResponseOK.json")
	public void validateStructure() {
		run(new FinancingContractInstallmentsResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingV2/installments/financingInstallmentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new FinancingContractInstallmentsResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createElementNotFoundMessage("pastDueInstalments",
				new FinancingContractInstallmentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingV2/installments/financingInstallmentsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new FinancingContractInstallmentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("typeNumberOfInstalments",
			new FinancingContractInstallmentsResponseValidatorV2().getApiName())));
	}
}
