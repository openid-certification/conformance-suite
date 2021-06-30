package net.openid.conformance.fapi1advancedfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.as.LogAccessTokenAlwaysRejectedToForceARefreshGrant;
import net.openid.conformance.condition.as.RemoveIssuedAccessTokenFromEnvironment;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-refresh-token-test",
	displayName = "FAPI1-Advanced-Final: client refresh token test",
	summary = "Tests a refresh token flow; " +
		"the client should perform OpenID discovery from the displayed discoveryUrl, " +
		"call the authorization endpoint (which will immediately redirect back), " +
		"exchange the authorization code for an access token at the token endpoint " +
		"and make a GET request to the accounts endpoint displayed." +
		" This call will always return a 401 error," +
		" the client must call the token endpoint again using refresh_token grant type to obtain a new access token" +
		" and call the accounts endpoint again with the new access token obtained using the refresh_token.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = {"plain_fapi", "consumerdataright_au", "openbanking_uk"})
public class FAPI1AdvancedFinalClientRefreshTokenTest extends AbstractFAPI1AdvancedFinalClientTest {
	private boolean issuedAccessTokenViaRefreshTokenGrant = false;

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
		issuedAccessTokenViaRefreshTokenGrant = true;
		return superResult;
	}

	@Override
	protected Object accountsEndpoint(String requestId) {
		if(!issuedAccessTokenViaRefreshTokenGrant) {
			setStatus(Status.RUNNING);

			call(exec().startBlock("Accounts endpoint (always rejected)"));
			callAndStopOnFailure(LogAccessTokenAlwaysRejectedToForceARefreshGrant.class);
			JsonObject wwwAuthHeader = new JsonObject();
			wwwAuthHeader.addProperty("WWW-Authenticate",
				"Bearer realm=\"conformancesuite\", " +
					"error=\"invalid_token\", " +
					"error_description=\"Invalid access token. This test requires you to obtain a new access token using the refresh_token\"");
			setStatus(Status.WAITING);
			return new ResponseEntity<>(headersFromJson(wwwAuthHeader), HttpStatus.UNAUTHORIZED);
		}
		return super.accountsEndpoint(requestId);
	}
}
