package net.openid.conformance.apis.registrationData;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.registrationData.LegalEntityIdentificationValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponse.json")
public class LegalEntityIdentificationValidatorTest extends AbstractJsonResponseConditionUnitTest {
	@Test
	public void validateStructure() {
		LegalEntityIdentificationValidator condition = new LegalEntityIdentificationValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponseWithError.json")
	public void validateStructureWithMissingField() {
		LegalEntityIdentificationValidator condition = new LegalEntityIdentificationValidator();
		ConditionError error = runAndFail(condition);
		String expected = "LegalEntityIdentificationValidator: Unable to find path $.data[0].contacts.emails[0].email";
		assertThat(error.getMessage(), containsString(expected));
	}

}
