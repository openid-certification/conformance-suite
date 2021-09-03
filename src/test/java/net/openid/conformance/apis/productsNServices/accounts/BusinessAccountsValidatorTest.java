package net.openid.conformance.apis.productsNServices.accounts;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.productsNServices.accounts.BusinessAccountsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/productsNServices/accounts/businessAccountsResponseOK.json")
public class BusinessAccountsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		BusinessAccountsValidator condition = new BusinessAccountsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/accounts/businessAccountsResponse(MissOptional).json")
	public void validateMissingOptional() {
		BusinessAccountsValidator condition = new BusinessAccountsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/accounts/businessAccountsResponse(WrongRegexp).json")
	public void validateStructureWrongPattern() {
		BusinessAccountsValidator condition = new BusinessAccountsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchPatternMessage("rate")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/accounts/businessAccountsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		BusinessAccountsValidator condition = new BusinessAccountsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchEnumerationMessage("type")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/accounts/businessAccountsResponse(MoreMaxItems).json")
	public void validateStructureMoreMaxItems() {
		BusinessAccountsValidator condition = new BusinessAccountsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createArrayIsMoreThanMaxItemsMessage("openingClosingChannels")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/accounts/businessAccountsResponse(LessMinItems).json")
	public void validateStructureLessMinItems() {
		BusinessAccountsValidator condition = new BusinessAccountsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createArrayIsLessThanMaxItemsMessage("openingClosingChannels")));
	}
}
