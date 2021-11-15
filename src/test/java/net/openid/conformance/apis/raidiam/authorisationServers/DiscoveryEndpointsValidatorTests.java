package net.openid.conformance.apis.raidiam.authorisationServers;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.authorisationServers.discoveryEndpointsAPI.GetDiscoveryEndpointsByEndpointIDValidator;
import net.openid.conformance.raidiam.validators.authorisationServers.discoveryEndpointsAPI.GetDiscoveryEndpointsValidator;
import net.openid.conformance.raidiam.validators.authorisationServers.discoveryEndpointsAPI.PostDiscoveryEndpointsValidator;
import net.openid.conformance.raidiam.validators.authorisationServers.discoveryEndpointsAPI.PutDiscoveryEndpointsByEndpointIDValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class DiscoveryEndpointsValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/discoveryEndpointsAPI/GetDiscoveryEndpointsByEndpointIDResponse.json")
	public void validateGetDiscoveryEndpointsByEndpointIDValidator() {
		GetDiscoveryEndpointsByEndpointIDValidator condition = new GetDiscoveryEndpointsByEndpointIDValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/discoveryEndpointsAPI/GetDiscoveryEndpointsResponse.json")
	public void validateGetDiscoveryEndpointsValidator() {
		GetDiscoveryEndpointsValidator condition = new GetDiscoveryEndpointsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/discoveryEndpointsAPI/PostDiscoveryEndpointsResponse.json")
	public void validatePostDiscoveryEndpointsValidator() {
		PostDiscoveryEndpointsValidator condition = new PostDiscoveryEndpointsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/discoveryEndpointsAPI/PutDiscoveryEndpointsByEndpointIDResponse.json")
	public void validatePutDiscoveryEndpointsByEndpointIDValidator() {
		PutDiscoveryEndpointsByEndpointIDValidator condition = new PutDiscoveryEndpointsByEndpointIDValidator();
		run(condition);
	}
}
