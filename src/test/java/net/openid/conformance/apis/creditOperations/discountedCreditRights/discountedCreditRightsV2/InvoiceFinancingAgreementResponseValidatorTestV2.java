package net.openid.conformance.apis.creditOperations.discountedCreditRights.discountedCreditRightsV2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.discountedCreditRightsV2.InvoiceFinancingAgreementResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class InvoiceFinancingAgreementResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/agreement/invoiceFinancingAgreementResponse.json")
	public void validateStructure() {
		run(new InvoiceFinancingAgreementResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/agreement/invoiceFinancingAgreementResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new InvoiceFinancingAgreementResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("productType",
			new InvoiceFinancingAgreementResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/agreement/invoiceFinancingAgreementResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new InvoiceFinancingAgreementResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("productType",
			new InvoiceFinancingAgreementResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/agreement/invoiceFinancingAgreementResponse(WrongPattern).json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new InvoiceFinancingAgreementResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("settlementDate",
			new InvoiceFinancingAgreementResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/agreement/invoiceFinancingAgreementResponse(WrongMaxLength).json")
	public void validateStructureWrongMaxLength() {
		ConditionError error = runAndFail(new InvoiceFinancingAgreementResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("instalmentPeriodicityAdditionalInfo",
			new InvoiceFinancingAgreementResponseValidatorV2().getApiName())));
	}
}
