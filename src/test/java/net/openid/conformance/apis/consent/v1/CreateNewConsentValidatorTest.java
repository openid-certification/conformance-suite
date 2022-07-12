package net.openid.conformance.apis.consent.v1;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.consent.v1.ConsentDetailsIdentifiedByConsentIdValidator;
import net.openid.conformance.openbanking_brasil.consent.v1.CreateNewConsentValidator;
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
	@UseResurce("jsonResponses/consent/createConsentResponse/v1/createConsentResponse.json")
	public void validateStructure() {
		CreateNewConsentValidator condition = new CreateNewConsentValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/v1/createConsentResponse_missing_expirationDateTime.json")
	public void validateStructureWithMissingExpirationDateTimeField() {
		CreateNewConsentValidator condition = new CreateNewConsentValidator();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("expirationDateTime", condition.getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/v1/createConsentResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		CreateNewConsentValidator condition = new CreateNewConsentValidator();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("permissions", condition.getApiName());
		assertThat(error.getMessage(), containsString(expected));

	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/v1/createConsentResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		ConsentDetailsIdentifiedByConsentIdValidator condition = new ConsentDetailsIdentifiedByConsentIdValidator();
		ConditionError error = runAndFail(condition);
		Assert.assertThat(error.getMessage(), StringContains.containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("creationDateTime",
			condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/v1/createConsentResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		ConsentDetailsIdentifiedByConsentIdValidator condition = new ConsentDetailsIdentifiedByConsentIdValidator();
		ConditionError error = runAndFail(condition);
		Assert.assertThat(error.getMessage(), StringContains.containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("permissions",
			condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/v1/createConsentResponseTooLongExpiration.json")
	public void validateStructureWithTooLongExpiration() {
		CreateNewConsentValidator condition = new CreateNewConsentValidator();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createFieldValueIsOlderThanLimit("expirationDateTime",
			condition.getApiName());
		Assert.assertThat(error.getMessage(), containsString(expected));
	}

	// @Test
	// @UseResurce("jsonResponses/consent/createConsentResponse/createConsentResponseMissingLinks.json")
	// public void validateStructureWithMissingLinksObject() {
	// 	CreateNewConsentValidator condition = new CreateNewConsentValidator();
	// 	ConditionError error = runAndFail(condition);
	// 	String expected = condition.createElementNotFoundMessage("$.links");
	// 	assertThat(error.getMessage(), containsString(expected));
	// }

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/v1/createConsentResponseMissingOptionalLinks.json")
	public void validateStructureWithOptionalLinks() {
		CreateNewConsentValidator condition = new CreateNewConsentValidator();
		run(condition);
	}

}