package net.openid.conformance.openinsurance.consents;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.consents.v1.OpinCreateNewConsentValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class OpinCreateNewConsentValidatorV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/consents/v1/createConsent/createConsentResponse.json")
	public void validateStructure() {
		run(new OpinCreateNewConsentValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/consents/v1/createConsent/createConsentResponse_missing_expirationDateTime.json")
	public void validateStructureWithMissingExpirationDateTimeField() {
		ConditionError error = runAndFail(new OpinCreateNewConsentValidatorV1());
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("expirationDateTime", new OpinCreateNewConsentValidatorV1().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("openinsuranceResponses/consents/v1/createConsent/createConsentResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		ConditionError error = runAndFail(new OpinCreateNewConsentValidatorV1());
		String expected = ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("permissions", new OpinCreateNewConsentValidatorV1().getApiName());
		assertThat(error.getMessage(), containsString(expected));

	}

	@Test
	@UseResurce("openinsuranceResponses/consents/v1/createConsent/createConsentResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		ConditionError error = runAndFail(new OpinCreateNewConsentValidatorV1());
		String expected = ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("consentId", new OpinCreateNewConsentValidatorV1().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}
}
