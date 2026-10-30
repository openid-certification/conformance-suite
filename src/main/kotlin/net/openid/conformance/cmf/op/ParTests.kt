package net.openid.conformance.cmf.op

import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.client.*
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.testmodule.TestModule

@PublishTestModule(
	testName = "cmf-chile-op-par-without-duplicate-parameters",
	displayName = "Chile CMF OP: PAR request without duplicate parameters",
	summary = "This test makes a PAR request and calls the authorization endpoint passing only the client_id and request_uri parameters - which must succeed, as per section 5 of https://datatracker.ietf.org/doc/html/rfc9101 - this intent was confirmed by the FAPI working group in https://bitbucket.org/openid/fapi/issues/315/par-certification-question-must-servers",
	profile = CMF_OP_PROFILE
)
class CMFChileOPParWithoutDuplicateParameters : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		modifiedLeg {
			replace(
				BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint::class.java,
				stopOnFailure().requirements("PAR-4")
					.condition(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates::class.java)
			)
		}
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-par-ensure-reused-request-uri-prior-to-auth-completion-succeeds",
	displayName = "Chile CMF OP: PAR - ensure reused request uri prior to auth completion succeeds",
	summary = "This test checks that authorization servers that enforce one-time use of `request_uri` values do so at the point of authorization, not at the point of visiting the authorization endpoint. This is achieved by visiting the authorization endpoint twice with the same 'request_uri' value. On the first visit no login should be attempted. On the second visit the login should be attempted and is expected to succeed. On error a screenshot showing the resulting error needs to be uploaded  This test is as per FAPI 2.0 Security Profile 5.3.2.2 Note 3. This is a recommendation and as such any failure of this test will result in a warning.\n\nYou may need to clear any cookies for the authorization server before running this test, to remove any existing login session and hence ensure the initial visit to the login page does not automatically authenticate the user.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPPAREnsureServerAcceptsReusedRequestUriBeforeAuthenticationCompletion : AbstractCMFChileOPTestModule() {

	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = flow.beforeRedirect(),
				// one-time use enforced at the authorization endpoint is only against a recommendation, so it warns
				afterCallback = flow.afterCallback(
					onError = Steps.of(
						"Request uri rejected",
						continueOnFailure().onFail(ConditionResult.WARNING).requirements("FAPI2-SP-FINAL-5.3.2.2")
							.condition(WarningAboutRequestUriError::class.java),
						authorizationErrorResponseSteps(
							continueOnFailure().requirements("OIDCC-3.3.2.6").condition(EnsureInvalidRequestUriError::class.java)
						)
					)
				),
				placeholder = expectErrorPage(condition(ExpectLoginPage::class.java), "login_page_placeholder")
			)
		)
	}

	/** The first visit, on which the user takes no action, then the second, on which the user logs in. */
	override fun redirectToAuthorizationEndpoint(leg: AuthorizationLeg) {
		val authorizationUrl = env.getString("redirect_to_authorization_endpoint")
		redirect(authorizationUrl)

		var loginPageVisited = false
		for (attempt in 0 until INITIAL_LOGIN_VISIT_TIMEOUT_SECS / 2) {
			status = TestModule.Status.WAITING
			try {
				Thread.sleep(2000)
			} catch (_: InterruptedException) {
			}
			status = TestModule.Status.RUNNING

			if (browser.visited.contains(authorizationUrl)) {
				loginPageVisited = true
				break
			}
		}
		if (!loginPageVisited) {
			throw RuntimeException("The initial authorization server login page was not visited within the $INITIAL_LOGIN_VISIT_TIMEOUT_SECS seconds timeout period.")
		}

		eventLog.log(name, "Make second request to authorization endpoint")
		super.redirectToAuthorizationEndpoint(leg)
	}

	override fun processCallback() {
		// the user must not authenticate until the second visit to the login page
		if (browser.visited.count { it == env.getString("redirect_to_authorization_endpoint") } < 2) {
			throw RuntimeException("The user was authenticated on the initial visit to login page. This must not be attempted until the second visit. " +
				"If no login was attempted then please delete related cookies or logout from the server before re-running.")
		}
		super.processCallback()
	}

	private companion object {
		const val INITIAL_LOGIN_VISIT_TIMEOUT_SECS = 120
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-par-attempt-reuse-request_uri",
	displayName = "Chile CMF OP: PAR - try to reuse a request_uri",
	summary = "This test tries to use a request_uri twice and expects the authorization server either show an error or redirect back with an invalid_request_uri error, PAR section 7.3 states 'the AS SHOULD make the request URIs one-time use'. If the authentication succeeds a warning will be issued.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPPARAttemptReuseRequestUri : AbstractCMFChileOPTestModule() {

	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(flow.beforeRedirect(), flow.afterCallback()),
			AuthorizationLeg(
				beforeRedirect = Steps.block(
					"Attempting reuse of request_uri and testing if Authorization server returns error in callback",
					condition(BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint::class.java)
				),
				// one-time use is only a SHOULD, so a successful second authentication is a warning and the flow is
				// then checked as normal
				afterCallback = flow.afterCallback(
					onError = continueOnFailure().requirements("PAR-2.2", "JAR-7").condition(EnsureInvalidRequestUriError::class.java),
					onSuccess = Steps.of(
						"Request uri reused",
						continueOnFailure().onFail(ConditionResult.WARNING).requirements("PAR-7.3")
							.condition(EnsureErrorFromAuthorizationEndpointResponse::class.java),
						flow.verifySuccessfulAuthorizationResponse
					)
				),
				placeholder = invalidRequestUriErrorPage("PAR-7.3", "PAR-4", "PAR-2.2")
			)
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-par-attempt-to-use-expired-request_uri",
	displayName = "Chile CMF OP: PAR - try to use an expired request_uri",
	summary = "This test tries to use a request_uri which has expired and expects authorization server to return an error. The test will call the PAR endpoint to create a new request_uri, then sleep until the expiry time for that request_uri before attempting to use it (this means this test may take some minutes to run, depending on the lifetime of the request_uri).",
	profile = CMF_OP_PROFILE
)
class CMFChileOPPARAttemptToUseExpiredRequestUri : AbstractCMFChileOPTestModule() {

	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(AuthorizationLeg(
			beforeRedirect = Steps.modify(flow.beforeRedirect())
				.insertBefore(
					BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint::class.java,
					Steps.block(
						"Attempting use of request_uri after expiry and testing if Authorization server returns error in callback",
						Steps.action("Skip when the request_uri lives too long") { env ->
							val expiresIn = env.getLong("expires_in")
							if (expiresIn != null && expiresIn > MAX_WAIT_SECONDS) {
								fireTestSkipped("The expires in value is longer than 30 minutes so is too long for the suite to wait for it to expire.")
							}
						},
						condition(WaitForExpiry::class.java)
					)
				)
				.build(),
			afterCallback = flow.errorCallback(
				authorizationErrorWithOptionalState(
					continueOnFailure().requirements("PAR-2.2").condition(EnsureInvalidRequestUriError::class.java)
				)
			),
			placeholder = invalidRequestUriErrorPage("PAR-2.2")
		))
	}

	private companion object {
		const val MAX_WAIT_SECONDS = 30 * 60
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-par-attempt-to-use-request_uri-for-different-client",
	displayName = "Chile CMF OP: PAR - try to use request_uri from client1 for client2",
	summary = "This test tries to use a request_uri (meant for client1) with client2 and expects authorization server to return an error",
	profile = CMF_OP_PROFILE
)
class CMFChileOPPAREnsureRequestUriIsBoundToClient : AbstractCMFChileOPMultipleClientTestModule() {

	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = Steps.modify(flow.beforeRedirect())
					.insertBefore(
						BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint::class.java,
						Steps.block(
							"Attempting to send client2's clientId with request_uri to AS and expect it returns error in callback",
							switchToSecondClient(),
							stopOnFailure().requirements("PAR-4").condition(AddClientIdToAuthorizationEndpointRequest::class.java)
						)
					)
					.build(),
				afterCallback = flow.errorCallback(
					authorizationErrorWithOptionalState(
						continueOnFailure().requirements("PAR-3-3").condition(EnsureInvalidRequestInvalidRequestObjectOrInvalidRequestUriError::class.java)
					)
				),
				placeholder = invalidRequestUriErrorPage("PAR-3-3")
			)
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-par-authorization-request-containing-request_uri-form-param",
	displayName = "Chile CMF OP: PAR - the request_uri authorization request parameter MUST NOT be provided",
	summary = "This test sends a random request_uri form parameter in PAR authorization request and expects authorization server to return an error",
	profile = CMF_OP_PROFILE
)
class CMFChileOPPARRejectRequestUriInParAuthorizationFormParams : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		parRequestRejectedLeg(
			beforeCall = condition(AddBadRequestUriToRequestParameters::class.java),
			check = continueOnFailure().requirements("PAR-2.1-2")
				.condition(EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError::class.java)
		)
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-par-attempt-invalid-http-method",
	displayName = "Chile CMF OP: PAR - try to use an invalid http method",
	summary = "This test tries to use an invalid http method and expects authorization server to return an error",
	profile = CMF_OP_PROFILE
)
class CMFChileOPPARRejectInvalidHttpVerb : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		parRequestRejectedLeg(
			beforeCall = Steps.action("Use PUT for the PAR request") { env -> env.putString(CallPAREndpoint.HTTP_METHOD_KEY, "PUT") },
			check = continueOnFailure().requirements("PAR-2.3").condition(EnsureParHTTPError::class.java)
		)
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-par-authorization-request-containing-request_uri",
	displayName = "Chile CMF OP: PAR - authorization request must not contain request_uri parameter",
	summary = "This test sends a random request_uri parameter in authorization request object and expects authorization server to return an error",
	profile = CMF_OP_PROFILE
)
class CMFChileOPPARRejectRequestUriInParAuthorizationRequest : AbstractCMFChileOPTestModule() {
	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			parEndpointOnlyLeg(
				Steps.of(
					"Request object with request_uri",
					flow.createAuthorizationRequest,
					Steps.modify(flow.createRequestObject)
						.insertBefore(
							ConvertAuthorizationEndpointRequestToRequestObject::class.java,
							stopOnFailure().requirements("PAR-2").condition(AddBadRequestUriToAuthorizationRequest::class.java)
						)
						.build(),
					flow.pushAuthorizationRequest,
					// the spec mentions this error without requiring it; the firm requirement is a failure status
					parInvalidRequestObject("JAR-6.2", "PAR-2.1")
				)
			)
		)
	}
}

/**
 * PKCE the server must reject (FAPI2 5.3.2.2): at the PAR endpoint with invalid_request, at the authorization
 * endpoint with invalid_request, or with an error page.
 */
abstract class AbstractCMFChileOPPKCERejected : AbstractCMFChileOPTestModule() {

	/** Creates the placeholder for the error page. */
	protected abstract val errorPage: TestExecutionUnit

	/** Makes the PKCE parameters of the authorization request unacceptable. */
	protected abstract fun breakPkce(modifier: Steps.Modifier)

	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = errorPage,
			parRejected = continueOnFailure().requirements("RFC7636-4.4.1").condition(EnsurePARInvalidRequestError::class.java),
			callback = expectAuthorizationError(
				continueOnFailure().requirements("RFC7636-4.4.1").condition(EnsureInvalidRequestError::class.java)
			),
			modify = { breakPkce(this) }
		)
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-par-ensure-pkce-required",
	displayName = "Chile CMF OP: ensure pkce is required when using pushed authorization requests",
	summary = "This test makes a FAPI authorization request without using PKCE (RFC7636), which must be rejected. FAPI2-Security-Profile-Final requires servers to reject PAR requests that do not use PKCE, clause 5.2.2-18. Depending on when the server chooses to verify the request, the refusal may be an error from the pushed authorization request endpoint, or an invalid_request error may be returned from the authorization endpoint, or an error may be shown to the user (a screenshot of which must be uploaded).",
	profile = CMF_OP_PROFILE
)
class CMFChileOPPAREnsurePKCERequired : AbstractCMFChileOPPKCERejected() {
	override val errorPage: TestExecutionUnit = expectErrorPage(
		stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-5").condition(ExpectPkceMissingErrorPage::class.java),
		"pkce_missing_error"
	)

	override fun breakPkce(modifier: Steps.Modifier) {
		modifier
			.skip(CreateRandomCodeVerifier::class.java, "PKCE is not used")
			.skip(CreateS256CodeChallenge::class.java, "PKCE is not used")
			.skip(AddCodeChallengeToAuthorizationEndpointRequest::class.java, "PKCE is not used")
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-par-plain-pkce-rejected",
	displayName = "Chile CMF OP: ensure plain pkce is rejected",
	summary = "This test makes a FAPI authorization request using plain PKCE (RFC7636), which must be rejected. FAPI2-Security-Profile-Final requires servers to reject PAR requests that do not use S256, clause 5.2.2-18. Depending on when the server chooses to verify the request, the refusal may be an error from the pushed authorization request endpoint, or an invalid_request error may be returned from the authorization endpoint, or an error may be shown to the user (a screenshot of which must be uploaded).",
	profile = CMF_OP_PROFILE
)
class CMFChileOPPAREnsurePlainPKCERejected : AbstractCMFChileOPPKCERejected() {
	override val errorPage: TestExecutionUnit = expectErrorPage(
		stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-5").condition(ExpectPlainPkceErrorPage::class.java),
		"plain_pkce_error"
	)

	override fun breakPkce(modifier: Steps.Modifier) {
		modifier.replace(CreateS256CodeChallenge::class.java, condition(CreatePlainCodeChallenge::class.java))
	}
}

private fun invalidRequestUriErrorPage(vararg requirements: String): TestExecutionUnit =
	expectErrorPage(
		stopOnFailure().requirements(*requirements).condition(ExpectInvalidRequestUriErrorPage::class.java),
		"request_uri_invalid_error"
	)

