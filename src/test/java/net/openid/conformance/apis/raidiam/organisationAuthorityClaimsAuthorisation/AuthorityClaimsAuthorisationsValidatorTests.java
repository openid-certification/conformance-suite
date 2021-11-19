package net.openid.conformance.apis.raidiam.organisationAuthorityClaimsAuthorisation;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.organisationAuthorityClaimsAuthorisation.GetAuthorityClaimsAuthorisationsByAuthorisationIdValidator;
import net.openid.conformance.raidiam.validators.organisationAuthorityClaimsAuthorisation.GetAuthorityClaimsAuthorisationsValidator;
import net.openid.conformance.raidiam.validators.organisationAuthorityClaimsAuthorisation.PostAuthorityClaimsAuthorisationsValidator;
import net.openid.conformance.raidiam.validators.organisationAuthorityDomainClaims.GetAuthorityDomainClaimsByClaimIdValidator;
import net.openid.conformance.raidiam.validators.organisationAuthorityDomainClaims.GetAuthorityDomainClaimsValidator;
import net.openid.conformance.raidiam.validators.organisationAuthorityDomainClaims.PostAuthorityDomainClaimsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class AuthorityClaimsAuthorisationsValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAuthorityClaimsAuthorisation/GetAuthorityClaimsAuthorisationsByAuthorisationIdResponse.json")
	public void validateGetAuthorityClaimsAuthorisationsByAuthorisationIdValidator() {
		GetAuthorityClaimsAuthorisationsByAuthorisationIdValidator condition = new GetAuthorityClaimsAuthorisationsByAuthorisationIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAuthorityClaimsAuthorisation/GetAuthorityClaimsAuthorisationsResponse.json")
	public void validateGetAuthorityClaimsAuthorisationsValidator() {
		GetAuthorityClaimsAuthorisationsValidator condition = new GetAuthorityClaimsAuthorisationsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAuthorityClaimsAuthorisation/PostAuthorityClaimsAuthorisationsResponse.json")
	public void validatePostAuthorityClaimsAuthorisationsValidator() {
		PostAuthorityClaimsAuthorisationsValidator condition = new PostAuthorityClaimsAuthorisationsValidator();
		run(condition);
	}
}
