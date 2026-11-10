package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.variant.VariantSelection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FAPI2MessageSigningChileCertificationProfile_UnitTest {

	private static Map<String, String> chileVariant(String clientAuth) {
		Map<String, String> v = new HashMap<>();
		v.put("fapi_profile", "openbanking_chile");
		v.put("client_auth_type", clientAuth);
		v.put("fapi_request_method", "signed_non_repudiation");
		v.put("fapi_response_mode", "plain_response");
		v.put("sender_constrain", "mtls");
		v.put("authorization_request_type", "rar");
		v.put("grant_management", "enabled");
		v.put("openid", "openid_connect");
		v.put("fapi_client_type", "oidc");
		return v;
	}

	@Test
	public void opPlanAcceptsMtlsClientAuthentication() {
		List<String> names = new FAPI2MessageSigningFinalTestPlan()
			.certificationProfileName(new VariantSelection(chileVariant("mtls")));

		assertThat(names).containsExactly("FAPI2MS OP CL-OF");
	}

	@Test
	public void rpPlanAcceptsMtlsClientAuthentication() {
		List<String> names = new FAPI2MessageSigningFinalClientTestPlan()
			.certificationProfileName(new VariantSelection(chileVariant("mtls")));

		assertThat(names).containsExactly("FAPI2MS RP CL-OF");
	}

	@ParameterizedTest
	@ValueSource(strings = {"private_key_jwt", "client_attestation"})
	public void opPlanRejectsClientAuthenticationOtherThanMtls(String clientAuth) {
		VariantSelection selection = new VariantSelection(chileVariant(clientAuth));

		assertThatThrownBy(() -> new FAPI2MessageSigningFinalTestPlan().certificationProfileName(selection))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Only MTLS client authentication is used for Chile");
	}

	@ParameterizedTest
	@ValueSource(strings = {"private_key_jwt", "client_attestation"})
	public void rpPlanRejectsClientAuthenticationOtherThanMtls(String clientAuth) {
		VariantSelection selection = new VariantSelection(chileVariant(clientAuth));

		assertThatThrownBy(() -> new FAPI2MessageSigningFinalClientTestPlan().certificationProfileName(selection))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Only MTLS client authentication is used for Chile");
	}
}
