package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckIdTokenAuthTimeClaimsSameIfPresent;
import net.openid.conformance.condition.client.CheckIdTokenSubConsistentForSecondAuthorization;

/**
 * Calls the authorization endpoint twice, with subclass specified changes the second time, and expects the id_tokens
 * between the two calls to have the same auth_time (if present) and sub.
 */
public abstract class AbstractOIDCCSameAuthTwiceServerTest extends AbstractOIDCCServerTest {
	private boolean firstTime = true;

	@Override
	protected String currentClientString() {
		return firstTime ? "" : "Second authorization: ";
	}

	@Override
	protected void createAuthorizationRequest() {
		if (firstTime) {
			// capture id_token from first authentication for later comparison (we don't care if it's from
			// the authorization endpoint or the token endpoint)
			env.mapKey("id_token", "first_id_token");
			createFirstAuthorizationRequest();
		} else {
			env.unmapKey("id_token");
			createSecondAuthorizationRequest();
		}
	}

	protected void createFirstAuthorizationRequest() {
		super.createAuthorizationRequest();
	}

	protected abstract void createSecondAuthorizationRequest();

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (firstTime) {
			firstTime = false;
			validateFirstIdToken();
			// do the process again, but this time calling createSecondAuthorizationRequest()
			performAuthorizationFlow();
		} else {
			validateFirstAndSecondIdTokens();

			fireTestFinished();
		}
	}

	protected void validateFirstIdToken() {
		// no extra checks necessary beyond the generic ones in the superclass
	}

	protected void validateFirstAndSecondIdTokens() {
		// these two checks are equivalent to same-authn, https://github.com/rohe/oidctest/blob/a306ff8ccd02da456192b595cf48ab5dcfd3d15a/src/oidctest/op/check.py#L1117

		// this check only works if the server actually returns auth_time; it might be better to explicitly request auth_time
		// in the first authorization but this matches what the original python tests did.
		// we could also time how long the second authorization endpoint call takes; it should really only take seconds as it
		// should just redirect straight back instantly.
		callAndContinueOnFailure(CheckIdTokenAuthTimeClaimsSameIfPresent.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
		callAndContinueOnFailure(CheckIdTokenSubConsistentForSecondAuthorization.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
	}

	@Override
	public void cleanup() {
		firstTime = true; // to avoid any blocks created in cleanup being prefixed in currentClientString()
		super.cleanup();
	}
}
