package net.openid.conformance.cmf.rp

import net.openid.conformance.condition.`as`.AddACRClaimToIdTokenClaims
import net.openid.conformance.condition.`as`.AddInvalidAudValueToIdToken
import net.openid.conformance.condition.`as`.AddInvalidExpiredExpValueToIdToken
import net.openid.conformance.condition.`as`.AddInvalidIssValueToIdToken
import net.openid.conformance.condition.`as`.AddInvalidNonceValueToIdToken
import net.openid.conformance.condition.`as`.AddUntrustedSecondAudValueToIdToken
import net.openid.conformance.condition.`as`.CreateAuthorizationCode
import net.openid.conformance.condition.`as`.CreateEffectiveAuthorizationRequestParameters
import net.openid.conformance.condition.`as`.ForceIdTokenToBeSignedWithAltRS256
import net.openid.conformance.condition.`as`.RemoveAudFromIdToken
import net.openid.conformance.condition.`as`.RemoveExpFromIdToken
import net.openid.conformance.condition.`as`.RemoveIssFromIdToken
import net.openid.conformance.condition.`as`.RemoveNonceFromIdToken
import net.openid.conformance.condition.`as`.SignIdToken
import net.openid.conformance.condition.`as`.SignIdTokenWithNullAlgorithm
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.variant.ConfigurationFields
import net.openid.conformance.variant.FAPIClientType
import net.openid.conformance.variant.VariantNotApplicable

/**
 * A test whose token endpoint returns an invalid id_token, after which the client must not call the server again
 * (OIDCC 3.1.3.7).
 */
@VariantNotApplicable(parameter = FAPIClientType::class, values = ["plain_oauth"])
@ConfigurationFields("waitTimeoutSeconds")
abstract class AbstractCMFChileRPInvalidIdTokenTest : AbstractCMFChileRPTestModule() {

	/** What is wrong with the id_token, as shown to the tester. */
	protected abstract val fault: String

	/** Changes the id_token claims before they are signed. */
	protected open fun claimsFault(): TestExecutionUnit = Steps.NONE

	/** Signs the id_token again, replacing the valid signature. */
	protected open fun signatureFault(): TestExecutionUnit = Steps.NONE

	override fun server(): RPServer {
		val server = super.server()
		return server.copy(
			tokenEndpoint = Steps.modify(server.tokenEndpoint)
				.insertBefore(AddACRClaimToIdTokenClaims::class.java, claimsFault())
				.insertAfter(
					SignIdToken::class.java,
					Steps.of(null, signatureFault(), clientMustStopAfter("an invalid id_token ($fault)"))
				)
				.build()
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-rp-invalid-iss",
	displayName = "Chile CMF RP: client test - invalid iss in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the iss value in the id_token does not match the authorization server's issuer",
	profile = CMF_RP_PROFILE
)
class CMFChileRPInvalidIss : AbstractCMFChileRPInvalidIdTokenTest() {
	override val fault = "invalid iss value"

	override fun claimsFault(): TestExecutionUnit =
		stopOnFailure().requirements("OIDCC-3.1.3.7-2").condition(AddInvalidIssValueToIdToken::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-rp-invalid-aud",
	displayName = "Chile CMF RP: client test - invalid aud in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the aud value in the id_token does not match the client id",
	profile = CMF_RP_PROFILE
)
class CMFChileRPInvalidAud : AbstractCMFChileRPInvalidIdTokenTest() {
	override val fault = "invalid aud value"

	override fun claimsFault(): TestExecutionUnit =
		stopOnFailure().requirements("OIDCC-3.1.3.7-3").condition(AddInvalidAudValueToIdToken::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-rp-invalid-secondary-aud",
	displayName = "Chile CMF RP: client test - untrusted aud value in id_token from token_endpoint, must be rejected",
	summary = "This test issues an id_token where the 'aud' is an array which contains both the correct client_id and the id for a non-existent client. This test should end with the client displaying an error message that there is an untrusted aud value in the id_token from the token_endpoint, or that the 'azp' claim is missing.\n\nAs per OpenID Connect section 3.1.3.7 clause 3:\n\n'The ID Token MUST be rejected if the ID Token does not list the Client as a valid audience, or if it contains additional audiences not trusted by the Client.'\n\n and clause 4:\n\n'If the ID Token contains multiple audiences, the Client SHOULD verify that an azp Claim is present.'",
	profile = CMF_RP_PROFILE
)
class CMFChileRPInvalidSecondaryAud : AbstractCMFChileRPInvalidIdTokenTest() {
	override val fault = "aud is an array that contains an untrusted value"

	override fun claimsFault(): TestExecutionUnit =
		stopOnFailure().requirements("OIDCC-3.1.3.7-3").condition(AddUntrustedSecondAudValueToIdToken::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-rp-invalid-null-alg",
	displayName = "Chile CMF RP: client test - null algorithm used for serialization of id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the id_token was signed with alg: none",
	profile = CMF_RP_PROFILE
)
class CMFChileRPInvalidNullAlg : AbstractCMFChileRPInvalidIdTokenTest() {
	override val fault = "null alg value"

	override fun signatureFault(): TestExecutionUnit =
		stopOnFailure().requirements("OIDCC-3.1.3.7-7").condition(SignIdTokenWithNullAlgorithm::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-rp-invalid-alternate-alg",
	displayName = "Chile CMF RP: client test - the id_token is signed with RS256, which the client should not accept as it is expecting PS256, ES256, or EdDSA",
	summary = "This test should end with the client displaying an error message that the algorithm used to sign the id_token does not match the required algorithm",
	profile = CMF_RP_PROFILE
)
class CMFChileRPInvalidAlternateAlg : AbstractCMFChileRPInvalidIdTokenTest() {
	override val fault = "signed using alt RS256 alg."

	override fun signatureFault(): TestExecutionUnit =
		stopOnFailure().requirements("OIDCC-3.1.3.7-8").condition(ForceIdTokenToBeSignedWithAltRS256::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-rp-invalid-expired-exp",
	displayName = "Chile CMF RP: client test - expired exp value in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the exp value in the id_token from the token_endpoint has expired more than 5 minutes in the past",
	profile = CMF_RP_PROFILE
)
class CMFChileRPInvalidExpiredExp : AbstractCMFChileRPInvalidIdTokenTest() {
	override val fault = "expired exp value"

	override fun claimsFault(): TestExecutionUnit =
		stopOnFailure().requirements("OIDCC-3.1.3.7-9").condition(AddInvalidExpiredExpValueToIdToken::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-rp-invalid-missing-exp",
	displayName = "Chile CMF RP: client test - missing exp value in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the exp value in the id_token from the token_endpoint is missing",
	profile = CMF_RP_PROFILE
)
class CMFChileRPInvalidMissingExp : AbstractCMFChileRPInvalidIdTokenTest() {
	override val fault = "missing exp value"

	override fun claimsFault(): TestExecutionUnit =
		stopOnFailure().requirements("OIDCC-3.1.3.7-9").condition(RemoveExpFromIdToken::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-rp-invalid-missing-aud",
	displayName = "Chile CMF RP: client test - missing aud value in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the aud value in the id_token from the token_endpoint is missing",
	profile = CMF_RP_PROFILE
)
class CMFChileRPInvalidMissingAud : AbstractCMFChileRPInvalidIdTokenTest() {
	override val fault = "missing aud value"

	override fun claimsFault(): TestExecutionUnit =
		stopOnFailure().requirements("OIDCC-3.1.3.7-3").condition(RemoveAudFromIdToken::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-rp-invalid-missing-iss",
	displayName = "Chile CMF RP: client test - missing iss value in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the iss value in the id_token from the token_endpoint is missing",
	profile = CMF_RP_PROFILE
)
class CMFChileRPInvalidMissingIss : AbstractCMFChileRPInvalidIdTokenTest() {
	override val fault = "missing iss value"

	override fun claimsFault(): TestExecutionUnit =
		stopOnFailure().requirements("OIDCC-3.1.3.7-2").condition(RemoveIssFromIdToken::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-rp-invalid-nonce",
	displayName = "Chile CMF RP: client test - invalid nonce in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the nonce value in the id_token does not match the authorization request's nonce value",
	profile = CMF_RP_PROFILE
)
class CMFChileRPInvalidNonce : AbstractCMFChileRPInvalidIdTokenTest() {
	override val fault = "invalid nonce value"

	override fun claimsFault(): TestExecutionUnit =
		stopOnFailure().requirements("OIDCC-3.1.3.7-11").condition(AddInvalidNonceValueToIdToken::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-rp-invalid-missing-nonce",
	displayName = "Chile CMF RP: client test - missing nonce in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the nonce in the id_token is missing if the authorization request supplied one. If the client does not send a nonce value the test result will be SKIPPED.",
	profile = CMF_RP_PROFILE
)
class CMFChileRPInvalidMissingNonce : AbstractCMFChileRPInvalidIdTokenTest() {
	override val fault = "missing nonce value"

	override fun claimsFault(): TestExecutionUnit =
		stopOnFailure().requirements("OIDCC-3.1.3.7-11").condition(RemoveNonceFromIdToken::class.java)

	override fun server(): RPServer {
		val server = super.server()
		return server.copy(
			authorizationEndpoint = Steps.modify(server.authorizationEndpoint)
				.insertBefore(
					CreateAuthorizationCode::class.java,
					Steps.`when`(
						"the client sent no nonce",
						{ env ->
							env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.NONCE)
								.isNullOrEmpty()
						},
						Steps.action("Skip the test") {
							fireTestSkipped(
								"This test is being skipped as it relies on the client supplying an OPTIONAL nonce value - since none is " +
									"supplied, this can not be tested. PKCE prevents CSRF so this is acceptable and will not prevent certification."
							)
						}
					)
				)
				.build()
		)
	}
}
