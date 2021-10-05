package net.openid.conformance.apis.registrationData;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.registrationData.BusinessRelationsResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class BusinessRelationsResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/registrationData/corporateRelationship/corporateRelationshipResponseOK.json")
	public void validateStructureOK() {
		BusinessRelationsResponseValidator condition = new BusinessRelationsResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/corporateRelationship/corporateRelationshipResponseOK(with_missing_field).json")
	public void validateStructureWithMissingFieldOK() {
		BusinessRelationsResponseValidator condition = new BusinessRelationsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("type")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/corporateRelationship/errors/corporateRelationshipResponseError(PatternNotMatch).json")
	public void validateStructurePatternNotMatch() {
		BusinessRelationsResponseValidator condition = new BusinessRelationsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createFieldValueNotMatchPatternMessage("number")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/corporateRelationship/errors/corporateRelationshipResponseError(enumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		BusinessRelationsResponseValidator condition = new BusinessRelationsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createFieldValueNotMatchEnumerationMessage(
				"productsServicesType")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/corporateRelationship/errors/corporateRelationshipResponseError(excessMaxItems).json")
	public void validateStructureExcessMaxItems() {
		BusinessRelationsResponseValidator condition = new BusinessRelationsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createArrayIsMoreThanMaxItemsMessage(
				"productsServicesType")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/corporateRelationship/errors/corporateRelationshipResponseError(lessMinItems).json")
	public void validateStructureLessMinItems() {
		BusinessRelationsResponseValidator condition = new BusinessRelationsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createArrayIsLessThanMaxItemsMessage(
				"accounts")));
	}
}
