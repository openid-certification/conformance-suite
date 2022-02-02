package net.openid.conformance.apis.productsNServices.loans;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
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
		assertThat(error.getMessage(),  containsString(ErrorMessagesUtils
			.createArrayIsLessThanMaxItemsMessage("applications", condition.getApiName())));

	}

	@Test
	@UseResurce("jsonResponses/productsNServices/loans/businessLoansResponse(MoreMaxitems).json")
	public void validateStructureMoreMaxItems() {
		BusinessLoansValidator condition = new BusinessLoansValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(ErrorMessagesUtils
			.createArrayIsMoreThanMaxItemsMessage("applications", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/loans/businessLoansResponse(WrongPattern).json")
	public void validateStructureWrongPattern() {
		BusinessLoansValidator condition = new BusinessLoansValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(ErrorMessagesUtils
			.createFieldValueNotMatchPatternMessage("rate", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/loans/businessLoansResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		BusinessLoansValidator condition = new BusinessLoansValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(ErrorMessagesUtils
			.createFieldValueNotMatchEnumerationMessage("interval", condition.getApiName())));
	}
}
