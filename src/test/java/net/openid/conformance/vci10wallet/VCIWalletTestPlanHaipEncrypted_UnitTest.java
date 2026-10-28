package net.openid.conformance.vci10wallet;

import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.variant.VCICredentialIssuanceMode;
import net.openid.conformance.variant.VariantSelection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class VCIWalletTestPlanHaipEncrypted_UnitTest {

	private final VCIWalletTestPlanHaipEncrypted plan = new VCIWalletTestPlanHaipEncrypted();

	@Test
	public void everyEntryUsesEncryption() {
		for (TestPlan.ModuleListEntry entry : plan.testModulesWithVariants()) {
			assertThat(entry.variant)
				.anySatisfy(v -> {
					assertThat(v.key).isEqualTo(VCICredentialEncryption.class);
					assertThat(v.value).isEqualTo("encrypted");
				});
		}
	}

	@Test
	public void runsTheHaipModulesImmediateAndDeferredAndTheFapi2ClientModules() {
		List<TestPlan.ModuleListEntry> entries = plan.testModulesWithVariants();
		Set<String> issuanceModes = entries.stream()
			.flatMap(e -> e.variant.stream())
			.filter(v -> v.key == VCICredentialIssuanceMode.class)
			.map(v -> v.value)
			.collect(Collectors.toSet());
		assertThat(issuanceModes).containsExactlyInAnyOrder("immediate", "deferred");
		assertThat(entries.get(2).testModules).containsExactlyElementsOf(VCIWalletTestPlanHaip.vciFapi2SPFinalClientTestModules());
	}

	@Test
	public void certificationProfileNameMarksTheEncryptedPlan() {
		VariantSelection selection = new VariantSelection(Map.of(
			"credential_format", "sd_jwt_vc",
			"vci_authorization_code_flow_variant", "issuer_initiated",
			"vci_credential_offer_variant", "by_value"));
		assertThat(plan.certificationProfileName(selection))
			.containsExactly("OID4VCI-1.0-FINAL+HAIP-1.0-FINAL Wallet sd_jwt_vc issuer_initiated by_value encrypted");
	}
}
