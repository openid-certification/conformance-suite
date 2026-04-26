package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.AddOpenBankingUkClaimsToAuthorizationCodeGrant;
import net.openid.conformance.sequence.as.AddOpenBankingUkClaimsToAuthorizationEndpointResponse;
import net.openid.conformance.sequence.as.GenerateOpenBankingUkAccountsEndpointResponse;

/**
 * Profile behavior for OpenBanking UK client tests.
 * Requires mTLS everywhere; exposes the UK-specific account-requests endpoint;
 * adds OB-UK claims to id_tokens and accounts response payload.
 */
public class OpenBankingUkClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public Class<? extends ConditionSequence> getAuthorizationCodeGrantTypeProfileSteps() {
		return AddOpenBankingUkClaimsToAuthorizationCodeGrant.class;
	}

	@Override
	public Class<? extends ConditionSequence> getAuthorizationEndpointProfileSteps() {
		return AddOpenBankingUkClaimsToAuthorizationEndpointResponse.class;
	}

	@Override
	public Class<? extends ConditionSequence> getAccountsEndpointProfileSteps() {
		return GenerateOpenBankingUkAccountsEndpointResponse.class;
	}

	@Override
	public void exposeProfileEndpoints() {
		module.exposeMtlsPath("accounts_endpoint", AbstractFAPI2SPFinalClientTest.ACCOUNTS_PATH);
		module.exposePath("account_requests_endpoint", AbstractFAPI2SPFinalClientTest.ACCOUNT_REQUESTS_PATH);
	}

	@Override
	public boolean claimsHttpPath(String path) {
		return AbstractFAPI2SPFinalClientTest.ACCOUNT_REQUESTS_PATH.equals(path);
	}

	@Override
	public Object handleProfileSpecificPath(String requestId, String path) {
		return module.accountRequestsEndpoint(requestId);
	}

	@Override
	public boolean supportsClientCredentialsGrant() {
		return true;
	}
}
