package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddIdTokenEssentialNameClaimToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddUserInfoEssentialNameClaimToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.EnsureIdTokenContainsName;
import net.openid.conformance.condition.client.EnsureIdTokenDoesNotContainName;
import net.openid.conformance.condition.client.EnsureUserInfoContainsName;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_claims_essential
@PublishTestModule(
	testName = "oidcc-claims-essential",
	displayName = "OIDCC: claims essential",
	summary = "This test makes an authorization request requesting the 'name' claim as essential (in the userinfo, except for response_type=id_token where it is requested in the id_token), and the OP must return a successful result. A warning is raised if the OP fails to return a value for the name claim.",
	profile = "OIDCC"
)
public class OIDCCClaimsEssential extends AbstractOIDCCReturnedClaimsServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
	}

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		Class<? extends Condition> addClaim = AddUserInfoEssentialNameClaimToAuthorizationEndpointRequest.class;

		if (responseType.isIdToken()) {
			addClaim = AddIdTokenEssentialNameClaimToAuthorizationEndpointRequest.class;
		}

		return super.createAuthorizationRequestSequence()
			.then(condition(addClaim).requirements("OIDCC-5.5", "OIDCC-5.5.1"));
	}

	@Override
	protected void validateUserInfoResponse() {
		super.validateUserInfoResponse();
		callAndContinueOnFailure(EnsureUserInfoContainsName.class, Condition.ConditionResult.WARNING, "OIDCC-5.5", "OIDCC-5.5.1");

		// the python test did not check this as far as I know
		callAndContinueOnFailure(EnsureIdTokenDoesNotContainName.class, Condition.ConditionResult.WARNING,  "OIDCC-5.5", "OIDCC-5.5.1");
	}

	@Override
	protected void validateIdTokenForResponseTypeIdToken() {
		super.validateIdTokenForResponseTypeIdToken();
		callAndContinueOnFailure(EnsureIdTokenContainsName.class, Condition.ConditionResult.WARNING,  "OIDCC-5.5", "OIDCC-5.5.1");
	}
}
