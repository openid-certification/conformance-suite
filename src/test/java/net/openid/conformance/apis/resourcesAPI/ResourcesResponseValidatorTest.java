package net.openid.conformance.apis.resourcesAPI;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v1.ResourcesResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/resourcesAPI/resourcesAPIResponse.json")
public class ResourcesResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		ResourcesResponseValidator condition = new ResourcesResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/resourcesAPI/resourcesAPIResponseWithError.json")
	public void validateStructureWithMissingField() {
		ResourcesResponseValidator condition = new ResourcesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("type", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/resourcesAPI/resourcesAPIResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		ResourcesResponseValidator condition = new ResourcesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("type", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/resourcesAPI/resourcesAPIResponseWrongPattern.json")
	public void validateStructureWithWrongPattern() {
		ResourcesResponseValidator condition = new ResourcesResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("resourceId", condition.getApiName())));
	}
}
