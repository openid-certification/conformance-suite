package net.openid.conformance.apis.consent.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.consent.v2.DeleteConsentByIdResponseWithErrorValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/consent/deleteConsent/v2/deleteConsentByIdResponseWithError.json")
public class DeleteConsentByIdResponseWithErrorValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new DeleteConsentByIdResponseWithErrorValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/consent/deleteConsent/v2/deleteConsentByIdResponseWithErrorNoField.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new DeleteConsentByIdResponseWithErrorValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createElementNotFoundMessage("detail", new DeleteConsentByIdResponseWithErrorValidatorV2().getApiName())));
	}
}
