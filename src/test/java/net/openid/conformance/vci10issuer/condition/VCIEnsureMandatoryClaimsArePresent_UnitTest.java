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
public class VCIEnsureMandatoryClaimsArePresent_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIEnsureMandatoryClaimsArePresent cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureMandatoryClaimsArePresent();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void put(String decoded, String typeMetadata) {
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("decoded", JsonParser.parseString(decoded).getAsJsonObject());
		env.putObject("sdjwt", sdjwt);
		JsonObject vci = new JsonObject();
		vci.add("sdjwt_vc_type_metadata", JsonParser.parseString(typeMetadata).getAsJsonObject());
		env.putObject("vci", vci);
	}

	@Test
	public void mandatoryClaimPresent_passes() {
		put(
			"{\"given_name\": \"Erika\"}",
			"{\"vct\": \"x\", \"claims\": [ { \"path\": [\"given_name\"], \"mandatory\": true } ] }"
		);
		cond.execute(env);
	}

	@Test
	public void mandatoryClaimAbsent_fails() {
		put(
			"{\"family_name\": \"Mustermann\"}",
			"{\"vct\": \"x\", \"claims\": [ { \"path\": [\"given_name\"], \"mandatory\": true } ] }"
		);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("§9.3"));
	}

	@Test
	public void mandatoryClaimNestedPresent_passes() {
		put(
			"{\"address\": {\"country\": \"DE\"}}",
			"{\"vct\":\"x\",\"claims\":[{\"path\":[\"address\",\"country\"],\"mandatory\":true}]}"
		);
		cond.execute(env);
	}

	@Test
	public void mandatoryClaimNestedAbsent_fails() {
		put(
			"{\"address\": {\"city\": \"Berlin\"}}",
			"{\"vct\":\"x\",\"claims\":[{\"path\":[\"address\",\"country\"],\"mandatory\":true}]}"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void nonMandatoryAbsent_passes() {
		// mandatory: false / omitted → claim being absent is fine.
		put(
			"{\"family_name\": \"Mustermann\"}",
			"{\"vct\":\"x\",\"claims\":[{\"path\":[\"given_name\"]},{\"path\":[\"middle_name\"],\"mandatory\":false}]}"
		);
		cond.execute(env);
	}

	@Test
	public void noClaimsArray_passes() {
		put("{\"given_name\": \"Erika\"}", "{\"vct\":\"x\"}");
		cond.execute(env);
	}

	@Test
	public void mandatoryPathWithNullElement_isSkippedNotFailed() {
		// Path traversal across array wildcard not implemented; skip rather than misjudge.
		put(
			"{\"degrees\": [{\"type\": \"BSc\"}]}",
			"{\"vct\":\"x\",\"claims\":[{\"path\":[\"degrees\",null,\"type\"],\"mandatory\":true}]}"
		);
		cond.execute(env);
	}

	@Test
	public void mandatoryPathWithIntegerElement_isSkippedNotFailed() {
		put(
			"{\"degrees\": [{\"type\": \"BSc\"}]}",
			"{\"vct\":\"x\",\"claims\":[{\"path\":[\"degrees\",0,\"type\"],\"mandatory\":true}]}"
		);
		cond.execute(env);
	}
}
