package net.openid.conformance.vpverifier;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.CheckForInvalidCharsInNonce;
import net.openid.conformance.condition.as.CheckNonceLength;
import net.openid.conformance.testmodule.PublishTestModule;

/**
 * the default happy path test
 */
@PublishTestModule(
	testName = "oid4vp-verifier-happy-flow",
	displayName = "OID4VP Verifier: Happy flow test ",
	summary = "TBC",
	profile = "OID4VP-ID2",
	configurationFields = {
		"client.client_id",
		"client.redirect_uri"
	}
)
public class VPID2VerifierHappyFlow extends AbstractVPID2VerifierTest {

	@Override
	protected void extractNonceFromAuthorizationEndpointRequestParameters() {
		super.extractNonceFromAuthorizationEndpointRequestParameters();

		skipIfMissing(null, new String[] {"nonce"}, ConditionResult.INFO,
			CheckForInvalidCharsInNonce.class, ConditionResult.WARNING);
		skipIfMissing(null, new String[] {"nonce"}, ConditionResult.INFO,
			CheckNonceLength.class, ConditionResult.WARNING);
	}
}
