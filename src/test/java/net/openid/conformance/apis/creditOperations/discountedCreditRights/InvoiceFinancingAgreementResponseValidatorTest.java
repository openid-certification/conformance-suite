package net.openid.conformance.apis.creditOperations.discountedCreditRights;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.InvoiceFinancingAgreementResponseValidator;
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
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("productType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/agreement/invoiceFinancingAgreementResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		InvoiceFinancingAgreementResponseValidator condition = new InvoiceFinancingAgreementResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("productType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/agreement/invoiceFinancingAgreementResponse(WrongPattern).json")
	public void validateStructureWrongRegexp() {
		InvoiceFinancingAgreementResponseValidator condition = new InvoiceFinancingAgreementResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("settlementDate")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/agreement/invoiceFinancingAgreementResponse(WrongMaxLength).json")
	public void validateStructureWrongMaxLength() {
		InvoiceFinancingAgreementResponseValidator condition = new InvoiceFinancingAgreementResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("instalmentPeriodicityAdditionalInfo")));
	}
}
