package net.openid.conformance.fapi2baselineid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.rs.FAPIBrazilRsPathConstants;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@PublishTestModule(
	testName = "fapi2-baseline-id2-client-test-pkce-code-different",
	displayName = "FAPI2-Baseline-ID2: client test to ensure that the client uses a different PKCE code verifier in each authorization flow",
	summary = "Tests two  authorization flows; the client should perform OpenID discovery from the displayed discoveryUrl, call the authorization endpoint with a code challenge (which will immediately redirect back), exchange the authorization code and code verifiere for an access token at the token endpoint and make a GET request to the resource endpoint displayed (usually the 'accounts' or 'userinfo' endpoint). The client repeats the flow again. The test will check whether different PKCE code verifiers are used.",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)

public class FAPI2BaselineID2ClientTestPkceCodeDifferent extends AbstractFAPI2BaselineID2ClientTest {
	protected int numReceivedAuthorizationRequest;
	protected int numReceivedTokenRequest;
	protected int numReceivedParRequest;
	protected int numReceivedResourceRequest;
	protected int numReceivedGetConsentRequest;
	protected int numReceivedHandleConsentRequest;
	protected int numReceivedGetPaymentsConsentRequest;
	protected int numReceivedHandlePaymentsConsentRequest;
	protected int numReceivedHandlePaymentsInitiationRequest;
	protected int numReceivedJwksRequest;
	protected int numReceivedDiscoveryRequest;


	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected Object handleClientRequestForPath(String requestId, String path) {
		if (path.equals("authorize")) {
			++numReceivedAuthorizationRequest;
			if (startingShutdown) {
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			return authorizationEndpoint(requestId);
		} else if (path.equals("token")) {
			++numReceivedTokenRequest;
			if (startingShutdown) {
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			if (profileRequiresMtlsEverywhere) {
				throw new TestFailureException(getId(), "This ecosystems requires that the token endpoint is called over an mTLS secured connection " +
					"using the token_endpoint found in mtls_endpoint_aliases.");
			} else {
				return tokenEndpoint(requestId);
			}
		} else if (path.equals("jwks")) {
			++numReceivedJwksRequest;
			return jwksEndpoint();
		} else if (path.equals("userinfo")) {
			++numReceivedResourceRequest;
			if (startingShutdown) {
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			if (fapi2SenderConstrainMethod == FAPI2SenderConstrainMethod.MTLS) {
				throw new TestFailureException(getId(), "The userinfo endpoint must be called over an mTLS secured connection.");
			}
			return userinfoEndpoint(requestId);
		} else if (path.equals(".well-known/openid-configuration")) {
			++numReceivedDiscoveryRequest;
			return discoveryEndpoint();
		} else if (path.equals("par")) {
			++numReceivedParRequest;
			if (startingShutdown) {
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			if (profileRequiresMtlsEverywhere) {
				throw new TestFailureException(getId(), "In this ecosystem, the PAR endpoint must be called over an mTLS " +
					"secured connection using the pushed_authorization_request_endpoint found in mtls_endpoint_aliases.");
			}
			if (clientAuthType == ClientAuthType.MTLS) {
				throw new TestFailureException(getId(), "The PAR endpoint must be called over an mTLS secured connection when using MTLS client authentication.");
			}
			return parEndpoint(requestId);
		} else if (path.equals(ACCOUNT_REQUESTS_PATH) && profile == FAPI2ID2OPProfile.OPENBANKING_UK) {
			++numReceivedResourceRequest;
			if (startingShutdown) {
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			return accountRequestsEndpoint(requestId);
		}
		throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
	}

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		switch (path) {
			case "token":
				++numReceivedTokenRequest;
				break;

			case ACCOUNTS_PATH:
			case FAPIBrazilRsPathConstants.BRAZIL_ACCOUNTS_PATH:
			case "userinfo":
				++numReceivedResourceRequest;
				break;

			case "par":
				++numReceivedParRequest;
				break;
		}

		if (profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
			if(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH.equals(path)) {
				++numReceivedGetConsentRequest;
			} else if(path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH + "/")) {
				++numReceivedHandleConsentRequest;
			}
			if(FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH.equals(path)) {
				++numReceivedGetPaymentsConsentRequest;
			} else if(path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH + "/")) {
				++numReceivedHandlePaymentsConsentRequest;
			}
			if(FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH.equals(path)) {
				++numReceivedHandlePaymentsInitiationRequest;
			}
		}
		return super.handleHttpMtls(path, req, res, session, requestParts);
	}


	@Override
	protected void resourceEndpointCallComplete() {
		if(checkIfAllRequestsAreReceived()) {
			fireTestFinished();
		} else {
			setStatus(Status.WAITING);
		}
	}

	protected boolean checkIfAllRequestsAreReceived() {
		boolean resourceRequestsComplete = (numReceivedResourceRequest == 2) ||
			(numReceivedHandlePaymentsInitiationRequest == 2);

		boolean allRequestsReceived =
				(numReceivedJwksRequest >= 1) &&
				(numReceivedDiscoveryRequest >= 1)&&
				(numReceivedAuthorizationRequest == 2) &&
				(numReceivedParRequest == 2) &&
				(numReceivedTokenRequest == 2) &&
				resourceRequestsComplete;

		if (profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
			boolean brazilRequestsComplete =
				(
					(
						(numReceivedGetConsentRequest >= 1) &&
						(numReceivedHandleConsentRequest >= 1)
					)
						||
					(
						(numReceivedGetPaymentsConsentRequest >= 1) &&
						(numReceivedHandlePaymentsConsentRequest >= 1)
					)
				);
			allRequestsReceived= allRequestsReceived && brazilRequestsComplete;
		}
		return allRequestsReceived;
	}

}
