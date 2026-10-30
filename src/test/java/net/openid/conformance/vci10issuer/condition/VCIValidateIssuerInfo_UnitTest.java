package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class VCIValidateIssuerInfo_UnitTest {

	private VCIValidateIssuerInfo cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIValidateIssuerInfo();
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
	}

	@Test
	public void acceptsTheSuitesIssuerInfo() {
		cond.execute(IssuerInfoTestFixtures.envWithIssuerInfo(IssuerInfoTestFixtures.suiteIssuerInfo()));
	}

	@Test
	public void rejectsAnEmptyArray() {
		assertThrows(ConditionError.class, () -> cond.execute(IssuerInfoTestFixtures.envWithIssuerInfo(new JsonArray())));
	}

	@Test
	public void rejectsAnElementWithoutData() {
		assertThrows(ConditionError.class, () -> cond.execute(env("""
			[ { "format": "registrar_dataset" } ]
			""")));
	}

	@Test
	public void rejectsARegistrarDatasetWhoseDataIsAString() {
		assertThrows(ConditionError.class, () -> cond.execute(env("""
			[ { "format": "registrar_dataset", "data": "eyJ..." } ]
			""")));
	}

	@Test
	public void rejectsARegistrationCertWhoseDataIsAnObject() {
		assertThrows(ConditionError.class, () -> cond.execute(env("""
			[ { "format": "registration_cert", "data": { "sub": "x" } } ]
			""")));
	}

	@Test
	public void ignoresUnknownMembers() {
		cond.execute(env("""
			[ { "format": "registrar_dataset", "data": { "identifier": [] }, "unknown": 1 } ]
			"""));
	}

	private static Environment env(String issuerInfo) {
		return IssuerInfoTestFixtures.envWithIssuerInfo(JsonParser.parseString(issuerInfo).getAsJsonArray());
	}
}
