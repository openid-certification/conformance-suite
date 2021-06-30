package net.openid.conformance.apis.creditOperations.discountedCreditRights;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.InvoiceFinancingContractPaymentsResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class InvoiceFinancingContractPaymentsResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contractPayments/invoiceFinancingContractPaymentsResponse.json")
	public void validateStructure() {
		InvoiceFinancingContractPaymentsResponseValidator condition = new InvoiceFinancingContractPaymentsResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contractPayments/invoiceFinancingContractPaymentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		InvoiceFinancingContractPaymentsResponseValidator condition = new InvoiceFinancingContractPaymentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("isOverParcelPayment")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contractPayments/invoiceFinancingContractPaymentsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		InvoiceFinancingContractPaymentsResponseValidator condition = new InvoiceFinancingContractPaymentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("chargeType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contractPayments/invoiceFinancingContractPaymentsResponse(WrongRegexp).json")
	public void validateStructureWrongRegexp() {
		InvoiceFinancingContractPaymentsResponseValidator condition = new InvoiceFinancingContractPaymentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("paidDate")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contractPayments/invoiceFinancingContractPaymentsResponse(WrongMaxLength).json")
	public void validateStructureWrongMaxLength() {
		InvoiceFinancingContractPaymentsResponseValidator condition = new InvoiceFinancingContractPaymentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("paidInstalments")));
	}
}
