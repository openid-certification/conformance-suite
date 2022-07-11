package net.openid.conformance.apis.registrationData.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.registrationData.v2.PersonalRelationsResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/registrationData/registrationDataV2/naturalPersonalRelationshipV2/naturalPersonRelationshipResponse.json")
public class PersonalRelationsResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new PersonalRelationsResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/naturalPersonalRelationshipV2/naturalPersonRelationshipResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new PersonalRelationsResponseValidatorV2());
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("number", new PersonalRelationsResponseValidatorV2().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/naturalPersonalRelationshipV2/naturalPersonRelationshipResponseWrongPattern.json")
	public void validateStructureWithWrongPattern() {
		ConditionError error = runAndFail(new PersonalRelationsResponseValidatorV2());
		String expected = ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("startDate", new PersonalRelationsResponseValidatorV2().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/naturalPersonalRelationshipV2/naturalPersonRelationshipResponseWrongMaxItems.json")
	public void validateStructureWithWrongMaxItems() {
		ConditionError error = runAndFail(new PersonalRelationsResponseValidatorV2());
		String expected = ErrorMessagesUtils.createArrayIsMoreThanMaxItemsMessage("productsServicesType", new PersonalRelationsResponseValidatorV2().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}
}
