package net.openid.conformance.apis;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.registrationData.CorporateRelationshipResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/registrationData/corporateRelationshipResponse.json")
public class CorporateRelationshipResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		CorporateRelationshipResponseValidator condition = new CorporateRelationshipResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/corporateRelationshipResponse_with_missing_field_procurators.type.json")
	public void validateStructureWithMissingField() {
		CorporateRelationshipResponseValidator condition = new CorporateRelationshipResponseValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createElementNotFoundMessage("$.data.procurators[0].type");
		assertThat(error.getMessage(), containsString(expected));
	}

}
