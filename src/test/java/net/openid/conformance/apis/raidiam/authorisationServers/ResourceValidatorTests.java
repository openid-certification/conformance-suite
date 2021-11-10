package net.openid.conformance.apis.raidiam.authorisationServers;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.authorisationServers.resourcesAPI.GetResourceByResourceIdValidator;
import net.openid.conformance.raidiam.validators.authorisationServers.resourcesAPI.GetResourceValidator;
import net.openid.conformance.raidiam.validators.authorisationServers.resourcesAPI.PostResourceValidator;
import net.openid.conformance.raidiam.validators.authorisationServers.resourcesAPI.PutResourceByResourceIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class ResourceValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/resourcesAPI/GetResourceByResourceIdResponse.json")
	public void validateGetResourceByResourceIdValidator() {
		GetResourceByResourceIdValidator condition = new GetResourceByResourceIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/resourcesAPI/GetResourceResponse.json")
	public void validateGetResourceValidator() {
		GetResourceValidator condition = new GetResourceValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/resourcesAPI/PostResourceResponse.json")
	public void validatePostResourceValidator() {
		PostResourceValidator condition = new PostResourceValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/resourcesAPI/PutResourceByResourceIdResponse.json")
	public void validatePutResourceByResourceIdValidator() {
		PutResourceByResourceIdValidator condition = new PutResourceByResourceIdValidator();
		run(condition);
	}
}
