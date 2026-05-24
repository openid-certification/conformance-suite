package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
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
public class VCIEnsureSelectiveDisclosureConformsToTypeMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIEnsureSelectiveDisclosureConformsToTypeMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureSelectiveDisclosureConformsToTypeMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void put(String[] disclosures, String typeMetadataClaimsJson) {
		JsonObject sdjwt = new JsonObject();
		JsonArray arr = new JsonArray();
		for (String d : disclosures) {
			arr.add(d);
		}
		sdjwt.add("disclosures", arr);
		env.putObject("sdjwt", sdjwt);

		JsonObject vci = new JsonObject();
		JsonObject tm = new JsonObject();
		tm.add("claims", JsonParser.parseString(typeMetadataClaimsJson).getAsJsonArray());
		vci.add("sdjwt_vc_type_metadata", tm);
		env.putObject("vci", vci);
	}

	@Test
	public void alwaysWithMatchingDisclosure_passes() {
		put(
			new String[]{ "[\"salt\",\"given_name\",\"Erika\"]" },
			"[ { \"path\": [\"given_name\"], \"sd\": \"always\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void alwaysWithoutMatchingDisclosure_fails() {
		put(
			new String[]{ "[\"salt\",\"family_name\",\"Mustermann\"]" },
			"[ { \"path\": [\"given_name\"], \"sd\": \"always\" } ]"
		);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("§9.4"));
	}

	@Test
	public void neverWithoutMatchingDisclosure_passes() {
		put(
			new String[]{ "[\"salt\",\"family_name\",\"Mustermann\"]" },
			"[ { \"path\": [\"given_name\"], \"sd\": \"never\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void neverWithMatchingDisclosure_fails() {
		put(
			new String[]{ "[\"salt\",\"given_name\",\"Erika\"]" },
			"[ { \"path\": [\"given_name\"], \"sd\": \"never\" } ]"
		);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void allowedHasNoConstraint_passes() {
		put(
			new String[]{ "[\"salt\",\"given_name\",\"Erika\"]" },
			"[ { \"path\": [\"given_name\"], \"sd\": \"allowed\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void omittedSdHasNoConstraint_passes() {
		put(
			new String[]{ "[\"salt\",\"given_name\",\"Erika\"]" },
			"[ { \"path\": [\"given_name\"] } ]"
		);
		cond.execute(env);
	}

	@Test
	public void multiSegmentPathIsSkippedNotFailed() {
		put(
			new String[]{ "[\"salt\",\"country\",\"DE\"]" },
			"[ { \"path\": [\"address\",\"country\"], \"sd\": \"never\" } ]"
		);
		cond.execute(env);
	}

	@Test
	public void noClaimsArray_passes() {
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("disclosures", new JsonArray());
		env.putObject("sdjwt", sdjwt);
		JsonObject vci = new JsonObject();
		vci.add("sdjwt_vc_type_metadata", new JsonObject());
		env.putObject("vci", vci);
		cond.execute(env);
	}

	@Test
	public void twoElementDisclosuresIgnored() {
		// [salt, value] disclosures (array elements) aren't keyed by name.
		put(
			new String[]{ "[\"salt\",\"Erika\"]" },
			"[ { \"path\": [\"given_name\"], \"sd\": \"always\" } ]"
		);
		// sd: always on given_name fails because the 2-element disclosure
		// can't be matched by claim name.
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
