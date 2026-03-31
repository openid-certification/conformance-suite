package net.openid.conformance.sharing;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SharedAsset_UnitTest {

	@Test
	void constructor_stores_all_fields_correctly() {
		Map<String, String> owner = new HashMap<>();
		owner.put("sub", "user1");
		owner.put("iss", "https://issuer.example.com");

		SharedAsset asset = new SharedAsset("token123", "plan456", "test789", owner, "https://example.com/plan-detail.html?plan=plan456");

		assertEquals("token123", asset.getTokenId());
		assertEquals("plan456", asset.getPlanId());
		assertEquals("test789", asset.getTestId());
		assertEquals("user1", asset.getOwner().get("sub"));
		assertEquals("https://issuer.example.com", asset.getOwner().get("iss"));
		assertEquals("https://example.com/plan-detail.html?plan=plan456", asset.getRedirectUri());
	}

	@Test
	void owner_map_is_unmodifiable() {
		Map<String, String> owner = new HashMap<>();
		owner.put("sub", "user1");
		owner.put("iss", "https://issuer.example.com");

		SharedAsset asset = new SharedAsset("token123", "plan456", "test789", owner, "https://example.com/redirect");

		assertThrows(UnsupportedOperationException.class, () -> asset.getOwner().put("extra", "value"));
	}

	@Test
	void null_testId_is_allowed() {
		Map<String, String> owner = Map.of("sub", "user1", "iss", "https://issuer.example.com");

		SharedAsset asset = new SharedAsset("token123", "plan456", null, owner, "https://example.com/redirect");

		assertNull(asset.getTestId());
		assertEquals("plan456", asset.getPlanId());
	}
}
