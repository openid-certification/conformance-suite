package net.openid.conformance.openinsurance.productsNServices;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.productsNServices.GetPersonValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class PersonValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/person/GetPersonResponse.json")
	public void validateStructure() {
		GetPersonValidator condition = new GetPersonValidator();
		run(condition);
	}

	@Test
	@UseResurce("openinsuranceResponses/person/GetPersonResponse(MissOptional).json")
	public void validateStructureMissOptionalField() {
		GetPersonValidator condition = new GetPersonValidator();
		run(condition);
	}

	@Test
	@UseResurce("openinsuranceResponses/person/GetPersonResponse(WrongEnum).json")
	public void validateStructureWrongEnumInStringArray() {
		GetPersonValidator condition = new GetPersonValidator();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage(
			"indemnityPaymentMethod", condition.getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("openinsuranceResponses/person/GetPersonResponse(MissingMandatoryField_pmbacUpdateIndex).json")
	public void validateStructureMissingMandatoryFilledPmbacUpdateIndex() {
		GetPersonValidator condition = new GetPersonValidator();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createElementNotFoundMessage(
			"pmbacUpdateIndex", condition.getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("openinsuranceResponses/person/GetPersonResponse(MissingMandatoryField_reclaimTable).json")
	public void validateStructureMissingMandatoryFilledReclaimTable() {
		GetPersonValidator condition = new GetPersonValidator();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createElementNotFoundMessage(
			"reclaimTable", condition.getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("openinsuranceResponses/person/GetPersonResponse(MissingMandatoryField_reclaimTable)OK.json")
	public void validateStructureMissingOptionalReclaimTableOK() {
		GetPersonValidator condition = new GetPersonValidator();
		run(condition);
	}
}
