package net.openid.conformance.openinsurance.customers;

	import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
	import net.openid.conformance.condition.ConditionError;
	import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
    import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersBusinessComplimentaryInformationListValidatorV1;
    import net.openid.conformance.util.UseResurce;
	import org.junit.Test;

	import static org.hamcrest.MatcherAssert.assertThat;
	import static org.hamcrest.Matchers.containsString;

@UseResurce("openinsuranceResponses/customers/v1/businessComplimentaryInformation/businessRelationshipResponse.json")
public class OpinCustomersBusinessComplimentaryInformationListValidatorV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new OpinCustomersBusinessComplimentaryInformationListValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/businessComplimentaryInformation/businessRelationshipResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new OpinCustomersBusinessComplimentaryInformationListValidatorV1());
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("updateDateTime", new OpinCustomersBusinessComplimentaryInformationListValidatorV1().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/businessComplimentaryInformation/businessRelationshipResponseWrongEnumValue.json")
	public void validateStructureWithWrongPattern() {
		ConditionError error = runAndFail(new OpinCustomersBusinessComplimentaryInformationListValidatorV1());
		String expected = ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("updateDateTime", new OpinCustomersBusinessComplimentaryInformationListValidatorV1().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/businessComplimentaryInformation/businessRelationshipResponseWrongPattern.json")
	public void validateStructureWithWrongEnumValue() {
		ConditionError error = runAndFail(new OpinCustomersBusinessComplimentaryInformationListValidatorV1());
		String expected = ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("type", new OpinCustomersBusinessComplimentaryInformationListValidatorV1().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}
}
