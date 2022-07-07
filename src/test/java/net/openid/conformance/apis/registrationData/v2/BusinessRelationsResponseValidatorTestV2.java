package net.openid.conformance.apis.registrationData.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.registrationData.v2.BusinessRelationsResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class BusinessRelationsResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/corporateRelationshipV2/corporateRelationshipResponseOK.json")
	public void validateStructureOK() {
		run(new BusinessRelationsResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/corporateRelationshipV2/corporateRelationshipResponseOK(with_missing_field).json")
	public void validateStructureWithMissingFieldOK() {

		run(new BusinessRelationsResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/corporateRelationshipV2/errors/corporateRelationshipResponseError(PatternNotMatch).json")
	public void validateStructurePatternNotMatch() {

		ConditionError error = runAndFail(new BusinessRelationsResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("number", new BusinessRelationsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/corporateRelationshipV2/errors/corporateRelationshipResponseError(enumNotMatch).json")
	public void validateStructureEnumNotMatch() {

		ConditionError error = runAndFail(new BusinessRelationsResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage(
				"productsServicesType", new BusinessRelationsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/corporateRelationshipV2/errors/corporateRelationshipResponseError(excessMaxItems).json")
	public void validateStructureExcessMaxItems() {

		ConditionError error = runAndFail(new BusinessRelationsResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createArrayIsMoreThanMaxItemsMessage(
				"productsServicesType", new BusinessRelationsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/corporateRelationshipV2/errors/corporateRelationshipResponseError(lessMinItems).json")
	public void validateStructureLessMinItems() {

		ConditionError error = runAndFail(new BusinessRelationsResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createArrayIsLessThanMaxItemsMessage(
				"productsServicesType", new BusinessRelationsResponseValidatorV2().getApiName())));
	}
}
