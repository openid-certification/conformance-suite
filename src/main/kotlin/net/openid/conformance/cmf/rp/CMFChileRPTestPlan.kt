package net.openid.conformance.cmf.rp

import net.openid.conformance.plan.PublishTestPlan
import net.openid.conformance.plan.TestPlan
import net.openid.conformance.variant.VariantSelection

/**
 * The FAPI2 Message Signing client tests, as the Chile Open Finance (CMF) profile fixes them: tls_client_auth, mTLS
 * sender constraining, PAR with signed request objects, RAR, plain responses and grant management. The only
 * parameter left to the tester is whether OpenID Connect is used.
 */
@PublishTestPlan(
	testPlanName = "cmf-chile-fapi2-message-signing-rp-test-plan",
	shortName = "Final + Chile CMF",
	certifiable = true,
	alpha = true,
	displayName = ("Chile Open Finance (CMF): FAPI2-Message-Signing-Final - Relying Party (client) test "
		+ "(alpha version - may be incomplete or incorrect, please email certification@oidf.org)"),
	profile = TestPlan.ProfileNames.rptest,
	specFamily = TestPlan.SpecFamilyNames.fapi2MessageSigning,
	specVersion = TestPlan.SpecVersionNames.fapi2MsFinal,
	summary = ("Tests a client implementing FAPI2 Message Signing under the Chile Open Finance (CMF) profile. "
		+ "Configure the client's tls_client_auth certificate, its request object signing key and its redirect uri.")
)
class CMFChileRPTestPlan : TestPlan {

	override fun testModulesWithVariants(): List<TestPlan.ModuleListEntry> =
		listOf(TestPlan.ModuleListEntry(testModules, listOf()))

	override fun certificationProfileName(variant: VariantSelection): List<String> = listOf("FAPI2MS RP CL-OF")

	companion object {
		@JvmField
		val testModules = listOf(
			CMFChileRPHappyPath::class.java,
			CMFChileRPDiscoveryIssuerMismatch::class.java,
			CMFChileRPInvalidIss::class.java,
			CMFChileRPInvalidAud::class.java,
			CMFChileRPInvalidSecondaryAud::class.java,
			CMFChileRPInvalidNullAlg::class.java,
			CMFChileRPInvalidAlternateAlg::class.java,
			CMFChileRPInvalidExpiredExp::class.java,
			CMFChileRPInvalidMissingExp::class.java,
			CMFChileRPInvalidMissingAud::class.java,
			CMFChileRPInvalidMissingIss::class.java,
			CMFChileRPValidAudAsArray::class.java,
			CMFChileRPInvalidNonce::class.java,
			CMFChileRPInvalidMissingNonce::class.java,
			CMFChileRPInvalidAuthorizationResponseIss::class.java,
			CMFChileRPRemoveAuthorizationResponseIss::class.java,
			CMFChileRPEnsureAuthorizationResponseWithInvalidStateFails::class.java,
			CMFChileRPEnsureAuthorizationResponseWithInvalidMissingStateFails::class.java,
			CMFChileRPTokenEndpointResponseWithoutExpiresIn::class.java,
			CMFChileRPTokenTypeCaseInsensitivity::class.java,
			CMFChileRPHappyPathNoMtlsEndpointAliases::class.java,
			CMFChileRPRefreshToken::class.java,
			CMFChileRPGrantManagementHappyPath::class.java,
			CMFChileRPGrantManagementQueryAndRevoke::class.java,
			CMFChileRPGrantManagementInvalidGrantIdFails::class.java
		)
	}
}
