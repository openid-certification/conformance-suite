package net.openid.conformance.apis.raidiam.organisationAuthorityClaims;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.raidiam.validators.organisationAuthorityClaims.GetAuthorityClaimsByClaimIdValidator;
import net.openid.conformance.raidiam.validators.organisationAuthorityClaims.GetAuthorityClaimsValidator;
import net.openid.conformance.raidiam.validators.organisationAuthorityClaims.PostAuthorityClaimsValidator;
import net.openid.conformance.raidiam.validators.organisationAuthorityClaims.PutAuthorityClaimsByClaimIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class AuthorityClaimsValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAuthorityClaims/GetAuthorityClaimsByClaimIdResponse.json")
	public void validateGetAuthorityClaimsByClaimIdValidator() {
		GetAuthorityClaimsByClaimIdValidator condition = new GetAuthorityClaimsByClaimIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAuthorityClaims/GetAuthorityClaimsResponse.json")
	public void validateGetAuthorityClaimsValidator() {
		GetAuthorityClaimsValidator condition = new GetAuthorityClaimsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAuthorityClaims/GetAuthorityClaimsResponse_maxLengthError.json")
	public void validatePostOrganisationAdminUsersCantBeNull() {
		GetAuthorityClaimsValidator condition = new GetAuthorityClaimsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("OrganisationId",
			condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAuthorityClaims/PostAuthorityClaimsResponse.json")
	public void validatePostAuthorityClaimsValidator() {
		PostAuthorityClaimsValidator condition = new PostAuthorityClaimsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAuthorityClaims/PutAuthorityClaimsByClaimIdResponse.json")
	public void validatePutAuthorityClaimsByClaimIdValidator() {
		PutAuthorityClaimsByClaimIdValidator condition = new PutAuthorityClaimsByClaimIdValidator();
		run(condition);
	}
}
