package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.FAPIKSAValidateConsentScope;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.TestFailureException;

/**
 * Profile behavior for KSA (Saudi Arabia Open Finance) client (RP) tests.
 * Requires mTLS everywhere, handles the account-request consent endpoint over mTLS,
 * and validates the authorization-request scope against the KSA consent scope.
 */
public class KsaClientProfileBehavior extends FAPI2ClientProfileBehavior {

	/**
	 * KSA's consent (account-request) endpoint path. Note this is singular
	 * ({@code account-request}), unlike the UK {@code account-requests} (plural) path that
	 * {@link AbstractFAPI2SPFinalClientTest#ACCOUNT_REQUESTS_PATH} carries.
	 */
	public static final String KSA_ACCOUNT_REQUEST_PATH = "open-banking/v1.1/account-request";

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public void exposeProfileEndpoints() {
		module.exposeMtlsPath("accounts_endpoint", AbstractFAPI2SPFinalClientTest.ACCOUNTS_PATH);
		module.exposeMtlsPath("account_requests_endpoint", KSA_ACCOUNT_REQUEST_PATH);
	}

	@Override
	public boolean claimsHttpPath(String path) {
		// Claim the consent endpoint on the plain (non-mtls) host too, so a call that omits
		// mTLS gets a clear "must use mTLS" failure instead of a generic "unexpected HTTP call".
		return KSA_ACCOUNT_REQUEST_PATH.equals(path);
	}

	@Override
	public Object handleProfileSpecificPath(String requestId, String path) {
		if (KSA_ACCOUNT_REQUEST_PATH.equals(path)) {
			throw new TestFailureException(module.getId(),
				"The account-request (consent) endpoint must be called over an mTLS secured connection " +
				"using the account_requests_endpoint URL from the 'Exported Values' section of the test log " +
				"(the mtls_endpoint_aliases host).");
		}
		return super.handleProfileSpecificPath(requestId, path);
	}

	@Override
	public boolean claimsHttpMtlsPath(String path) {
		return KSA_ACCOUNT_REQUEST_PATH.equals(path);
	}

	@Override
	public Object handleProfileSpecificMtlsPath(String requestId, String path) {
		// Dispatch via the module method so test classes can override and fall through via super.
		if (KSA_ACCOUNT_REQUEST_PATH.equals(path)) {
			return module.ksaAccountRequestEndpoint(requestId);
		}
		return super.handleProfileSpecificMtlsPath(requestId, path);
	}

	@Override
	public boolean supportsClientCredentialsGrant() {
		// KSA RP first calls the token endpoint with client_credentials to obtain an access
		// token used to create the consent (account-requests), then uses authorization_code.
		return true;
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
