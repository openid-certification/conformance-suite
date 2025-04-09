package net.openid.conformance.openid.federation;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

@PublishTestModule(
		testName = "openid-federation-automatic-client-registration-with-jar-and-post",
		displayName = "openid-federation-automatic-client-registration-with-jar-and-post",
		summary = "The test acts as an RP wanting to perform automatic client registration with an OP, " +
			"with JAR and HTTP POST to the authorization endpoint",
		profile = "OIDFED",
		configurationFields = {
			"client.jwks",
			"client.trust_chain",
			"federation.entity_identifier",
			"federation.trust_anchor",
			"federation.trust_anchor_jwks",
			"federation.authority_hints",
			"internal.op_to_rp_mode"
		}
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationWithJarAndPostTest extends OpenIDFederationAutomaticClientRegistrationTest {

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return FAPIAuthRequestMethod.BY_VALUE;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return HttpMethod.POST;
	}

}
