package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VCIClientAuthType;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-happy-flow-multiple-clients",
	displayName = "OID4VCI 1.0: Issuer happy flow with multiple clients",
	summary = """
	This test case validates the standard credential issuance flow using an emulated wallet, as defined in the OpenID
	for Verifiable Credential Issuance (OpenID4VCI) specification. It uses two different clients to obtain the credentials consecutively.
	It begins by retrieving metadata from both the Credential Issuer and the OAuth 2.0 Authorization Server. An authorization request is initiated using
	Pushed Authorization Requests (PAR), and an access token is obtained.

	The test then retrieves a nonce from the Credential Endpoint, constructs a DPoP proof JWT bound to the nonce,
	and successfully requests a credential from the Credential Endpoint.
	""",
	profile = "OID4VCI-1_0"
)
@VariantHidesConfigurationFields(parameter = VCIClientAuthType.class, value = "client_attestation",
	configurationFields = {"client.jwks", "client2.jwks"})
public class VCIIssuerHappyFlowMultipleClients extends AbstractVCIIssuerMultipleClient {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		if (isOpenId) {
			Command cmd = new Command();

			if (isSecondClient()) {
				cmd.putInteger("requested_nonce_length", 43);
			} else {
				cmd.removeNativeValue("requested_nonce_length");
			}

			ConditionSequence conditionSequence = super.makeCreateAuthorizationRequestSteps()
				.insertBefore(CreateRandomNonceValue.class, cmd);

			return conditionSequence;
		}

		return super.makeCreateAuthorizationRequestSteps();
	}
}
