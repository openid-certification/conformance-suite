package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureIdTokenDoesNotContainEmail;
import net.openid.conformance.condition.client.EnsureIdTokenDoesNotContainName;
import net.openid.conformance.condition.client.EnsureUserInfoDoesNotContainName;
import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenIdEmail;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-email
@PublishTestModule(
	testName = "oidcc-scope-email",
	displayName = "OIDCC: check email scope",
	summary = "This test requests authorization with email scope and issues a warning if an email address isn't returned in the expected place.",
	profile = "OIDCC"
)
public class OIDCCScopeEmail extends AbstractOIDCCReturnedClaimsServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		callAndStopOnFailure(SetScopeInClientConfigurationToOpenIdEmail.class);
		super.skipTestIfScopesNotSupported();
	}

	@Override
	protected void performIdTokenValidation() {
		super.performIdTokenValidation();

		// the python test did not check this as far as I know
		callAndContinueOnFailure(EnsureIdTokenDoesNotContainName.class, Condition.ConditionResult.WARNING,  "OIDCC-5.5", "OIDC-5.5.1");

		if (responseType.includesCode() || responseType.includesToken()) {
			// we have an access token so response should not be in id_token
			callAndContinueOnFailure(EnsureIdTokenDoesNotContainEmail.class, Condition.ConditionResult.WARNING, "OIDCC-5.4");
		}
	}

	@Override
	protected void validateUserInfoResponse() {
		super.validateUserInfoResponse();
		callAndContinueOnFailure(EnsureUserInfoDoesNotContainName.class, Condition.ConditionResult.WARNING,  "OIDCC-5.5", "OIDC-5.5.1");
	}
}
