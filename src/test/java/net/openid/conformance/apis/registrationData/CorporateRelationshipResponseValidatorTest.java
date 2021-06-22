package net.openid.conformance.apis.registrationData;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.registrationData.CorporateRelationshipResponseValidator;
import net.openid.conformance.openbanking_brasil.registrationData.NaturalPersonIdentificationResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class CorporateRelationshipResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/registrationData/corporateRelationship/corporateRelationshipResponseOK.json")
	public void validateStructureOK() {
		CorporateRelationshipResponseValidator condition = new CorporateRelationshipResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/corporateRelationship/corporateRelationshipResponseOK(with_missing_field).json")
	public void validateStructureWithMissingFieldOK() {
		CorporateRelationshipResponseValidator condition = new CorporateRelationshipResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("type")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/corporateRelationship/errors/corporateRelationshipResponseError(PatternNotMatch).json")
	public void validateStructurePatternNotMatch() {
		CorporateRelationshipResponseValidator condition = new CorporateRelationshipResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createFieldValueNotMatchPatternMessage("number")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/corporateRelationship/errors/corporateRelationshipResponseError(enumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		CorporateRelationshipResponseValidator condition = new CorporateRelationshipResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createFieldValueNotMatchEnumerationMessage(
				"productsServicesType")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/corporateRelationship/errors/corporateRelationshipResponseError(excessMaxItems).json")
	public void validateStructureExcessMaxItems() {
		CorporateRelationshipResponseValidator condition = new CorporateRelationshipResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createArrayIsMoreThanMaxItemsMessage(
				"productsServicesType")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/corporateRelationship/errors/corporateRelationshipResponseError(lessMinItems).json")
	public void validateStructureLessMinItems() {
		CorporateRelationshipResponseValidator condition = new CorporateRelationshipResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(condition.createArrayIsLessThanMaxItemsMessage(
				"accounts")));
	}
}
