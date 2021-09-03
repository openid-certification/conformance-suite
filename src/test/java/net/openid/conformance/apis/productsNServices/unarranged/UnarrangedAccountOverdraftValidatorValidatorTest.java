package net.openid.conformance.apis.productsNServices.unarranged;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.productsNServices.unarrangedAccountOverdraft.UnarrangedAccountOverdraftValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/personalUnarrangedAccountOverdraftResponseOK.json")
public class UnarrangedAccountOverdraftValidatorValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		UnarrangedAccountOverdraftValidator condition = new UnarrangedAccountOverdraftValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/personalUnarrangedAccountOverdraftResponse(MissOptional).json")
	public void validateMissingOptional() {
		UnarrangedAccountOverdraftValidator condition = new UnarrangedAccountOverdraftValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/personalUnarrangedAccountOverdraftResponse(Wrongregexp).json")
	public void validateStructureWrongPattern() {
		UnarrangedAccountOverdraftValidator condition = new UnarrangedAccountOverdraftValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchPatternMessage("rate")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/personalUnarrangedAccountOverdraftResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		UnarrangedAccountOverdraftValidator condition = new UnarrangedAccountOverdraftValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchEnumerationMessage("referentialRateIndexer")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/personalUnarrangedAccountOverdraftResponse(MoreMaxItems).json")
	public void validateStructureMoreMaxItems() {
		UnarrangedAccountOverdraftValidator condition = new UnarrangedAccountOverdraftValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createArrayIsMoreThanMaxItemsMessage("applications")));

	}
}
