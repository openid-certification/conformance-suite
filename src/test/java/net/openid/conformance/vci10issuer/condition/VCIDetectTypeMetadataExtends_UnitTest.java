package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCIDetectTypeMetadataExtends_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIDetectTypeMetadataExtends cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIDetectTypeMetadataExtends();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putTypeMetadata(String json) {
		JsonObject vci = new JsonObject();
		vci.add("sdjwt_vc_type_metadata", JsonParser.parseString(json).getAsJsonObject());
		env.putObject("vci", vci);
	}

	@Test
	public void noExtends_passes() {
		putTypeMetadata("{\"vct\":\"https://example.com/pid\",\"name\":\"PID\"}");
		cond.execute(env);
	}

	@Test
	public void extendsPresent_throwsSoCallerCanWarn() {
		// Wiring at the caller binds onFail to WARNING — the thrown error is
		// surfaced as a warning rather than a failure. mandatory/sd checks
		// downstream still run and apply to the child's declared claims.
		putTypeMetadata("{\"vct\":\"https://example.com/pid\",\"extends\":\"https://example.com/base\"}");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("extends"));
		assertTrue(e.getMessage().contains("§9.5"));
	}
}
