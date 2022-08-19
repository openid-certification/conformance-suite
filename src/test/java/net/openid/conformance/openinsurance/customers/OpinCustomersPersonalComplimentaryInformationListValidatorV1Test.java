package net.openid.conformance.openinsurance.customers;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersPersonalComplimentaryInformationListValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("openinsuranceResponses/customers/v1/personalComplimentaryInformation/naturalPersonRelationshipResponse.json")
public class OpinCustomersPersonalComplimentaryInformationListValidatorV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new OpinCustomersPersonalComplimentaryInformationListValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/personalComplimentaryInformation/naturalPersonRelationshipResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new OpinCustomersPersonalComplimentaryInformationListValidatorV1());
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("contract", new OpinCustomersPersonalComplimentaryInformationListValidatorV1().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/personalComplimentaryInformation/naturalPersonRelationshipResponseWrongPattern.json")
	public void validateStructureWithWrongPattern() {
		ConditionError error = runAndFail(new OpinCustomersPersonalComplimentaryInformationListValidatorV1());
		String expected = ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("updateDateTime", new OpinCustomersPersonalComplimentaryInformationListValidatorV1().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/personalComplimentaryInformation/naturalPersonRelationshipResponseWrongEnumValue.json")
	public void validateStructureWithWrongEnumValue() {
		ConditionError error = runAndFail(new OpinCustomersPersonalComplimentaryInformationListValidatorV1());
		String expected = ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("type", new OpinCustomersPersonalComplimentaryInformationListValidatorV1().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}
}
