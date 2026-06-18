package net.openid.conformance.condition.common;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.tls.ProtocolVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EnsureTLS13PreferredOverTLS12_UnitTest {

	private Environment env;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureTLS13PreferredOverTLS12 cond;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		cond = new EnsureTLS13PreferredOverTLS12();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testGetAllowedProtocolVersions_tls13PreferredOverTls12() {
		assertArrayEquals(new ProtocolVersion[]{ProtocolVersion.TLSv13, ProtocolVersion.TLSv12}, cond.getAllowedProtocolVersions());
	}

	@Test
	public void testNegotiatedProtocolVersions_tls13_noError() {
		env.putString("tls13_negotiated", "");

		cond.negotiatedProtocolVersions(env, ProtocolVersion.TLSv13);

		assertNull(env.getString("tls13_negotiated"));
	}

	@Test
	public void testNegotiatedProtocolVersions_tls12_throwsAndClearsMarker() {
		env.putString("tls13_negotiated", "");

		assertThrows(ConditionError.class, () -> cond.negotiatedProtocolVersions(env, ProtocolVersion.TLSv12));

		assertNull(env.getString("tls13_negotiated"));
	}
}
