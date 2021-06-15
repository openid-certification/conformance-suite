package net.openid.conformance.apis.creditOperations.discountedCreditRights;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.InvoiceFinancingContractPaymentsResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/discountedCreditRights/invoiceFinancingContractPaymentsResponse.json")
public class InvoiceFinancingContractPaymentsResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		InvoiceFinancingContractPaymentsResponseValidator condition = new InvoiceFinancingContractPaymentsResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/invoiceFinancingContractPaymentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		InvoiceFinancingContractPaymentsResponseValidator condition = new InvoiceFinancingContractPaymentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("$.data.releases[0].isOverParcelPayment")));
	}
}


