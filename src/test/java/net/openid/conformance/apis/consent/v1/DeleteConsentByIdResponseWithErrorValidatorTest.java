package net.openid.conformance.apis.consent.v1;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.consent.v1.DeleteConsentByIdResponseWithErrorValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/consent/deleteConsent/v1/deleteConsentByIdResponseWithError.json")
public class DeleteConsentByIdResponseWithErrorValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		DeleteConsentByIdResponseWithErrorValidator condition = new DeleteConsentByIdResponseWithErrorValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/consent/deleteConsent/v1/deleteConsentByIdResponseWithErrorNoField.json")
	public void validateStructureWithMissingField() {
		DeleteConsentByIdResponseWithErrorValidator condition = new DeleteConsentByIdResponseWithErrorValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createElementNotFoundMessage("detail", condition.getApiName())));
	}
}
