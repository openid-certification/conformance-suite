package net.openid.conformance.vciid2issuer;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vci-id2-issuer-happy-flow",
	displayName = "OID4VCIID2: Happy flow test",
	summary = "Expects the issuer to issue a verifiable credential according to the OID4VCI specification.",
	profile = "OID4VCI-ID2",
	configurationFields = {
	}
)
public class VCIID2IssuerTestHappyPath extends AbstractVciId2TestModule {

	@Override
	public void start() {
		setStatus(Status.RUNNING);
	}
}
