package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.FAPIKSAValidateConsentScope;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

/**
 * Profile behavior for KSA (Saudi Arabia Open Finance) client (RP) tests.
 * Requires mTLS everywhere, handles the account-requests consent endpoint over mTLS,
 * and validates the authorization-request scope against the KSA consent scope.
 */
public class KsaClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public boolean claimsHttpMtlsPath(String path) {
		return AbstractFAPI2SPFinalClientTest.ACCOUNT_REQUESTS_PATH.equals(path);
	}

	@Override
	public Object handleProfileSpecificMtlsPath(String requestId, String path) {
		// Dispatch via the module method so test classes can override and fall through via super.
		if (AbstractFAPI2SPFinalClientTest.ACCOUNT_REQUESTS_PATH.equals(path)) {
			return module.ksaAccountRequestEndpoint(requestId);
		}
		return super.handleProfileSpecificMtlsPath(requestId, path);
	}

	@Override
	public ConditionSequence validateAuthorizationRequestScope() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIKSAValidateConsentScope.class);
			}
		};
	}
}
