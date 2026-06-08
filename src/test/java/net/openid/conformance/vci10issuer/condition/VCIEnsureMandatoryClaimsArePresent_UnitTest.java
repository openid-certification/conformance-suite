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
	public void wildcardOverArrayAllElementsHaveClaim_passes() {
		// §9.1: null wildcard selects every element of the array; mandatory requires
		// every selected position to be populated.
		put(
			"{\"degrees\": [{\"type\": \"BSc\"}, {\"type\": \"MSc\"}]}",
			"{\"vct\":\"x\",\"claims\":[{\"path\":[\"degrees\",null,\"type\"],\"mandatory\":true}]}"
		);
		cond.execute(env);
	}

	@Test
	public void wildcardOverArrayOneElementMissing_fails() {
		// One element of the array does not have "type" — mandatory violated.
		put(
			"{\"degrees\": [{\"type\": \"BSc\"}, {\"other\": \"value\"}]}",
			"{\"vct\":\"x\",\"claims\":[{\"path\":[\"degrees\",null,\"type\"],\"mandatory\":true}]}"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void wildcardOverEmptyArray_fails() {
		// An empty array addresses zero claims; "no claim included" cannot satisfy mandatory.
		put(
			"{\"degrees\": []}",
			"{\"vct\":\"x\",\"claims\":[{\"path\":[\"degrees\",null,\"type\"],\"mandatory\":true}]}"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void integerIndexResolves_passes() {
		put(
			"{\"degrees\": [{\"type\": \"BSc\"}, {\"type\": \"MSc\"}]}",
			"{\"vct\":\"x\",\"claims\":[{\"path\":[\"degrees\",0,\"type\"],\"mandatory\":true}]}"
		);
		cond.execute(env);
	}

	@Test
	public void integerIndexOutOfBounds_fails() {
		put(
			"{\"degrees\": [{\"type\": \"BSc\"}]}",
			"{\"vct\":\"x\",\"claims\":[{\"path\":[\"degrees\",5,\"type\"],\"mandatory\":true}]}"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void terminalWildcardOnPresentNonEmptyArray_passes() {
		put(
			"{\"nationalities\": [\"GB\", \"DE\"]}",
			"{\"vct\":\"x\",\"claims\":[{\"path\":[\"nationalities\",null],\"mandatory\":true}]}"
		);
		cond.execute(env);
	}

	@Test
	public void terminalWildcardOnEmptyArray_fails() {
		put(
			"{\"nationalities\": []}",
			"{\"vct\":\"x\",\"claims\":[{\"path\":[\"nationalities\",null],\"mandatory\":true}]}"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
