package net.openid.conformance.vci10issuer;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-happy-flow-skip-notification",
	displayName = "OID4VCI 1.0: Issuer happy flow without notification",
	summary = """
		This test case validates the standard credential issuance flow using an emulated wallet, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification.
		It begins by retrieving metadata from both the Credential Issuer and the OAuth 2.0 Authorization Server.
		An authorization request is initiated using Pushed Authorization Requests (PAR), and an access token is obtained.
		The test then retrieves a nonce from the Credential Endpoint, constructs a DPoP proof JWT bound to the nonce,
		and successfully requests a credential from the Credential Endpoint.
		This test skips sending a notification to the issuer. Sending a notification is optional for the wallet, so the issuer must be able to tolerate never receiving a notification.
		""",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerHappyFlowWithSkipNotification extends VCIIssuerHappyFlow {

	@Override
	protected void sendNotificationIfSupported() {
		// skip sending notification
	}
}
