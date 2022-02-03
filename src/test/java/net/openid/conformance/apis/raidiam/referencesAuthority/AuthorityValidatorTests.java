package net.openid.conformance.apis.raidiam.referencesAuthority;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.raidiam.validators.referencesAuthority.GetAuthorityByAuthorityIdValidator;
import net.openid.conformance.raidiam.validators.referencesAuthority.GetAuthorityValidator;
import net.openid.conformance.raidiam.validators.referencesAuthority.PostAuthorityValidator;
import net.openid.conformance.raidiam.validators.referencesAuthority.PutAuthorityByAuthorityIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class AuthorityValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthority/GetAuthorityByAuthorityIdResponse.json")
	public void validateGetAuthorityByAuthorityIdValidator() {
		GetAuthorityByAuthorityIdValidator condition = new GetAuthorityByAuthorityIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthority/GetAuthorityResponse.json")
	public void validateGetAuthorityValidator() {
		GetAuthorityValidator condition = new GetAuthorityValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthority/GetAuthorityResponse_maxLengthError.json")
	public void validateStructureWithWrongMaxLength() {
		GetAuthorityValidator condition = new GetAuthorityValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("AuthorityId",
			condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthority/PostAuthorityResponse.json")
	public void validatePostAuthorityValidator() {
		PostAuthorityValidator condition = new PostAuthorityValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthority/PutAuthorityByAuthorityIdResponse.json")
	public void validatePutAuthorityByAuthorityIdValidator() {
		PutAuthorityByAuthorityIdValidator condition = new PutAuthorityByAuthorityIdValidator();
		run(condition);
	}
}
