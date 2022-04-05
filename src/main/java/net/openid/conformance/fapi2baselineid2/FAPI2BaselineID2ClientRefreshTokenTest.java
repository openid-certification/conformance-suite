package net.openid.conformance.fapi2baselineid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.as.LogAccessTokenAlwaysRejectedToForceARefreshGrant;
import net.openid.conformance.condition.as.RemoveIssuedAccessTokenFromEnvironment;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi2-baseline-id2-client-refresh-token-test",
	displayName = "FAPI2-Baseline-ID2: client refresh token test",
	summary = "Tests a refresh token flow; " +
		"the client should perform OpenID discovery from the displayed discoveryUrl, " +
		"call the authorization endpoint (which will immediately redirect back), " +
		"exchange the authorization code for an access token at the token endpoint " +
		"and make a GET request to the accounts endpoint displayed." +
		" This call will always return a 401 error," +
		" the client must call the token endpoint again using refresh_token grant type twice (the" +
		" first call will return a new refresh token) to obtain a new access token" +
		" and call the accounts endpoint again with the new access token obtained using the refresh_token.",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"directory.keystore"
	}
)
@VariantNotApplicable(parameter = FAPI2ID2OPProfile.class, values = {"plain_fapi", "consumerdataright_au", "openbanking_uk", "idmvp"})
public class FAPI2BaselineID2ClientRefreshTokenTest extends AbstractFAPI2BaselineID2ClientTest {
	private int numberOfTimesRefreshTokenUsed = 0;

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {
		Object superResult = super.authorizationCodeGrantType(requestId);
		setStatus(Status.RUNNING);
		//we remove the access token as we don't want it to be used
		callAndStopOnFailure(RemoveIssuedAccessTokenFromEnvironment.class);
		setStatus(Status.WAITING);
		return superResult;
	}

	@Override
	protected Object refreshTokenGrantType(String requestId) {
		Object superResult = super.refreshTokenGrantType(requestId);
		numberOfTimesRefreshTokenUsed += 1;
		return superResult;
	}

	@Override
	protected Object accountsEndpoint(String requestId) {
		if(numberOfTimesRefreshTokenUsed < 2) {
			setStatus(Status.RUNNING);

			call(exec().startBlock("Accounts endpoint (always rejected)"));
			callAndStopOnFailure(LogAccessTokenAlwaysRejectedToForceARefreshGrant.class);
			JsonObject wwwAuthHeader = new JsonObject();
			wwwAuthHeader.addProperty("WWW-Authenticate",
				"Bearer realm=\"conformancesuite\", " +
					"error=\"invalid_token\", " +
					"error_description=\"Invalid access token. This test requires you to obtain a new access token twice using the refresh_token\"");
			setStatus(Status.WAITING);
			return new ResponseEntity<>(headersFromJson(wwwAuthHeader), HttpStatus.UNAUTHORIZED);
		}
		return super.accountsEndpoint(requestId);
	}

	@Override
	protected Object brazilHandleNewPaymentInitiationRequest(String requestId) {
		if(numberOfTimesRefreshTokenUsed < 2) {
			setStatus(Status.RUNNING);

			call(exec().startBlock("Payment initiation endpoint (always rejected)"));
			callAndStopOnFailure(LogAccessTokenAlwaysRejectedToForceARefreshGrant.class);
			JsonObject wwwAuthHeader = new JsonObject();
			wwwAuthHeader.addProperty("WWW-Authenticate",
				"Bearer realm=\"conformancesuite\", " +
					"error=\"invalid_token\", " +
					"error_description=\"Invalid access token. This test requires you to obtain a new access token twice using the refresh_token\"");
			setStatus(Status.WAITING);
			return new ResponseEntity<>(headersFromJson(wwwAuthHeader), HttpStatus.UNAUTHORIZED);
		}
		return super.brazilHandleNewPaymentInitiationRequest(requestId);
	}
}
