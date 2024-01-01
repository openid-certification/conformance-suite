package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.as.LogAccessTokenAlwaysRejectedToForceARefreshGrant;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-refresh-token-test",
	displayName = "FAPI-CIBA-ID1: client refresh token test",
	summary = "Tests a refresh token flow; " +
	"The client should perform OpenID discovery from the displayed " +
	"discoveryUrl, call the backchannel endpoint and then retrieve an " +
	"access token at the token endpoint and make a request to the " +
	"payments endpoint displayed. This call will always return a 401 " +
	"error, the client must call the token endpoint again using " +
	"refresh_token grant type twice (the first call will return a new " +
	"refresh token) to obtain a new access token and call the payments " +
	"endpoint again with the new access token obtained using the " +
	"refresh_token.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.backchannel_client_notification_endpoint",
		"client.certificate",
		"client.jwks"
	}
)
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = {"plain_fapi", "consumerdataright_au", "openbanking_uk", "openinsurance_brazil", "openbanking_ksa"})
public class FAPICIBAClientRefreshTokenTest extends AbstractFAPICIBAClientTest {

	private int numberOfTimesRefreshTokenUsed = 0;

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected Object refreshTokenGrantType() {
		Object superResult = super.refreshTokenGrantType();
		numberOfTimesRefreshTokenUsed += 1;
		return superResult;
	}

	@Override
	protected Object accountsEndpoint(String requestId) {
		if(numberOfTimesRefreshTokenUsed < 2) {
			return rejectAccessToken("Accounts endpoint (always rejected)");
		}
		return super.accountsEndpoint(requestId);
	}

	@Override
	protected Object brazilHandleNewPaymentInitiationRequest(String requestId) {
		if(numberOfTimesRefreshTokenUsed < 2) {
			return rejectAccessToken("Payment initiation endpoint (always rejected)");
		}
		return super.brazilHandleNewPaymentInitiationRequest(requestId);
	}

	protected ResponseEntity<Object> rejectAccessToken(String blockLabel) {
		setStatus(Status.RUNNING);
		call(exec().startBlock(blockLabel));

		callAndStopOnFailure(LogAccessTokenAlwaysRejectedToForceARefreshGrant.class);
		JsonObject wwwAuthHeader = new JsonObject();
		wwwAuthHeader.addProperty("WWW-Authenticate",
			"Bearer realm=\"conformancesuite\", " +
				"error=\"invalid_token\", " +
				"error_description=\"Invalid access token. This test requires you to obtain a new access token twice using the refresh_token\"");

		setStatus(Status.WAITING);
		return new ResponseEntity<>(headersFromJson(wwwAuthHeader), HttpStatus.UNAUTHORIZED);
	}

}
