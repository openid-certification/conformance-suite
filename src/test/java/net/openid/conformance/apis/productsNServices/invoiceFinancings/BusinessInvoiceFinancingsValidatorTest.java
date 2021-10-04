package net.openid.conformance.apis.productsNServices.invoiceFinancings;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.productsNServices.invoiceFinancings.BusinessInvoiceFinancingsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/productsNServices/invoiceFinancings/businessInvoiceFinancingsResponseOK.json")
public class BusinessInvoiceFinancingsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		BusinessInvoiceFinancingsValidator condition = new BusinessInvoiceFinancingsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/invoiceFinancings/businessInvoiceFinancingsResponse(MissOptional).json")
	public void validateMissingOptional() {
		BusinessInvoiceFinancingsValidator condition = new BusinessInvoiceFinancingsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/invoiceFinancings/businessInvoiceFinancingsResponse(WrongRegexp).json")
	public void validateStructureWrongPattern() {
		BusinessInvoiceFinancingsValidator condition = new BusinessInvoiceFinancingsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchPatternMessage("rate")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/invoiceFinancings/businessInvoiceFinancingsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		BusinessInvoiceFinancingsValidator condition = new BusinessInvoiceFinancingsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchEnumerationMessage("interval")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/invoiceFinancings/businessInvoiceFinancingsResponse(LessMinItems).json")
	public void validateStructureLessMinItems() {
		BusinessInvoiceFinancingsValidator condition = new BusinessInvoiceFinancingsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createArrayIsLessThanMaxItemsMessage("interestRates")));

	}
}
