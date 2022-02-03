package net.openid.conformance.openinsurance.productsNServices;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.productsNServices.GetLifePensionValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class LifePensionValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/lifePension/GetLifePensionResponse.json")
	public void validateStructure() {
		run(new GetLifePensionValidator());
	}

	@Test
	@UseResurce("openinsuranceResponses/lifePension/GetLifePensionResponse_maxLengthError.json")
	public void validateStructureWithWrongMaxLength() {
		GetLifePensionValidator condition = new GetLifePensionValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("brand",
			condition.getApiName())));
	}
}
