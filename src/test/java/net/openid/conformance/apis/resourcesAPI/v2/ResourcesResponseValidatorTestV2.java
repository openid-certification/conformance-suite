package net.openid.conformance.apis.resourcesAPI.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v2.ResourcesResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/resourcesAPI/resourcesAPIResponse.json")
public class ResourcesResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {

		run(new ResourcesResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/resourcesAPI/resourcesAPIResponseWithError.json")
	public void validateStructureWithMissingField() {

		ConditionError error = runAndFail(new ResourcesResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("type", new ResourcesResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/resourcesAPI/resourcesAPIResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {

		ConditionError error = runAndFail(new ResourcesResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("type", new ResourcesResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/resourcesAPI/resourcesAPIResponseWrongPattern.json")
	public void validateStructureWithWrongPattern() {

		ConditionError error = runAndFail(new ResourcesResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("resourceId", new ResourcesResponseValidatorV2().getApiName())));
	}
}
