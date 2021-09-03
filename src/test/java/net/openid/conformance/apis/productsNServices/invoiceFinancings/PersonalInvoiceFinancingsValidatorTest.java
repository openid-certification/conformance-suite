package net.openid.conformance.apis.productsNServices.invoiceFinancings;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.productsNServices.invoiceFinancings.PersonalInvoiceFinancingsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/productsNServices/invoiceFinancings/personalInvoiceFinancingsResponseOK.json")
public class PersonalInvoiceFinancingsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		PersonalInvoiceFinancingsValidator condition = new PersonalInvoiceFinancingsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/invoiceFinancings/personalInvoiceFinancingsResponse(MissOptional).json")
	public void validateMissingOptional() {
		PersonalInvoiceFinancingsValidator condition = new PersonalInvoiceFinancingsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/invoiceFinancings/personalInvoiceFinancingsResponse(WrongRegexp).json")
	public void validateStructureWrongPattern() {
		PersonalInvoiceFinancingsValidator condition = new PersonalInvoiceFinancingsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchPatternMessage("rate")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/invoiceFinancings/personalInvoiceFinancingsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		PersonalInvoiceFinancingsValidator condition = new PersonalInvoiceFinancingsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchEnumerationMessage("interval")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/invoiceFinancings/personalInvoiceFinancingsResponse(LessMinItems).json")
	public void validateStructureLessMinItems() {
		PersonalInvoiceFinancingsValidator condition = new PersonalInvoiceFinancingsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createArrayIsLessThanMaxItemsMessage("interestRates")));

	}
}
