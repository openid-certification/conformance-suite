package net.openid.conformance.apis.creditOperations.discountedCreditRights.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.v2.InvoiceFinancingContractGuaranteesResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/guarantees/invoiceFinancingContractGuaranteesResponse.json")
public class InvoiceFinancingContractGuaranteesResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new InvoiceFinancingContractGuaranteesResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/guarantees/invoiceFinancingContractGuaranteesResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new InvoiceFinancingContractGuaranteesResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("warrantyType",
			new InvoiceFinancingContractGuaranteesResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/guarantees/invoiceFinancingContractGuaranteesResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new InvoiceFinancingContractGuaranteesResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("warrantySubType",
			new InvoiceFinancingContractGuaranteesResponseValidatorV2().getApiName())));
	}
}
