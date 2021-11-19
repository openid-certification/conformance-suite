package net.openid.conformance.apis.raidiam.organisationAuthorityClaims;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.organisationAuthorityClaims.GetAuthorityClaimsByClaimIdValidator;
import net.openid.conformance.raidiam.validators.organisationAuthorityClaims.GetAuthorityClaimsValidator;
import net.openid.conformance.raidiam.validators.organisationAuthorityClaims.PostAuthorityClaimsValidator;
import net.openid.conformance.raidiam.validators.organisationAuthorityClaims.PutAuthorityClaimsByClaimIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

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
