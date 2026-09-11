package net.openid.conformance.openid.ssf;

import net.openid.conformance.variant.VariantSelection;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OIDSSFCertification_UnitTest {

	private static VariantSelection selection(String authMode, String delivery, String clientAuth) {
		Map<String, String> variant = new HashMap<>();
		variant.put("ssf_profile", "caep_interop");
		if (authMode != null) {
			variant.put("ssf_auth_mode", authMode);
		}
		if (delivery != null) {
			variant.put("ssf_delivery_mode", delivery);
		}
		if (clientAuth != null) {
			variant.put("client_auth_type", clientAuth);
		}
		return new VariantSelection(variant);
	}

	@Test
	void caepInteropNamesTheRoleDeliveryGrantAndClientAuthenticationForADynamicRun() {
		assertEquals(List.of("OIDSSF-1.0-FINAL+CAEPIOP-1.0-DRAFT01 Transmitter push client_credentials client_secret_post"),
			OIDSSFCertification.caepInteropTransmitterProfileName(selection("dynamic", "push", "client_secret_post")));
		assertEquals(List.of("OIDSSF-1.0-FINAL+CAEPIOP-1.0-DRAFT01 Receiver poll client_credentials private_key_jwt"),
			OIDSSFCertification.caepInteropReceiverProfileName(selection("dynamic", "poll", "private_key_jwt")));
	}

	@Test
	void caepInteropGrantsNoProfileForAPreSharedToken() {
		assertTrue(OIDSSFCertification.caepInteropTransmitterProfileName(selection("static", "push", "client_secret_post")).isEmpty());
		assertTrue(OIDSSFCertification.caepInteropReceiverProfileName(selection("static", "push", "client_secret_post")).isEmpty());
	}

	@Test
	void caepInteropGrantsNoProfileWithoutClientAuthentication() {
		// RFC 6749 4.4: the client credentials grant is for confidential clients only
		assertTrue(OIDSSFCertification.caepInteropTransmitterProfileName(selection("dynamic", "push", "none")).isEmpty());
		assertTrue(OIDSSFCertification.caepInteropTransmitterProfileName(selection("dynamic", "push", null)).isEmpty());
	}
}
