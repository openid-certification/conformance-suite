package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

/**
 * The FAPI2 client DCR module under the Chile Open Finance profile. Chile requires JAR, so this is a
 * Message Signing plan, with the variants the profile mandates fixed (see the openbanking_chile case
 * of {@link FAPI2MessageSigningFinalClientTestPlan#certificationProfileName}).
 */
@PublishTestPlan (
	testPlanName = "fapi2-message-signing-final-chile-dcr-client-test-plan",
	shortName = "Final + Chile Dynamic Client Registration",
	certifiable = true,
	alpha = true,
	displayName = "FAPI2-Message-Signing-Final: Chile Open Finance Dynamic Client Registration Relying Party (client) test (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.rptest,
	specFamily = TestPlan.SpecFamilyNames.fapi2MessageSigning,
	specVersion = TestPlan.SpecVersionNames.fapi2MsFinal
)
public class FAPI2MessageSigningFinalChileDCRClientTestPlan implements TestPlan {

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(FAPI2SPFinalBrazilClientDCRHappyPathTest.class),
				List.of(
					new Variant(FAPI2FinalOPProfile.class, "openbanking_chile"),
					new Variant(ClientAuthType.class, "mtls"),
					new Variant(FAPI2SenderConstrainMethod.class, "mtls"),
					new Variant(FAPI2AuthRequestMethod.class, "signed_non_repudiation"),
					new Variant(AuthorizationRequestType.class, "rar"),
					new Variant(FAPIResponseMode.class, "plain_response"),
					new Variant(GrantManagement.class, "enabled")
				)
			)
		);
	}

	@Override
	public List<Variant> variantsNotApplicable() {
		return FAPI2SPFinalTestPlan.FAPI2_VARIANTS_NOT_APPLICABLE;
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variant) {
		return List.of("FAPI2MS RP CL-OF DCR");
	}
}
