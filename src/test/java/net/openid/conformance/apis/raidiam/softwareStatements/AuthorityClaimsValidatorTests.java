package net.openid.conformance.apis.raidiam.softwareStatements;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.softwareStatements.authorityClaims.GetStatementAuthorityClaimsByClaimsIdValidator;
import net.openid.conformance.raidiam.validators.softwareStatements.authorityClaims.GetStatementAuthorityClaimsValidator;
import net.openid.conformance.raidiam.validators.softwareStatements.authorityClaims.PostStatementAuthorityClaimsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class AuthorityClaimsValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/softwareStatements/authorityClaims/GetAuthorityClaimsByClaimIdResponse.json")
	public void validateGetAuthorityClaimsByClaimsIdValidator() {
		GetStatementAuthorityClaimsByClaimsIdValidator condition = new GetStatementAuthorityClaimsByClaimsIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/softwareStatements/authorityClaims/GetAuthorityClaimsResponse.json")
	public void validateGetAuthorityClaimsValidator() {
		GetStatementAuthorityClaimsValidator condition = new GetStatementAuthorityClaimsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/softwareStatements/authorityClaims/PostAuthorityClaimsResponse.json")
	public void validatePostAuthorityClaimsValidator() {
		PostStatementAuthorityClaimsValidator condition = new PostStatementAuthorityClaimsValidator();
		run(condition);
	}
}
