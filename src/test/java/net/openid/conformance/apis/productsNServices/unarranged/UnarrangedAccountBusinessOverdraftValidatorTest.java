package net.openid.conformance.apis.productsNServices.unarranged;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.productsNServices.unarrangedAccountOverdraft.UnarrangedAccountBusinessOverdraftValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/businessUnarrangedAccountOverdraftResponseOK.json")
public class UnarrangedAccountBusinessOverdraftValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		UnarrangedAccountBusinessOverdraftValidator condition = new UnarrangedAccountBusinessOverdraftValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/businessUnarrangedAccountOverdraftResponse(MissOptional).json")
	public void validateMissingOptional() {
		UnarrangedAccountBusinessOverdraftValidator condition = new UnarrangedAccountBusinessOverdraftValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/businessUnarrangedAccountOverdraftResponse(WrongPattern).json")
	public void validateStructureWrongPattern() {
		UnarrangedAccountBusinessOverdraftValidator condition = new UnarrangedAccountBusinessOverdraftValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchPatternMessage("rate")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/businessUnarrangedAccountOverdraftResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		UnarrangedAccountBusinessOverdraftValidator condition = new UnarrangedAccountBusinessOverdraftValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createFieldValueNotMatchEnumerationMessage("referentialRateIndexer")));
	}

	@Test
	@UseResurce("jsonResponses/productsNServices/unarrangedAccountOverdraft/businessUnarrangedAccountOverdraftResponse(MoreMaxItems).json")
	public void validateStructureMoreMaxItems() {
		UnarrangedAccountBusinessOverdraftValidator condition = new UnarrangedAccountBusinessOverdraftValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),  containsString(condition
			.createArrayIsMoreThanMaxItemsMessage("applications")));

	}
}
