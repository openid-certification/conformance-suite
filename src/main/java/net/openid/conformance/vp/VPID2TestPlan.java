package net.openid.conformance.vp;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "vp-test-plan",
	displayName = "OpenID for Verifiable Presentations ID2: Alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.wallettest
)
public class VPID2TestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					VPID2HappyFlowNoState.class,
					VPID2HappyFlowWithStateAndRedirect.class,
					VPID2ResponseUriNotClientId.class,
					VPID2InvalidRequestObjectSignature.class

					// FIXME: more tests
					// positive tests:
					// try request without redirect_uri returned from direct post [not allowed by HAIP?]
					// try sending presentation_definition_uri [but this is not allowed by HAIP]
					// try response_mode "direct_post.jwt"
					// negative tests:
					// try sending a redirect_uri in auth request with response_mode=direct_post
					// sending client_id != response_uri when using redirect client scheme + direct post
					// sending invalid client_id_scheme should cause an error?
					// flow without nonce
					// for x509 client id scheme, try a client_id that's not permitted by the cert? FIXME
				),
				List.of(
					// a hack; not actually id_token we'll hardwire vp token
					new Variant(ResponseType.class, "id_token"),
					// FIXME: confirm if most of these options should be removed as I don't think they're ever going to be applicable
					new Variant(ServerMetadata.class, "static"),
					new Variant(ClientRegistration.class, "static_client")
				)
			)
		);
	}
	public static String certificationProfileName(VariantSelection variant) {

//		Map<String, String> v = variant.getVariant();
//		String responseMode = v.get("response_mode");
//		String credentialFormat = v.get("credential_format");
//		String requestMethod = v.get("request_method");
//		String clientIDScheme = v.get("client_id_scheme");

		String certProfile = "OID4VPID2";

//		if (credentialFormat.equals(CredentialFormat.ISO_MDL.toString())) {
//			if (!responseMode.equals(VPResponseMode.DIRECT_POST_JWT.toString())) {
//				throw new RuntimeException(String.format("Invalid configuration for %s: ISO mDL requires direct_post.jwt",
//					MethodHandles.lookup().lookupClass().getSimpleName()));
//			}
//			if (!requestMethod.equals(VPRequestMethod.REQUEST_URI_SIGNED.toString())) {
//				throw new RuntimeException(String.format("Invalid configuration for %s: ISO mDL requires signed request uri",
//					MethodHandles.lookup().lookupClass().getSimpleName()));
//			}
//			if (!clientIDScheme.equals(VPClientIdScheme.X509_SAN_DNS.toString())) {
//				throw new RuntimeException(String.format("Invalid configuration for %s: ISO mDL requires client_id_scheme x509_san_dns",
//					MethodHandles.lookup().lookupClass().getSimpleName()));
//			}
//		}

		return certProfile;
	}

}
