package net.openid.conformance.apis.registrationData;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.registrationData.BusinessQualificationResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/registrationData/legalEntityQualification/legalEntityQualificationResponse.json")
public class BusinessQualificationResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		BusinessQualificationResponseValidator condition = new BusinessQualificationResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityQualification/legalEntityQualificationResponseWithError.json")
	public void validateStructureWithMissingField() {
		BusinessQualificationResponseValidator condition = new BusinessQualificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("currency")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityQualification/legalEntityQualificationResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		BusinessQualificationResponseValidator condition = new BusinessQualificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("frequency")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityQualification/legalEntityQualificationResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		BusinessQualificationResponseValidator condition = new BusinessQualificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("date")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityQualification/legalEntityQualificationResponseWrongMaximum.json")
	public void validateStructureWrongMaximum() {
		BusinessQualificationResponseValidator condition = new BusinessQualificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("year")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityQualification/legalEntityQualificationResponseBigDouble.json")
	public void validateStructureWithBigDouble() {
		BusinessQualificationResponseValidator condition = new BusinessQualificationResponseValidator();
		run(condition);
	}
}
