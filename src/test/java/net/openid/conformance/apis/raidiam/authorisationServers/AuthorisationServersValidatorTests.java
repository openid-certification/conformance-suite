package net.openid.conformance.apis.raidiam.authorisationServers;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.authorisationServers.base.GetAuthorisationServersByServerIdValidator;
import net.openid.conformance.raidiam.validators.authorisationServers.base.GetAuthorisationServersValidator;
import net.openid.conformance.raidiam.validators.authorisationServers.base.PostAuthorisationServersValidator;
import net.openid.conformance.raidiam.validators.authorisationServers.base.PutAuthorisationServersByServerIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class AuthorisationServersValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/base/GetAuthorisationServersByServerIdResponse.json")
	public void validateGetAuthorisationServersByServerIdValidator() {
		GetAuthorisationServersByServerIdValidator condition = new GetAuthorisationServersByServerIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/base/GetAuthorisationServersResponse.json")
	public void validateGetAuthorisationServersValidator() {
		GetAuthorisationServersValidator condition = new GetAuthorisationServersValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/base/PostAuthorisationServersResponse.json")
	public void validatePostAuthorisationServersValidator() {
		PostAuthorisationServersValidator condition = new PostAuthorisationServersValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/base/PutAuthorisationServersByServerIdResponse.json")
	public void validatePutAuthorisationServersByServerIdValidator() {
		PutAuthorisationServersByServerIdValidator condition = new PutAuthorisationServersByServerIdValidator();
		run(condition);
	}
}
