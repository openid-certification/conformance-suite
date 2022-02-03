package net.openid.conformance.apis.raidiam.softwareStatements;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.raidiam.validators.softwareStatements.authorityClaims.GetStatementAuthorityClaimsByClaimsIdValidator;
import net.openid.conformance.raidiam.validators.softwareStatements.authorityClaims.GetStatementAuthorityClaimsValidator;
import net.openid.conformance.raidiam.validators.softwareStatements.authorityClaims.PostStatementAuthorityClaimsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

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
	@UseResurce("jsonResponses/raidiam/softwareStatements/authorityClaims/GetAuthorityClaimsResponse_maxLengthError.json")
	public void validateStructureWithWrongMaxLength() {
		GetStatementAuthorityClaimsValidator condition = new GetStatementAuthorityClaimsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("SoftwareStatementId",
			condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/raidiam/softwareStatements/authorityClaims/PostAuthorityClaimsResponse.json")
	public void validatePostAuthorityClaimsValidator() {
		PostStatementAuthorityClaimsValidator condition = new PostStatementAuthorityClaimsValidator();
		run(condition);
	}
}
