package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

@PublishTestModule(
	testName = "openid-federation-automatic-client-registration-with-par",
	displayName = "openid-federation-automatic-client-registration-with-par",
	summary = "The test acts as an RP wanting to perform automatic client registration with an OP, with PAR",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationWithParTest extends OpenIDFederationAutomaticClientRegistrationTest {

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return FAPIAuthRequestMethod.PUSHED;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return HttpMethod.GET;
	}

	@Override
	protected void verifyTestConditions() {
		JsonElement parEndpoint = env.getElementFromObject("primary_entity_statement_jwt", "claims.metadata.openid_provider.pushed_authorization_request_endpoint");
		if (parEndpoint == null) {
			fireTestSkipped("The server does not support the 'pushed authorization request' endpoint");
		}
	}
}
