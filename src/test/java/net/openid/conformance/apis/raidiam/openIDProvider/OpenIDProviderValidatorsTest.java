package net.openid.conformance.apis.raidiam.openIDProvider;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.openIDProvider.*;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OpenIDProviderValidatorsTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/openIDProvider/PostBackChannelResponse.json")
	public void validatePostBackChannelValidator() {
		PostBackChannelValidator condition = new PostBackChannelValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/openIDProvider/PostRequestResponse.json")
	public void validatePostRequestValidator() {
		PostRequestValidator condition = new PostRequestValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/openIDProvider/PostTokenIntrospectionResponse.json")
	public void validatePostTokenIntrospectionValidator() {
		PostTokenIntrospectionValidator condition = new PostTokenIntrospectionValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/openIDProvider/PostTokenResponse.json")
	public void validatePostTokenValidator() {
		PostTokenValidator condition = new PostTokenValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/openIDProvider/PostDeviceAuthResponse.json")
	public void validatePostDeviceAuthValidator() {
		PostDeviceAuthValidator condition = new PostDeviceAuthValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/openIDProvider/GetMeResponse.json")
	public void validateGetMeValidator() {
		GetMeValidator condition = new GetMeValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/openIDProvider/GetWellKnownOpenIdConfigurationResponse.json")
	public void validateGetWellKnownOpenIdConfigurationValidator() {
		GetWellKnownOpenIdConfigurationValidator condition = new GetWellKnownOpenIdConfigurationValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/openIDProvider/PostRegResponse.json")
	public void validatePostRegValidator() {
		PostRegValidator condition = new PostRegValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/openIDProvider/PutRegByClientIdResponse.json")
	public void validatePutRegByClientIdValidator() {
		PutRegByClientIdValidator condition = new PutRegByClientIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/openIDProvider/GetRegByClientIdResponse.json")
	public void validateGetRegByClientIdValidator() {
		GetRegByClientIdValidator condition = new GetRegByClientIdValidator();
		run(condition);
	}
}
