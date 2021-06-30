package net.openid.conformance.apis.creditOperations.discountedCreditRights;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.InvoiceFinancingContractsResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contracts/invoiceFinancingContractsResponse.json")
public class InvoiceFinancingContractsResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		InvoiceFinancingContractsResponseValidator condition = new InvoiceFinancingContractsResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contracts/invoiceFinancingContractsResponseWithError.json")
	public void validateStructureWithMissingField() {
		InvoiceFinancingContractsResponseValidator condition = new InvoiceFinancingContractsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("ipocCode")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contracts/invoiceFinancingContractsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		InvoiceFinancingContractsResponseValidator condition = new InvoiceFinancingContractsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("productSubType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/contracts/invoiceFinancingContractsResponse(WrongPattern).json")
	public void validateStructureWrongRegexp() {
		InvoiceFinancingContractsResponseValidator condition = new InvoiceFinancingContractsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("companyCnpj")));
	}
}
