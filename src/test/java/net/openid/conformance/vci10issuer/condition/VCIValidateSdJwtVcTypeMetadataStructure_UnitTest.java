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

@ExtendWith(MockitoExtension.class)
public class VCIValidateSdJwtVcTypeMetadataStructure_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIValidateSdJwtVcTypeMetadataStructure cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIValidateSdJwtVcTypeMetadataStructure();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putTypeMetadata(String json) {
		JsonObject vci = new JsonObject();
		vci.add("sdjwt_vc_type_metadata", JsonParser.parseString(json).getAsJsonObject());
		env.putObject("vci", vci);
	}

	@Test
	public void wellFormedMetadata_passes() {
		putTypeMetadata("""
			{
				"vct": "https://example.com/pid",
				"name": "Person Identification Data",
				"description": "PID credential",
				"display": [{ "locale": "en", "name": "Person Identification Data" }],
				"claims": [
					{
						"path": ["given_name"],
						"display": [{ "locale": "en", "label": "Given name" }],
						"mandatory": true,
						"sd": "allowed"
					}
				]
			}
			""");
		cond.execute(env);
	}

	@Test
	public void minimalMetadataWithVct_passes() {
		putTypeMetadata("{\"vct\": \"https://example.com/pid\"}");
		cond.execute(env);
	}

	@Test
	public void missingVct_fails() {
		// §6.2: vct is REQUIRED
		putTypeMetadata("{\"name\": \"PID\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void integritySiblings_pass() {
		putTypeMetadata("""
			{
				"vct": "https://example.com/pid",
				"extends": "https://example.com/base",
				"extends#integrity": "sha256-abc123"
			}
			""");
		cond.execute(env);
	}

	@Test
	public void unknownTopLevelPropertyDoesNotFailStructuralCheck() {
		// Unknowns are filtered here; CheckForUnexpectedParametersInSdJwtVcTypeMetadata
		// surfaces them at WARNING via the caller.
		putTypeMetadata("{\"vct\":\"https://example.com/x\",\"unknown_extension\":\"foo\"}");
		cond.execute(env);
	}

	@Test
	public void unknownSubordinatePropertyDoesNotFailStructuralCheck() {
		putTypeMetadata("""
			{
				"vct": "https://example.com/x",
				"claims": [ { "path": ["a"], "future_extension": true } ]
			}
			""");
		cond.execute(env);
	}

	@Test
	public void wrongTypeForName_fails() {
		// name MUST be a string per §6.2
		putTypeMetadata("{\"vct\":\"https://example.com/x\",\"name\": 42}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void claimsItemMissingPath_fails() {
		// §9: path is REQUIRED on each claims[] item
		putTypeMetadata("""
			{
				"vct": "https://example.com/x",
				"claims": [ { "mandatory": true } ]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void claimsPathItemWithWrongType_fails() {
		// §9.1: path elements MUST be string, null, or non-negative integer
		putTypeMetadata("""
			{
				"vct": "https://example.com/x",
				"claims": [ { "path": [ true ] } ]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void claimsPathNegativeInteger_fails() {
		// §9.1: integer path elements MUST be non-negative
		putTypeMetadata("""
			{
				"vct": "https://example.com/x",
				"claims": [ { "path": [ "a", -1 ] } ]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void claimsSdInvalidValue_fails() {
		// §9.4: sd MUST be always | allowed | never
		putTypeMetadata("""
			{
				"vct": "https://example.com/x",
				"claims": [ { "path": ["a"], "sd": "sometimes" } ]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void claimsSvgIdStartingWithDigit_fails() {
		// §9: svg_id MUST NOT start with a digit
		putTypeMetadata("""
			{
				"vct": "https://example.com/x",
				"claims": [ { "path": ["a"], "svg_id": "1foo" } ]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void claimsDisplayMissingLabel_fails() {
		// §9.2: label is REQUIRED in claim display
		putTypeMetadata("""
			{
				"vct": "https://example.com/x",
				"claims": [ { "path": ["a"], "display": [ { "locale": "en" } ] } ]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void claimsDisplayMissingLocale_fails() {
		// §9.2: locale is REQUIRED in claim display
		putTypeMetadata("""
			{
				"vct": "https://example.com/x",
				"claims": [ { "path": ["a"], "display": [ { "label": "A" } ] } ]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void typeDisplayMissingName_fails() {
		// §8: name is REQUIRED on each type-level display object
		putTypeMetadata("""
			{
				"vct": "https://example.com/x",
				"display": [ { "locale": "en" } ]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void typeDisplayMissingLocale_fails() {
		// §8: locale is REQUIRED on each type-level display object
		putTypeMetadata("""
			{
				"vct": "https://example.com/x",
				"display": [ { "name": "Person Identification Data" } ]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
