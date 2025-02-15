package net.openid.conformance.vpid3wallet;

import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-id3-wallet-happy-flow-no-state",
	displayName = "OID4VPID3+draft24: Happy flow test with no 'state' parameter",
	summary = "Expects the wallet to correctly process a request without a state parameter, which is an optional parameter in the OID4VP specification.",
	profile = "OID4VP-ID3",
	configurationFields = {
		"client.jwks"
	}
)

// Because state & redirect uri aren't used in browser API this test doesn't do anything substantially different
// to the other happy flow test. Though the names / behaviour / description etc could be improved.
@VariantNotApplicable(parameter = VPID3WalletResponseMode.class, values={"dc_api", "dc_api.jwt"})
public class VPID3WalletHappyFlowNoState extends AbstractVPID3WalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence createAuthorizationRequestSteps = super.createAuthorizationRequestSequence();

		createAuthorizationRequestSteps = createAuthorizationRequestSteps.
			skip(AddStateToAuthorizationEndpointRequest.class, "Not adding state, it's optional").
			skip(CreateRandomStateValue.class, "Not adding state, it's optional");

		return createAuthorizationRequestSteps;
	}

}
