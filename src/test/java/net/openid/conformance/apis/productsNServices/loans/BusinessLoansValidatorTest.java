package net.openid.conformance.apis.productsNServices.loans;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.productsNServices.loans.BusinessLoansValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/productsNServices/loans/businessLoansResponseOK.json")
public class BusinessLoansValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		BusinessLoansValidator condition = new BusinessLoansValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/loans/businessLoansResponse(LessMinItems).json")
	public void validateStructureLessMinItems() {
		BusinessLoansValidator condition = new BusinessLoansValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createArrayIsLessThanMaxItemsMessage("applications")));

	}

	@Test
	@UseResurce("jsonResponses/productsNServices/loans/businessLoansResponse(MoreMaxitems).json")
	public void validateStructureMoreMaxItems() {
		BusinessLoansValidator condition = new BusinessLoansValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createArrayIsMoreThanMaxItemsMessage("applications")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/loans/businessLoansResponse(WrongPattern).json")
	public void validateStructureWrongPattern() {
		BusinessLoansValidator condition = new BusinessLoansValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchPatternMessage("rate")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/loans/businessLoansResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		BusinessLoansValidator condition = new BusinessLoansValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchEnumerationMessage("interval")));
	}
}
