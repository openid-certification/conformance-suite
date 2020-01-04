package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.OIDCCCheckScopesSupportedContainScopeTest;
import net.openid.conformance.condition.client.VerifyScopesReturnedInAuthorizationEndpointIdToken;

public class AbstractOIDCCScopesServerTest extends AbstractOIDCCServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		callAndContinueOnFailure(OIDCCCheckScopesSupportedContainScopeTest.class);

		Boolean scopesSupportedFlag = env.getBoolean("scopes_not_supported_flag");
		if (scopesSupportedFlag != null && scopesSupportedFlag) {
			fireTestSkipped("The discovery endpoint scopes_supported doesn't contain expected scope test; this cannot be tested");
		}
	}

	@Override
	protected void verifyScopesReturnedInIdToken() {
		callAndContinueOnFailure(VerifyScopesReturnedInAuthorizationEndpointIdToken.class, Condition.ConditionResult.WARNING);
	}

}
