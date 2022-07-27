package net.openid.conformance.apis.consent.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.consent.v2.ConsentDetailsIdentifiedByConsentIdValidatorV2;
import net.openid.conformance.openbanking_brasil.consent.v2.CreateNewConsentValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class CreateNewConsentValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {
	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/v2/createConsentResponse.json")
	public void validateStructure() {
		run(new CreateNewConsentValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/v2/createConsentResponse_missing_expirationDateTime.json")
	public void validateStructureWithMissingExpirationDateTimeField() {
		ConditionError error = runAndFail(new CreateNewConsentValidatorV2());
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("expirationDateTime", new CreateNewConsentValidatorV2().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/v2/createConsentResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new CreateNewConsentValidatorV2());
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("permissions", new CreateNewConsentValidatorV2().getApiName());
		assertThat(error.getMessage(), containsString(expected));

	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/v2/createConsentResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		ConditionError error = runAndFail(new ConsentDetailsIdentifiedByConsentIdValidatorV2());
		Assert.assertThat(error.getMessage(), StringContains.containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("creationDateTime",
			new ConsentDetailsIdentifiedByConsentIdValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/v2/createConsentResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		ConditionError error = runAndFail(new ConsentDetailsIdentifiedByConsentIdValidatorV2());
		Assert.assertThat(error.getMessage(), StringContains.containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("permissions",
			new ConsentDetailsIdentifiedByConsentIdValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/consent/createConsentResponse/v2/createConsentResponseMissingOptionalLinks.json")
	public void validateStructureWithOptionalLinks() {
		run(new CreateNewConsentValidatorV2());
	}

}
