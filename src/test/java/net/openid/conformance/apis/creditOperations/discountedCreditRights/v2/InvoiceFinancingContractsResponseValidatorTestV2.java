package net.openid.conformance.apis.creditOperations.discountedCreditRights.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.v2.InvoiceFinancingContractsResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/contracts/invoiceFinancingContractsResponse.json")
public class InvoiceFinancingContractsResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new InvoiceFinancingContractsResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/contracts/invoiceFinancingContractsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new InvoiceFinancingContractsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("ipocCode",
			new InvoiceFinancingContractsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/contracts/invoiceFinancingContractsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new InvoiceFinancingContractsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("productSubType",
			new InvoiceFinancingContractsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/discountedCreditRights/discountedCreditRightsV2/contracts/invoiceFinancingContractsResponse(WrongPattern).json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new InvoiceFinancingContractsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("companyCnpj",
			new InvoiceFinancingContractsResponseValidatorV2().getApiName())));
	}
}
