package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsureIncomingRequestBodyIsEmpty;
import net.openid.conformance.condition.client.EnsureIncomingUrlQueryIsEmpty;
import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest;
import net.openid.conformance.condition.rs.CreateResourceServerDpopNonce;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsGet;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Shared base for FAPI2-SP-Final client (RP) grant management test modules.
 *
 * <p>Grant management certification is only meaningful for generic FAPI, where it is an opt-in
 * capability, and for Chile, whose profile requires it. Every other profile - including the VCI ones,
 * and the client credentials grant, which has no authorization flow to produce a grant at all - must
 * not generate these modules. The test plans also exclude those profiles at the plan level, but that
 * guard only fires when a plan is instantiated through the UI/plan API; a module invoked directly with
 * a config (as the integration tests do) is only protected by the module's own
 * {@code @VariantNotApplicable}. This mirrors {@link AbstractFAPI2SPFinalGrantManagementTestModule} on
 * the server (OP) side, including the reason the static profile exclusion is needed alongside the
 * conditional one on {@link AbstractFAPI2SPFinalClientTest}.
 *
 * <p>This base also emulates the grant management API (GM 6) that the suite advertises in the discovery
 * document it serves, so an RP can query and revoke the grant it was issued. Only the modules that ask
 * the tester to call it end on those calls; the others simply serve the endpoint if the client goes
 * looking, rather than returning a bare 404.
 */
@VariantNotApplicable(parameter = GrantManagement.class, values = {"disabled"})
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {"openbanking_uk",
	"consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "ksa",
	"fapi_client_credentials_grant", "vci", "vci_haip"})
public abstract class AbstractFAPI2SPFinalClientTestGrantManagement extends AbstractFAPI2SPFinalClientTest {

	/** Matches the path AddGrantManagementToServerConfiguration advertises, plus the grant_id segment. */
	public static final String GRANTS_PATH_PREFIX = "grants/";

	protected boolean grantQueried = false;

	protected boolean grantRevoked = false;

	/** Guards {@link #finishGrantManagementTest()} against ending the test more than once. */
	private boolean grantManagementTestFinished = false;

	/**
	 * Verifies that the client under test really did make a grant management request. Without this the
	 * grant management client tests would be indistinguishable from the ordinary ones: the suite drives
	 * both the responses and the checks, so a client that never sent grant_management_action would
	 * otherwise pass.
	 */
	@Override
	protected void extractParEndpointRequest() {
		super.extractParEndpointRequest();

		callAndContinueOnFailure(GrantManagementSupport.EnsurePARRequestContainsGrantManagementAction.class, ConditionResult.FAILURE, "GM-5.2");
		checkGrantIdInParEndpointRequest();
	}

	/**
	 * Checks the grant_id the client sent (or did not send) for the action this test asks the client to use.
	 * The default expects the create action, where GM 5.2 says grant_id MUST NOT be present.
	 */
	protected void checkGrantIdInParEndpointRequest() {
		callAndContinueOnFailure(GrantManagementSupport.EnsurePARRequestDoesNotContainGrantIdWithCreateAction.class, ConditionResult.FAILURE, "GM-5.2");
	}

	@Override
	protected Object handleClientRequestForPath(String requestId, String path) {
		if (path.startsWith(GRANTS_PATH_PREFIX)) {
			if (isMTLSConstrain() || profileRequiresMtlsEverywhere) {
				throw new TestFailureException(getId(), "The grant management endpoint must be called over an mTLS secured connection, "
					+ "using the grant_management_endpoint found in mtls_endpoint_aliases.");
			}
			return grantManagementEndpoint(requestId, path);
		}
		return super.handleClientRequestForPath(requestId, path);
	}

	@Override
	protected Object handleClientRequestForMtlsPath(String requestId, String path) {
		if (path.startsWith(GRANTS_PATH_PREFIX)) {
			if (!isMTLSConstrain() && !profileRequiresMtlsEverywhere) {
				throw new TestFailureException(getId(), "The grant management endpoint must not be called over an mTLS secured connection.");
			}
			return grantManagementEndpoint(requestId, path);
		}
		return super.handleClientRequestForMtlsPath(requestId, path);
	}

	/**
	 * The emulated grant management API: GET returns the grant (GM 6.4), DELETE revokes it (GM 6.5), and
	 * a query of a revoked grant returns 404 (GM 6.6), as a real authorization server would.
	 */
	protected Object grantManagementEndpoint(String requestId, String path) {
		if (startingShutdown) {
			throw new TestFailureException(getId(), "Client has incorrectly called the grant management endpoint "
				+ "after receiving a response that must cause it to stop interacting with the server");
		}

		setStatus(Status.RUNNING);

		call(exec().startBlock("Grant management endpoint").mapKey("incoming_request", requestId));

		env.putString(GrantManagementSupport.EnsureGrantManagementRequestIsForIssuedGrant.REQUESTED_GRANT_ID,
			path.substring(GRANTS_PATH_PREFIX.length()));

		if (isMTLSConstrain() || profileRequiresMtlsEverywhere) {
			call(exec().mapKey("token_endpoint_request", requestId));
			checkMtlsCertificate();
			call(exec().unmapKey("token_endpoint_request"));
		}

		// the grant management API is a protected resource (GM 6.1), so the access token has to be
		// presented and sender constrained exactly as at any other resource endpoint
		checkResourceEndpointRequest(false);

		// under DPoP the suite may have demanded a nonce; if the client did not use it, answer with the
		// prepared 401 challenge rather than treating the call as successful
		ResponseEntity<?> dpopNonceError = handlePendingDpopNonceErrorResponse();
		if (dpopNonceError != null) {
			call(exec().unmapKey("incoming_request").endBlock());
			setStatus(Status.WAITING);
			return dpopNonceError;
		}

		callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementRequestIsForIssuedGrant.class, ConditionResult.FAILURE, "GM-6.3");
		// neither the query nor the revoke request carries a body or any query parameters in GM 6.4/6.5
		callAndContinueOnFailure(EnsureIncomingRequestBodyIsEmpty.class, ConditionResult.FAILURE, "GM-6.4", "GM-6.5");
		callAndContinueOnFailure(EnsureIncomingUrlQueryIsEmpty.class, ConditionResult.FAILURE, "GM-6.4", "GM-6.5");

		String method = env.getString("incoming_request", "method");
		ResponseEntity<Object> response;

		if ("DELETE".equalsIgnoreCase(method)) {
			grantRevoked = true;
			response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} else {
			callAndContinueOnFailure(EnsureIncomingRequestMethodIsGet.class, ConditionResult.FAILURE, "GM-6.4", "GM-6.5");

			if (grantRevoked) {
				// GM 6.6: the grant resource URL is unknown once the grant has been revoked
				response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
			} else {
				grantQueried = true;
				callAndStopOnFailure(GrantManagementSupport.CreateGrantManagementQueryResponse.class, "GM-6.4");
				response = new ResponseEntity<>(
					env.getObject(GrantManagementSupport.CreateGrantManagementQueryResponse.RESPONSE_KEY),
					headersFromJson(env.getObject(GrantManagementSupport.CreateGrantManagementQueryResponse.RESPONSE_HEADERS_KEY)),
					HttpStatus.OK);
			}
		}

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		if (requireResourceServerEndpointDpopNonce()) {
			callAndContinueOnFailure(CreateResourceServerDpopNonce.class, ConditionResult.INFO);
		}

		call(exec().unmapKey("incoming_request").endBlock());

		onGrantManagementRequestComplete();

		// the hook may have ended the test, which already moves the status to WAITING; setting it again
		// would trip the "status is the same" guard in AbstractTestModule and fail the test spuriously
		if (getStatus() == Status.RUNNING) {
			setStatus(Status.WAITING);
		}

		return response;
	}

	/**
	 * Hook for the modules that end once the client has finished using the grant management API. The
	 * default does nothing, so a client that queries or revokes during another test does not end it early.
	 *
	 * <p>An implementation that finishes the test must do so through {@link #finishGrantManagementTest()},
	 * so that a later call (for instance a query of an already revoked grant) does not finish it twice.
	 */
	protected void onGrantManagementRequestComplete() {
	}

	/**
	 * Ends the test from {@link #onGrantManagementRequestComplete()}, at most once. A client is free to
	 * keep calling the endpoint after the call this test was waiting for - a query after the revocation
	 * answers 404 (GM 6.6) - and firing the finish a second time would be an illegal status transition.
	 */
	protected void finishGrantManagementTest() {
		if (grantManagementTestFinished) {
			return;
		}
		grantManagementTestFinished = true;
		fireTestFinished();
	}
}
