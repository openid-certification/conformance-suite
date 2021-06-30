package net.openid.conformance.apis.creditOperations.discountedCreditRights;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.InvoiceFinancingContractInstallmentsResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.ContractInstallmentsResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contractInstallments/invoiceFinancingContractInstallmentsResponse.json")
public class InvoiceFinancingContractInstallmentsResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		InvoiceFinancingContractInstallmentsResponseValidator condition = new InvoiceFinancingContractInstallmentsResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contractInstallments/invoiceFinancingContractInstallmentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		InvoiceFinancingContractInstallmentsResponseValidator condition = new InvoiceFinancingContractInstallmentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("contractRemainingNumber")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contractInstallments/invoiceFinancingContractInstallmentsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		InvoiceFinancingContractInstallmentsResponseValidator condition = new InvoiceFinancingContractInstallmentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("typeNumberOfInstalments")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contractInstallments/invoiceFinancingContractInstallmentsResponse(WrongRegexp).json")
	public void validateStructureWrongRegexp() {
		InvoiceFinancingContractInstallmentsResponseValidator condition = new InvoiceFinancingContractInstallmentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("dueDate")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contractInstallments/invoiceFinancingContractInstallmentsResponse(WrongMaxLength).json")
	public void validateStructureWrongMaxLength() {
		InvoiceFinancingContractInstallmentsResponseValidator condition = new InvoiceFinancingContractInstallmentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("paidInstalments")));
	}
}
