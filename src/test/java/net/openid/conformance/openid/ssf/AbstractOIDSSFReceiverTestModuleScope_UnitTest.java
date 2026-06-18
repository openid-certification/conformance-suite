package net.openid.conformance.openid.ssf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the endpoint-to-required-scope mapping used by the emulated resource
 * server to reject access with insufficient scopes (CAEP Interop Profile §2.7.2
 * / §2.7.3): read/status operations require {@code ssf.read}, stream-management
 * operations require {@code ssf.manage}.
 */
public class AbstractOIDSSFReceiverTestModuleScope_UnitTest {

	/** Minimal concrete subclass so the protected mapping can be exercised. */
	static class TestModule extends AbstractOIDSSFReceiverTestModule {
		@Override
		protected boolean isFinished() {
			return false;
		}
	}

	private final TestModule module = new TestModule();

	private String requiredScope(String path, String method) {
		return module.requiredScopeForOperation(path, method);
	}

	@Test
	void readOperationsRequireSsfRead() {
		assertEquals(SsfConstants.SCOPE_SSF_READ, requiredScope("streams", "GET"));
		assertEquals(SsfConstants.SCOPE_SSF_READ, requiredScope("status", "GET"));
		assertEquals(SsfConstants.SCOPE_SSF_READ, requiredScope("events", "POST"));
	}

	@Test
	void managementOperationsRequireSsfManage() {
		assertEquals(SsfConstants.SCOPE_SSF_MANAGE, requiredScope("streams", "POST"));
		assertEquals(SsfConstants.SCOPE_SSF_MANAGE, requiredScope("streams", "PUT"));
		assertEquals(SsfConstants.SCOPE_SSF_MANAGE, requiredScope("streams", "PATCH"));
		assertEquals(SsfConstants.SCOPE_SSF_MANAGE, requiredScope("streams", "DELETE"));
		assertEquals(SsfConstants.SCOPE_SSF_MANAGE, requiredScope("status", "POST"));
		assertEquals(SsfConstants.SCOPE_SSF_MANAGE, requiredScope("verify", "POST"));
		assertEquals(SsfConstants.SCOPE_SSF_MANAGE, requiredScope("add_subject", "POST"));
		assertEquals(SsfConstants.SCOPE_SSF_MANAGE, requiredScope("remove_subject", "POST"));
	}

	@Test
	void nonScopeGatedPathsReturnNull() {
		assertNull(requiredScope("ssf-configuration", "GET"));
		assertNull(requiredScope("jwks", "GET"));
		assertNull(requiredScope("token", "POST"));
	}
}
