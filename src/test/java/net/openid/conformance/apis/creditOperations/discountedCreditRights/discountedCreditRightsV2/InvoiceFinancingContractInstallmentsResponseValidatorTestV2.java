package net.openid.conformance.apis.creditOperations.discountedCreditRights.discountedCreditRightsV2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.discountedCreditRightsV2.InvoiceFinancingContractInstallmentsResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/contractInstallments/invoiceFinancingContractInstallmentsResponse.json")
public class InvoiceFinancingContractInstallmentsResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new InvoiceFinancingContractInstallmentsResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/contractInstallments/invoiceFinancingContractInstallmentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new InvoiceFinancingContractInstallmentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("typeContractRemaining",
			new InvoiceFinancingContractInstallmentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/contractInstallments/invoiceFinancingContractInstallmentsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new InvoiceFinancingContractInstallmentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("typeNumberOfInstalments",
			new InvoiceFinancingContractInstallmentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/contractInstallments/invoiceFinancingContractInstallmentsResponse(WrongRegexp).json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new InvoiceFinancingContractInstallmentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("dueDate",
			new InvoiceFinancingContractInstallmentsResponseValidatorV2().getApiName())));
	}
}
