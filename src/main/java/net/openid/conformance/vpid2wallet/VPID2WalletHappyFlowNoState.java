package net.openid.conformance.vpid2wallet;

import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-id2-wallet-happy-flow-no-state",
	displayName = "OID4VPID2: Happy flow test with no 'state' parameter",
	summary = "Expects the wallet to correctly process a request without a state parameter, which is an optional parameter in the OID4VP specification.",
	profile = "OID4VP-ID2",
	configurationFields = {
		"client.presentation_definition",
		"client.jwks"
	}
)

public class VPID2WalletHappyFlowNoState extends AbstractVPID2WalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence createAuthorizationRequestSteps = super.createAuthorizationRequestSequence();

		createAuthorizationRequestSteps = createAuthorizationRequestSteps.
			skip(AddStateToAuthorizationEndpointRequest.class, "Not adding state, it's optional").
			skip(CreateRandomStateValue.class, "Not adding state, it's optional");

		return createAuthorizationRequestSteps;
	}

}
