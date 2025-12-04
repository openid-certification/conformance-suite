package net.openid.conformance.openid.federation;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

@PublishTestModule(
	testName = "openid-federation-automatic-client-registration-with-jar-and-get",
	displayName = "OpenID Federation OP test: Automatic client registration with JAR and HTTP GET",
	summary = "The test acts as an RP wanting to perform automatic client registration with an OP, " +
		"with JAR and HTTP GET to the authorization endpoint",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_identifier",
		"federation.trust_anchor",
		"federation.rp_ec_jwks",
		"federation.rp_client_jwks",
		"federation.rp_entity_identifier_host_override",
		"federation_trust_anchor.trust_anchor_jwks",
		"internal.op_to_rp_mode",
	}
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationWithJarAndGetTest extends OpenIDFederationAutomaticClientRegistrationTest {

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return FAPIAuthRequestMethod.BY_VALUE;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return HttpMethod.GET;
	}

}
