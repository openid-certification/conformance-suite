package net.openid.conformance.apis.registrationData;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.registrationData.LegalEntityQualificationResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/registrationData/legalEntityQualification/legalEntityQualificationResponse.json")
public class LegalEntityQualificationResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		LegalEntityQualificationResponseValidator condition = new LegalEntityQualificationResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityQualification/legalEntityQualificationResponseWithError.json")
	public void validateStructureWithMissingField() {
		LegalEntityQualificationResponseValidator condition = new LegalEntityQualificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("currency")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityQualification/legalEntityQualificationResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		LegalEntityQualificationResponseValidator condition = new LegalEntityQualificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("frequency")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityQualification/legalEntityQualificationResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		LegalEntityQualificationResponseValidator condition = new LegalEntityQualificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("date")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityQualification/legalEntityQualificationResponseWrongMaximum.json")
	public void validateStructureWrongMaximum() {
		LegalEntityQualificationResponseValidator condition = new LegalEntityQualificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("year")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityQualification/legalEntityQualificationResponseBigDouble.json")
	public void validateStructureWithBigDouble() {
		LegalEntityQualificationResponseValidator condition = new LegalEntityQualificationResponseValidator();
		run(condition);
	}
}
