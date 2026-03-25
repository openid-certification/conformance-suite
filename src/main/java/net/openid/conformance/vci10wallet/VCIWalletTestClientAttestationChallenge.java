package net.openid.conformance.vci10wallet;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vci-1_0-wallet-test-client-attestation-challenge",
	displayName = "OID4VCI 1.0: Wallet Test Client Attestation Challenge",
	summary = """
		This test case validates that the wallet correctly supports the client attestation challenge endpoint \
		as defined in draft-ietf-oauth-attestation-based-client-auth-07 Section 8.

		The emulated Authorization Server exposes a challenge_endpoint in its metadata. \
		The wallet is expected to fetch a challenge from this endpoint and include it as the challenge claim \
		in the OAuth-Client-Attestation-PoP JWT.

		The test validates that the challenge claim in the PoP JWT matches the issued challenge. \
		If the wallet does not fetch or use the challenge, the test will fail.
		""",
	profile = "OID4VCI-1_0"
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post",
	"client_secret_jwt", "private_key_jwt", "mtls"
})
public class VCIWalletTestClientAttestationChallenge extends AbstractVCIWalletTest {

	@Override
	protected boolean isChallengeEndpointSupported() {
		return true;
	}
}
