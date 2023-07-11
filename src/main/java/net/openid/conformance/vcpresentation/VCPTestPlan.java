package net.openid.conformance.vcpresentation;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ResponseType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "vcp-test-plan",
	displayName = "OpenID for Verifiable Credential Presentation: Alpha tests (not part of certification program)",
	profile = TestPlan.ProfileNames.wallettest
)
public class VCPTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
						VCPHappyFlowRequestUriUnsigned.class

					// FIXME: more tests
					// positive tests:
					// try request without redirect_uri returned from direct post [not allowed by HAIP?]
					// try sending presentation_definition_uri [but this is not allowed by HAIP]
					// try response_mode "direct_post.jwt"
					// include nonce
					// negative tests:
					// try sending a redirect_uri in auth request with response_mode=direct_post
					// sending invalid client_id_scheme should cause an error?
					// flow without nonce
				),
				List.of(
					// a hack; not actually code we'll hardwire vp token
					new Variant(ResponseType.class, "code")
				)
			)
		);
	}

	}
