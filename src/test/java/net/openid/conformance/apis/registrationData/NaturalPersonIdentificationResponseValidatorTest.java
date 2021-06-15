package net.openid.conformance.apis.registrationData;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.registrationData.NaturalPersonIdentificationResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/registrationData/naturalPersonIdentificationResponse.json")
public class NaturalPersonIdentificationResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		NaturalPersonIdentificationResponseValidator condition = new NaturalPersonIdentificationResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonIdentificationResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		NaturalPersonIdentificationResponseValidator condition = new NaturalPersonIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createElementNotFoundMessage("$.data[0].sex");
		assertThat(error.getMessage(), containsString(expected));
	}
}
