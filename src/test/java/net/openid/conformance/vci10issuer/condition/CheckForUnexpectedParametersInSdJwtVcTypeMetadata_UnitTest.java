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

/**
 * Confirms the WARNING-path companion to {@link VCIValidateSdJwtVcTypeMetadataStructure}:
 * this condition MUST throw (so the caller can surface a WARNING) on properties the
 * schema does not define, while ignoring purely structural problems (missing required
 * fields, wrong types) which are the structure condition's job.
 */
@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedParametersInSdJwtVcTypeMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckForUnexpectedParametersInSdJwtVcTypeMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckForUnexpectedParametersInSdJwtVcTypeMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
	}

	private void putTypeMetadata(String json) {
		JsonObject vci = new JsonObject();
		vci.add("sdjwt_vc_type_metadata", JsonParser.parseString(json).getAsJsonObject());
		env.putObject("vci", vci);
	}

	@Test
	public void cleanDocument_passes() {
		putTypeMetadata("""
			{
				"vct": "https://example.com/pid",
				"name": "PID",
				"claims": [ { "path": ["given_name"], "sd": "always" } ]
			}
			""");
		cond.execute(env);
	}

	@Test
	public void unknownTopLevelProperty_throws() {
		putTypeMetadata("{\"vct\":\"https://example.com/x\",\"unknown_extension\":\"foo\"}");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("Unknown properties"));
	}

	@Test
	public void unknownNestedClaimsProperty_throws() {
		// additionalProperties:false on each claims[] item flags the sibling of "path".
		putTypeMetadata("""
			{
				"vct": "https://example.com/x",
				"claims": [ { "path": ["a"], "future_extension": true } ]
			}
			""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("Unknown properties"));
	}

	@Test
	public void unknownNestedDisplayProperty_throws() {
		putTypeMetadata("""
			{
				"vct": "https://example.com/x",
				"display": [ { "locale": "en", "name": "PID", "bogus": 1 } ]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void structuralErrorButNoUnknownProperties_passes() {
		// 'vct' is REQUIRED (§6.2) and is missing here, but there are no *unknown*
		// properties — only known ones. This condition flags only unknowns, so it
		// must not throw; the missing-required failure is the structure condition's
		// responsibility.
		putTypeMetadata("{\"name\":\"PID\"}");
		cond.execute(env);
	}

	@Test
	public void wrongTypeButNoUnknownProperties_passes() {
		// 'name' has the wrong type (§6.2 requires a string) but is a known property;
		// no additionalProperties violation, so this condition passes.
		putTypeMetadata("{\"vct\":\"https://example.com/x\",\"name\":42}");
		cond.execute(env);
	}
}
