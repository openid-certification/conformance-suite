package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.CreateRefreshToken;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.as.EnsureScopeInRefreshRequestMatchesOriginallyGranted;
import net.openid.conformance.condition.as.ExtractScopeFromTokenEndpointRequest;
import net.openid.conformance.condition.as.OIDCCGenerateServerConfiguration;
import net.openid.conformance.condition.as.OIDCCGenerateServerConfigurationWithRefreshTokenGrantType;
import net.openid.conformance.condition.as.ValidateRefreshToken;
import net.openid.conformance.testmodule.TestFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class AbstractOIDCCClientTestRefreshToken extends AbstractOIDCCClientTest {

	@Override
	protected void configureServerConfiguration() {
		callAndStopOnFailure(OIDCCGenerateServerConfigurationWithRefreshTokenGrantType.class);
	}

	@Override
	protected Object handleTokenEndpointRequest(String requestId) {
		//if we don't remove ExtractClientCredentialsFromBasicAuthorizationHeader throws "Found existing client authentication"
		env.removeObject("client_authentication");
		return super.handleTokenEndpointRequest(requestId);
	}

	/**
	 * Override in subclasses to change behavior
	 * @return
	 */
	protected boolean generateIdTokenOnRefreshRequest() {
		return true;
	}

	/**
	 * Override in subclasses to change behavior
	 * @return
	 */
	protected boolean generateRefreshTokenOnRefreshRequest() {
		return true;
	}

	protected void addCustomValuesToIdTokenForRefreshResponse() {

	}

	protected void customizeIdTokenSignatureForRefreshResponse() {

	}

	/**
	 * return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);
	 * @param requestId
	 * @return
	 */
	@Override
	protected Object refreshTokenGrantType(String requestId) {

		validateRefreshRequest();

		//this must be after EnsureScopeInRefreshRequestMatchesOriginallyGranted is called
		callAndStopOnFailure(ExtractScopeFromTokenEndpointRequest.class);

		generateAccessToken();

		if(generateIdTokenOnRefreshRequest()) {
			createIdTokenForRefreshRequest();
		}

		createRefreshToken(true);

		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		call(exec().unmapKey("token_endpoint_request").endBlock());

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);
	}

	/**
	 * Generate id_token to be used in refresh responses
	 */
	protected void createIdTokenForRefreshRequest() {
		generateIdTokenClaims();

		addAtHashToIdToken();

		addCustomValuesToIdTokenForRefreshResponse();

		signIdToken();

		customizeIdTokenSignatureForRefreshResponse();

		encryptIdTokenIfNecessary();
	}

	/**
	 *
	 * @param isRefreshTokenGrant false if called for an authorization grant, true for refresh token
	 */
	@Override
	protected void createRefreshToken(boolean isRefreshTokenGrant) {
		if(!isRefreshTokenGrant) {
			callAndStopOnFailure(CreateRefreshToken.class, "RFC6749-1.5");
		} else {
			if(generateRefreshTokenOnRefreshRequest()) {
				callAndStopOnFailure(CreateRefreshToken.class, "RFC6749-1.5");
			}
		}
	}

	protected void validateRefreshRequest() {
		//EnsureScopeInRefreshRequestMatchesOriginallyGranted must be used before ExtractScopeFromTokenEndpointRequest
		callAndStopOnFailure(EnsureScopeInRefreshRequestMatchesOriginallyGranted.class, "RFC6749-6");
		callAndStopOnFailure(ValidateRefreshToken.class, "RFC6749-6");
	}
}
