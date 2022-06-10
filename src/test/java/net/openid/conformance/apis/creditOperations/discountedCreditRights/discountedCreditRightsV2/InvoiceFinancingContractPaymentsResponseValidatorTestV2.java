package net.openid.conformance.apis.creditOperations.discountedCreditRights.discountedCreditRightsV2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.discountedCreditRightsV2.InvoiceFinancingContractPaymentsResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class InvoiceFinancingContractPaymentsResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/contractPayments/invoiceFinancingContractPaymentsResponse.json")
	public void validateStructure() {
		run(new InvoiceFinancingContractPaymentsResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/contractPayments/invoiceFinancingContractPaymentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new InvoiceFinancingContractPaymentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("isOverParcelPayment",
			new InvoiceFinancingContractPaymentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/contractPayments/invoiceFinancingContractPaymentsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new InvoiceFinancingContractPaymentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("chargeType",
			new InvoiceFinancingContractPaymentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/contractPayments/invoiceFinancingContractPaymentsResponse(WrongRegexp).json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new InvoiceFinancingContractPaymentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("paidDate",
			new InvoiceFinancingContractPaymentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/contractPayments/invoiceFinancingContractPaymentsResponse(WrongMaxLength).json")
	public void validateStructureWrongMaxLength() {
		ConditionError error = runAndFail(new InvoiceFinancingContractPaymentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("instalmentId",
			new InvoiceFinancingContractPaymentsResponseValidatorV2().getApiName())));
	}
}
