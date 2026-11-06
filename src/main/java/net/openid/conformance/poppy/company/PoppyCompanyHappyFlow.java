package net.openid.conformance.poppy.company;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "poppy-company-happy-flow",
	displayName = "Poppy Company: happy flow (Direct Sign-In, Bearer token, Session header)",
	summary = "Fetches poppy.json, the protected resource metadata and the OAuth server metadata for the configured Company domain, signs the user in with Direct Sign-In (authorization code + PKCE S256, private_key_jwt with an ES256 key), then calls the configured protected resource once with a Bearer access token and a Poppy-Session header signed with a delegated key. The Personal Agent's client metadata document is served by the suite at the client_id URL shown in the test log; the Company must accept that client_id.",
	profile = "Poppy-0.4")
public class PoppyCompanyHappyFlow extends AbstractPoppyCompanyTestModule {
}
