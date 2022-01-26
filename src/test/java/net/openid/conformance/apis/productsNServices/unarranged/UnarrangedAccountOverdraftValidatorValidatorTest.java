package net.openid.conformance.apis.productsNServices.unarranged;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.productsNServices.unarrangedAccountOverdraft.UnarrangedAccountPersonalOverdraftValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/personalUnarrangedAccountOverdraftResponseOK.json")
public class UnarrangedAccountOverdraftValidatorValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		UnarrangedAccountPersonalOverdraftValidator condition = new UnarrangedAccountPersonalOverdraftValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/personalUnarrangedAccountOverdraftResponse(MissOptional).json")
	public void validateMissingOptional() {
		UnarrangedAccountPersonalOverdraftValidator condition = new UnarrangedAccountPersonalOverdraftValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/personalUnarrangedAccountOverdraftResponse(Wrongregexp).json")
	public void validateStructureWrongPattern() {
		UnarrangedAccountPersonalOverdraftValidator condition = new UnarrangedAccountPersonalOverdraftValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(ErrorMessagesUtils
			.createFieldValueNotMatchPatternMessage("rate", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/personalUnarrangedAccountOverdraftResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		UnarrangedAccountPersonalOverdraftValidator condition = new UnarrangedAccountPersonalOverdraftValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(ErrorMessagesUtils
			.createFieldValueNotMatchEnumerationMessage("referentialRateIndexer", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/personalUnarrangedAccountOverdraftResponse(MoreMaxItems).json")
	public void validateStructureMoreMaxItems() {
		UnarrangedAccountPersonalOverdraftValidator condition = new UnarrangedAccountPersonalOverdraftValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(ErrorMessagesUtils
			.createArrayIsMoreThanMaxItemsMessage("applications", condition.getApiName())));

	}
}
