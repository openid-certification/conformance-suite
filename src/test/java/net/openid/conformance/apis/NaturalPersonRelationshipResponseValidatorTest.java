package net.openid.conformance.apis;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.registrationData.NaturalPersonRelationshipResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/registrationData/naturalPersonalRelationship/naturalPersonRelationshipResponse.json")
public class NaturalPersonRelationshipResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		NaturalPersonRelationshipResponseValidator condition = new NaturalPersonRelationshipResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonalRelationship/naturalPersonRelationshipResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		NaturalPersonRelationshipResponseValidator condition = new NaturalPersonRelationshipResponseValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createElementNotFoundMessage("branchCode");
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonalRelationship/naturalPersonRelationshipResponseWrongPattern.json")
	public void validateStructureWithWrongPattern() {
		NaturalPersonRelationshipResponseValidator condition = new NaturalPersonRelationshipResponseValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createFieldValueNotMatchPatternMessage("startDate");
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonalRelationship/naturalPersonRelationshipResponseWrongMaxItems.json")
	public void validateStructureWithWrongMaxItems() {
		NaturalPersonRelationshipResponseValidator condition = new NaturalPersonRelationshipResponseValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createArrayIsMoreThanMaxItemsMessage("productsServicesType");
		assertThat(error.getMessage(), containsString(expected));
	}
}
