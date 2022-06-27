package net.openid.conformance.apis.creditOperations.discountedCreditRights.v1;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.v1.InvoiceFinancingAgreementResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class InvoiceFinancingAgreementResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/agreement/invoiceFinancingAgreementResponse.json")
	public void validateStructure() {
		InvoiceFinancingAgreementResponseValidator condition = new InvoiceFinancingAgreementResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/agreement/invoiceFinancingAgreementResponseWithError.json")
	public void validateStructureWithMissingField() {
		InvoiceFinancingAgreementResponseValidator condition = new InvoiceFinancingAgreementResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("productType", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/agreement/invoiceFinancingAgreementResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		InvoiceFinancingAgreementResponseValidator condition = new InvoiceFinancingAgreementResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("productType", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/agreement/invoiceFinancingAgreementResponse(WrongPattern).json")
	public void validateStructureWrongRegexp() {
		InvoiceFinancingAgreementResponseValidator condition = new InvoiceFinancingAgreementResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("settlementDate", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/agreement/invoiceFinancingAgreementResponse(WrongMaxLength).json")
	public void validateStructureWrongMaxLength() {
		InvoiceFinancingAgreementResponseValidator condition = new InvoiceFinancingAgreementResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("instalmentPeriodicityAdditionalInfo", condition.getApiName())));
	}
}
