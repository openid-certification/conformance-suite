package net.openid.conformance.poppy.agent;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "poppy-agent-happy-flow",
	displayName = "Poppy Personal Agent: happy flow (Direct Sign-In, Bearer token, Session header)",
	summary = "The suite acts as a Company at the host root: it serves poppy.json, protected resource metadata and OAuth server metadata, signs the Personal Agent in with Direct Sign-In (PKCE S256, private_key_jwt), and verifies the Poppy-Session and Poppy-Version headers on one API call. The test configuration alias must be 'root-well-known' so that /.well-known/poppy.json is served at the host root; one such test can run at a time on a host.",
	profile = "Poppy-0.4")
public class PoppyAgentHappyFlow extends AbstractPoppyAgentTestModule {
}
