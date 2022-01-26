package net.openid.conformance.apis.productsNServices.loans;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.productsNServices.loans.PersonalLoansValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/productsNServices/loans/personalLoansResponseOK.json")
public class PersonalLoansValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		PersonalLoansValidator condition = new PersonalLoansValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/loans/personalLoansResponseLessMinItems.json")
	public void validateStructureLessMinItems() {
		PersonalLoansValidator condition = new PersonalLoansValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(ErrorMessagesUtils
			.createArrayIsLessThanMaxItemsMessage("applications", condition.getApiName())));

	}

	@Test
	@UseResurce("jsonResponses/productsNServices/loans/personalLoansResponseMoreMaxItems.json")
	public void validateStructureMoreMaxItems() {
		PersonalLoansValidator condition = new PersonalLoansValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(ErrorMessagesUtils
			.createArrayIsMoreThanMaxItemsMessage("applications", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/loans/personalLoansResponse(WrongPattern).json")
	public void validateStructureWrongPattern() {
		PersonalLoansValidator condition = new PersonalLoansValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(ErrorMessagesUtils
			.createFieldValueNotMatchPatternMessage("rate", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/loans/personalLoansResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		PersonalLoansValidator condition = new PersonalLoansValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(ErrorMessagesUtils
			.createFieldValueNotMatchEnumerationMessage("interval", condition.getApiName())));
	}
}
