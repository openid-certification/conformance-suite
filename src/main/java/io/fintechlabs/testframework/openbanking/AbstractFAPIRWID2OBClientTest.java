package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.as.AddACRClaimToIdTokenClaims;
import io.fintechlabs.testframework.condition.as.AddOBIntentIdToIdTokenClaims;
import io.fintechlabs.testframework.condition.as.ExtractOBIntentId;
import io.fintechlabs.testframework.condition.rs.CreateOpenBankingAccountsResponse;
import io.fintechlabs.testframework.condition.rs.GenerateOpenBankingAccountId;
import io.fintechlabs.testframework.fapi.AbstractFAPIRWID2ClientTest;


public abstract class AbstractFAPIRWID2OBClientTest extends AbstractFAPIRWID2ClientTest {

	@Override
	protected void authorizationCodeGrantTypeProfile() {

		callAndStopOnFailure(AddOBIntentIdToIdTokenClaims.class, "OB-5.2.2.8");

		callAndStopOnFailure(AddACRClaimToIdTokenClaims.class, "OB-5.2.2.8", "OIDCC-3.1.3.7-12");

	}

	@Override
	protected void authorizationEndpointProfile() {

		callAndStopOnFailure(ExtractOBIntentId.class, "OB-5.2.2.8");

		callAndStopOnFailure(AddOBIntentIdToIdTokenClaims.class, "OB-5.2.2.8");
	}

	@Override
	protected Object handleTokenEndpointGrantType(String requestId) {

		// dispatch based on grant type
		String grantType = env.getString("token_endpoint_request", "params.grant_type");

		if (grantType.equals("client_credentials")) {
			// we're doing the client credentials grant for initial token access
			return clientCredentialsGrantType(requestId);
		} else {
			return super.handleTokenEndpointGrantType(requestId);
		}

	}

	@Override
	protected Object handleClientRequestForPath(String requestId, String path) {

		if (path.equals(ACCOUNT_REQUESTS_PATH)) {
			return accountRequestsEndpoint(requestId);
		} else {
			return super.handleClientRequestForPath(requestId, path);
		}

	}

	@Override
	protected void accountsEndpointProfile(){

		callAndStopOnFailure(GenerateOpenBankingAccountId.class);
		exposeEnvString("account_id");

		callAndStopOnFailure(CreateOpenBankingAccountsResponse.class);

	}

}
