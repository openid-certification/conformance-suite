package net.openid.conformance.apis.consent;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.consent.ConsentDetailsIdentifiedByConsentIdValidator;
import net.openid.conformance.openbanking_brasil.consent.CreateNewConsentValidator;
//import net.openid.conformance.openbanking_brasil.consents.CreateNewConsentValidatorWithError;
import net.openid.conformance.util.UseResurce;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/consents/createConsentResponse.json")
public class CreateNewConsentValidatorTest extends AbstractJsonResponseConditionUnitTest {
	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/createConsentResponse.json")
	public void validateStructure() {
		CreateNewConsentValidator condition = new CreateNewConsentValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/createConsentResponse_missing_expirationDateTime.json")
	public void validateStructureWithMissingExpirationDateTimeField() {
		CreateNewConsentValidator condition = new CreateNewConsentValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createElementNotFoundMessage("expirationDateTime");
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/createConsentResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		CreateNewConsentValidator condition = new CreateNewConsentValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createElementNotFoundMessage("permissions");
		assertThat(error.getMessage(), containsString(expected));

	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/createConsentResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		ConsentDetailsIdentifiedByConsentIdValidator condition = new ConsentDetailsIdentifiedByConsentIdValidator();
		ConditionError error = runAndFail(condition);
		Assert.assertThat(error.getMessage(), StringContains.containsString(condition.createFieldValueNotMatchPatternMessage("creationDateTime")));
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/createConsentResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		ConsentDetailsIdentifiedByConsentIdValidator condition = new ConsentDetailsIdentifiedByConsentIdValidator();
		ConditionError error = runAndFail(condition);
		Assert.assertThat(error.getMessage(), StringContains.containsString(condition.createFieldValueNotMatchEnumerationMessage("permissions")));
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/createConsentResponseTooLongExpiration.json")
	public void validateStructureWithTooLongExpiration() {
		CreateNewConsentValidator condition = new CreateNewConsentValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createFieldValueIsOlderThanLimit("expirationDateTime");
		Assert.assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/createConsentResponseMissingLinks.json")
	public void validateStructureWithMissingLinksObject() {
		CreateNewConsentValidator condition = new CreateNewConsentValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createElementNotFoundMessage("$.links");
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/createConsentResponseMissingOptionalLinks.json")
	public void validateStructureWithOptionalLinks() {
		CreateNewConsentValidator condition = new CreateNewConsentValidator();
		run(condition);
	}

}
