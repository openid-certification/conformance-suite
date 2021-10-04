package net.openid.conformance.apis.consent;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.consent.DeleteConsentByIdResponseWithErrorValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/consent/deleteConsentByIdResponseWithError.json")
public class DeleteConsentByIdResponseWithErrorValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		DeleteConsentByIdResponseWithErrorValidator condition = new DeleteConsentByIdResponseWithErrorValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/consent/deleteConsentByIdResponseWithErrorNoField.json")
	public void validateStructureWithMissingField() {
		DeleteConsentByIdResponseWithErrorValidator condition = new DeleteConsentByIdResponseWithErrorValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("detail")));
	}
}
