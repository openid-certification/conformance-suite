package net.openid.conformance.authzen.scenarios.discovery;

import net.openid.conformance.variant.VariantSelection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthzenPDPDiscoveryTestPlan_UnitTest {

	@Test
	public void certifiesTheDiscoveryLevel() {
		assertEquals(List.of("AuthZEN Discovery"),
			new AuthzenPDPDiscoveryTestPlan().certificationProfileName(new VariantSelection(Map.of())));
	}
}
