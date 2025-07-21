package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi2-message-signing-final-client-test-plan",
	displayName = "FAPI2-Message-Signing-Final: Relying Party (client) test",
	profile = TestPlan.ProfileNames.rptest
)
public class FAPI2MessageSigningFinalClientTestPlan implements TestPlan {
	public static final List<Class<? extends TestModule>> testModules = List.of(
		FAPI2SPFinalClientTestHappyPath.class,
		FAPI2SPFinalClientTestInvalidIss.class,
		FAPI2SPFinalClientTestInvalidAud.class,
		FAPI2SPFinalClientTestInvalidSecondaryAud.class,
		FAPI2SPFinalClientTestInvalidNullAlg.class,
		FAPI2SPFinalClientTestInvalidAlternateAlg.class,
		FAPI2SPFinalClientTestInvalidExpiredExp.class,
		FAPI2SPFinalClientTestInvalidMissingExp.class,
		FAPI2SPFinalClientTestInvalidMissingAud.class,
		FAPI2SPFinalClientTestInvalidMissingIss.class,
		FAPI2SPFinalClientTestValidAudAsArray.class,
		FAPI2SPFinalClientTestInvalidNonce.class,
		FAPI2SPFinalClientTestInvalidMissingNonce.class,
		FAPI2SPFinalClientTestInvalidAuthorizationResponseIss.class,
		FAPI2SPFinalClientTestRemoveAuthorizationResponseIss.class,
		FAPI2SPFinalClientTestEnsureAuthorizationResponseWithInvalidStateFails.class,
		FAPI2SPFinalClientTestEnsureAuthorizationResponseWithInvalidMissingStateFails.class,
		FAPI2SPFinalClientTestTokenEndpointResponseWithoutExpiresIn.class,
		FAPI2SPFinalClientTestTokenTypeCaseInsenstivity.class,

		// Happy path for DPoP sender constrained without DPoP nonce
		FAPI2SPFinalClientTestHappyPathNoDpopNonce.class,

		// JARM tests
		FAPI2SPFinalClientTestEnsureJarmWithoutIssFails.class,
		FAPI2SPFinalClientTestEnsureJarmWithInvalidIssFails.class,
		FAPI2SPFinalClientTestEnsureJarmWithoutAudFails.class,
		FAPI2SPFinalClientTestEnsureJarmWithInvalidAudFails.class,
		FAPI2SPFinalClientTestEnsureJarmWithoutExpFails.class,
		FAPI2SPFinalClientTestEnsureJarmWithExpiredExpFails.class,
		FAPI2SPFinalClientTestEnsureJarmWithInvalidSigFails.class,
		FAPI2SPFinalClientTestEnsureJarmSignatureAlgIsNotNone.class,

		// OB systems specific tests
		FAPI2SPFinalClientTestInvalidOpenBankingIntentId.class,
		//Brazil specific
		FAPI2SPFinalClientRefreshTokenTest.class
	);

	public static List<ModuleListEntry> testModulesWithVariants() {
		List<Variant> variant = List.of(
		);

		return List.of(
			new ModuleListEntry(testModules, variant)
		);

	}

	public static List<String> certificationProfileName(VariantSelection variant) {

		List<String> profiles = new ArrayList<>();
		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String requestMethod = v.get("fapi_request_method");
		String responseMode = v.get("fapi_response_mode");
		String senderConstrain = v.get("sender_constrain");
		String authRequestType = v.get("authorization_request_type");
		boolean jarm = responseMode.equals("jarm");
		boolean privateKey = clientAuth.equals("private_key_jwt");
		boolean dpop = senderConstrain.equals("dpop");
		boolean mtlsBounded = senderConstrain.equals("mtls");
		boolean signedRequest = requestMethod.equals("signed_non_repudiation");
		String clientType = v.get("fapi_client_type");
		boolean openid = clientType.equals("oidc");
		boolean rar = "rar".equals(authRequestType);

		String certProfile = "FAPI2SP SP ";

		if (openid) {
			profiles.add(certProfile + "OpenID Connect");
		}

		switch (profile) {
			case "plain_fapi":
				break;
			case "openbanking_uk":
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM is not used in UK".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				return List.of( "FAPI2MS RP UK-OB");
			case "consumerdataright_au":
				certProfile = "AU-CDR";
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for AU-CDR".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM is not used in AU-CDR".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openbanking_brazil":
				return List.of( "FAPI2MS RP BR-OB");
				break;
			case "connectid_au":
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for ConnectID".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!signedRequest) {
					throw new RuntimeException("Invalid configuration for %s: Only signed requests are required for ConnectID".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (dpop) {
					throw new RuntimeException("Invalid configuration for %s: DPoP sender constraining is not used for ConnectID".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM responses are not used for ConnectID".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				// as there's only one possible correct configuration, stop here and return just the name
				return List.of( "FAPI2MS RP with ConnectId support");
			case "cbuae":
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for CBUAE".formatted(
							MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!signedRequest) {
					throw new RuntimeException("Invalid configuration for %s: Only signed requests are supported for CBUAE".formatted(
							MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!rar) {
					throw new RuntimeException("Invalid configuration for %s: Only signed requests are supported for CBUAE".formatted(
							MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!mtlsBounded) {
					throw new RuntimeException("Invalid configuration for %s: Only MTLS sender constraining is supported for CBUAE".formatted(
							MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM responses are not used for CBUAE".formatted(
							MethodHandles.lookup().lookupClass().getSimpleName()));
				}

				return List.of( "FAPI2MS RP CBUAE RP");
		}

		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += " private key";
				break;
			case "mtls":
				certProfile += " MTLS";
				break;
		}
		switch (senderConstrain) {
			case "mtls":
				certProfile += " + MTLS";
				break;
			case "dpop":
				certProfile += " + DPoP";
				break;
		}
		profiles.add( certProfile );

		switch (requestMethod) {
			case "unsigned":
				break;
			case "signed_non_repudiation":
				profiles.add( "FAPI2MS RP JAR" );
				break;
		}
		switch (responseMode) {
			case "plain_response":
				// nothing
				break;
			case "jarm":
				profiles.add( "FAPI2MS RP JARM" );
				break;
		}

		return profiles;
	}
}
