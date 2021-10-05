package net.openid.conformance.apis;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.registrationData.PersonalRelationsResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/registrationData/naturalPersonalRelationship/naturalPersonRelationshipResponse.json")
public class PersonalRelationsResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		PersonalRelationsResponseValidator condition = new PersonalRelationsResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonalRelationship/naturalPersonRelationshipResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		PersonalRelationsResponseValidator condition = new PersonalRelationsResponseValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createElementNotFoundMessage("branchCode");
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonalRelationship/naturalPersonRelationshipResponseWrongPattern.json")
	public void validateStructureWithWrongPattern() {
		PersonalRelationsResponseValidator condition = new PersonalRelationsResponseValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createFieldValueNotMatchPatternMessage("startDate");
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonalRelationship/naturalPersonRelationshipResponseWrongMaxItems.json")
	public void validateStructureWithWrongMaxItems() {
		PersonalRelationsResponseValidator condition = new PersonalRelationsResponseValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createArrayIsMoreThanMaxItemsMessage("productsServicesType");
		assertThat(error.getMessage(), containsString(expected));
	}
}
