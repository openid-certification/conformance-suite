package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-happy-flow",
	displayName = "OID4VP-1.0-FINAL: Happy flow",
	summary = "Baseline happy flow test. The optional 'state' authorization request parameter is omitted (note: 'state' is not used by DC API response modes regardless).",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"client.jwks",
		"client.dcql",
		"client.verifier_info"
	}
)
public class VP1FinalWalletHappyFlow extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence createAuthorizationRequestSteps = super.createAuthorizationRequestSequence();

		createAuthorizationRequestSteps = createAuthorizationRequestSteps.
			skip(AddStateToAuthorizationEndpointRequest.class, "Not adding state, it's optional").
			skip(CreateRandomStateValue.class, "Not adding state, it's optional");

		return createAuthorizationRequestSteps;
	}

}
