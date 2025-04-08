package net.openid.conformance.openid.federation;

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
public class OpenIDFederationAutomaticClientRegistrationWithParTest extends AbstractOpenIDFederationAutomaticClientRegistrationTest {

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return FAPIAuthRequestMethod.PUSHED;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return HttpMethod.GET;
	}

}
