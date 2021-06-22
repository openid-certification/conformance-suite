package net.openid.conformance.apis.registrationData;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.account.AccountTransactionsValidator;
import net.openid.conformance.openbanking_brasil.registrationData.NaturalPersonIdentificationResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


public class NaturalPersonIdentificationResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/registrationData/personIdentification/naturalPersonIdentificationResponseOK.json")
	public void validateStructure() {
		NaturalPersonIdentificationResponseValidator condition = new NaturalPersonIdentificationResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/personIdentification/naturalPersonIdentificationResponseOK(missingNonMandatoryField).json")
	public void validateStructureWithMissingNotMandatoryField() {
		NaturalPersonIdentificationResponseValidator condition = new NaturalPersonIdentificationResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/personIdentification/errors/naturalPersonIdentificationResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		NaturalPersonIdentificationResponseValidator condition = new NaturalPersonIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createElementNotFoundMessage("sex");
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/personIdentification/errors/naturalPersonIdentificationResponse(PatternNotMatch).json")
	public void validateStructurePatternNotMatch() {
		NaturalPersonIdentificationResponseValidator condition = new NaturalPersonIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createFieldValueNotMatchPatternMessage("birthDate")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/personIdentification/errors/naturalPersonIdentificationResponse(ExessMaxLength).json")
	public void validateStructureExcessMaxLength() {
		NaturalPersonIdentificationResponseValidator condition = new NaturalPersonIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createFieldValueIsMoreThanMaxLengthMessage(
				"countryCode")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/personIdentification/errors/naturalPersonIdentificationResponse(EnumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		NaturalPersonIdentificationResponseValidator condition = new NaturalPersonIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createFieldValueNotMatchEnumerationMessage(
				"sex")));
	}
}
