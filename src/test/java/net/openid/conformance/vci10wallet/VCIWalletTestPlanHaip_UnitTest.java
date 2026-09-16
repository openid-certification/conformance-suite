package net.openid.conformance.vci10wallet;

import net.openid.conformance.variant.VariantSelection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VCIWalletTestPlanHaip_UnitTest {

	private final VCIWalletTestPlanHaip plan = new VCIWalletTestPlanHaip();

	private static VariantSelection selection(String codeFlowVariant, String credentialOfferVariant) {
		return new VariantSelection(Map.of(
			"credential_format", "sd_jwt_vc",
			"vci_authorization_code_flow_variant", codeFlowVariant,
			"vci_credential_offer_variant", credentialOfferVariant));
	}

	@Test
	public void walletInitiatedFlowOmitsTheCredentialOfferVariantEvenWhenSelected() {
		List<String> names = plan.certificationProfileName(selection("wallet_initiated", "by_value"));

		assertEquals(List.of("OID4VCI-1.0-FINAL+HAIP-1.0-FINAL Wallet sd_jwt_vc wallet_initiated"), names);
	}

	@ParameterizedTest
	@CsvSource({"by_value", "by_reference"})
	public void issuerInitiatedFlowIncludesTheCredentialOfferVariant(String credentialOfferVariant) {
		List<String> names = plan.certificationProfileName(selection("issuer_initiated", credentialOfferVariant));

		assertEquals(List.of("OID4VCI-1.0-FINAL+HAIP-1.0-FINAL Wallet sd_jwt_vc issuer_initiated " + credentialOfferVariant), names);
	}

	@Test
	public void missingCredentialOfferVariantIsOmitted() {
		VariantSelection selection = new VariantSelection(Map.of(
			"credential_format", "mdoc",
			"vci_authorization_code_flow_variant", "issuer_initiated"));

		List<String> names = plan.certificationProfileName(selection);

		assertEquals(List.of("OID4VCI-1.0-FINAL+HAIP-1.0-FINAL Wallet mdoc issuer_initiated"), names);
	}
}
