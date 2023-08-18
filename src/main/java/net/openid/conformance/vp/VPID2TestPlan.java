package net.openid.conformance.vp;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.ServerMetadata;

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
						VPID2HappyFlowRequestUriUnsigned.class

					// FIXME: more tests
					// positive tests:
					// try request without redirect_uri returned from direct post [not allowed by HAIP?]
					// try sending presentation_definition_uri [but this is not allowed by HAIP]
					// try response_mode "direct_post.jwt"
					// include nonce
					// negative tests:
					// try sending a redirect_uri in auth request with response_mode=direct_post
					// sending client_id != response_uri when using redirect client scheme + direct post
					// sending invalid client_id_scheme should cause an error?
					// flow without nonce
				),
				List.of(
					// a hack; not actually id_token we'll hardwire vp token
					new Variant(ResponseType.class, "id_token"),
					// FIXME: confirm if most of these options should be removed as I don't think they're ever going to be applicable
					new Variant(ServerMetadata.class, "static"),
					new Variant(ClientRegistration.class, "static_client"),
					new Variant(ClientAuthType.class, "none"),
					new Variant(ResponseMode.class, "default")
				)
			)
		);
	}

	}
