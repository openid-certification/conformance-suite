package net.openid.conformance.apis.creditOperations.discountedCreditRights;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.InvoiceFinancingContractGuaranteesResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/discountedCreditRights/guarantees/invoiceFinancingContractGuaranteesResponse.json")
public class InvoiceFinancingContractGuaranteesResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		InvoiceFinancingContractGuaranteesResponseValidator condition = new InvoiceFinancingContractGuaranteesResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/guarantees/invoiceFinancingContractGuaranteesResponseWithError.json")
	public void validateStructureWithMissingField() {
		InvoiceFinancingContractGuaranteesResponseValidator condition = new InvoiceFinancingContractGuaranteesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("warrantyType", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/guarantees/invoiceFinancingContractGuaranteesResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		InvoiceFinancingContractGuaranteesResponseValidator condition = new InvoiceFinancingContractGuaranteesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("warrantySubType", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/guarantees/invoiceFinancingContractGuaranteesResponse(NoTrailingZero).json")
	public void validateStructureWrongRegexp() {
		InvoiceFinancingContractGuaranteesResponseValidator condition = new InvoiceFinancingContractGuaranteesResponseValidator();
		run(condition);
	}
}

