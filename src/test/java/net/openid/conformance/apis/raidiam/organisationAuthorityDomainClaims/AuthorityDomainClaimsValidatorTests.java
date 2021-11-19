package net.openid.conformance.apis.raidiam.organisationAuthorityDomainClaims;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.organisationAuthorityDomainClaims.GetAuthorityDomainClaimsByClaimIdValidator;
import net.openid.conformance.raidiam.validators.organisationAuthorityDomainClaims.GetAuthorityDomainClaimsValidator;
import net.openid.conformance.raidiam.validators.organisationAuthorityDomainClaims.PostAuthorityDomainClaimsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class AuthorityDomainClaimsValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAuthorityDomainClaims/GetAuthorityDomainClaimsByClaimIdResponse.json")
	public void validateGetAuthorityDomainClaimsByClaimIdValidator() {
		GetAuthorityDomainClaimsByClaimIdValidator condition = new GetAuthorityDomainClaimsByClaimIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAuthorityDomainClaims/GetAuthorityDomainClaimsResponse.json")
	public void validateGetAuthorityDomainClaimsValidator() {
		GetAuthorityDomainClaimsValidator condition = new GetAuthorityDomainClaimsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAuthorityDomainClaims/PostAuthorityDomainClaimsResponse.json")
	public void validatePostAuthorityDomainClaimsValidator() {
		PostAuthorityDomainClaimsValidator condition = new PostAuthorityDomainClaimsValidator();
		run(condition);
	}
}
