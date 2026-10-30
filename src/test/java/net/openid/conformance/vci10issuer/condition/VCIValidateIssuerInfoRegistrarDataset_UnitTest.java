package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class VCIValidateIssuerInfoRegistrarDataset_UnitTest {

	private static final String VALID = """
		{
		  "identifier": [ { "type": "http://data.europa.eu/eudi/id/LEI", "identifier": "529900T8BM49AURSDO55" } ],
		  "srvDescription": [ { "lang": "en", "content": "PID issuance" } ],
		  "registryURI": "https://registry.example.com",
		  "providesAttestations": [ { "format": "dc+sd-jwt", "meta": { "vct_values": ["urn:eudi:pid:1"] } } ]
		}
		""";

	private VCIValidateIssuerInfoRegistrarDataset cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIValidateIssuerInfoRegistrarDataset();
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
	}

	@Test
	public void acceptsAValidDataset() {
		cond.execute(env(dataset()));
	}

	@Test
	public void acceptsTheSuitesDataset() {
		Environment env = IssuerInfoTestFixtures.envWithIssuerInfo(IssuerInfoTestFixtures.suiteIssuerInfo());
		VCIEnsureIssuerInfoContainsOneRegistrarDataset extract = new VCIEnsureIssuerInfoContainsOneRegistrarDataset();
		extract.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
		extract.execute(env);
		cond.execute(env);
	}

	@Test
	public void rejectsEachMissingRequiredMember() {
		for (String member : new String[] {"identifier", "srvDescription", "registryURI", "providesAttestations"}) {
			JsonObject dataset = dataset();
			dataset.remove(member);
			assertThrows(ConditionError.class, () -> cond.execute(env(dataset)), member);
		}
	}

	@Test
	public void rejectsAMultiLangStringWithValueInsteadOfContent() {
		JsonObject dataset = dataset();
		dataset.add("srvDescription", JsonParser.parseString("[ { \"lang\": \"en\", \"value\": \"PID issuance\" } ]"));
		assertThrows(ConditionError.class, () -> cond.execute(env(dataset)));
	}

	@Test
	public void rejectsAnIdentifierString() {
		JsonObject dataset = dataset();
		dataset.addProperty("identifier", "LEIXG-529900T8BM49AURSDO55");
		assertThrows(ConditionError.class, () -> cond.execute(env(dataset)));
	}

	@Test
	public void rejectsAnAttestationWithoutMeta() {
		JsonObject dataset = dataset();
		dataset.add("providesAttestations", JsonParser.parseString("[ { \"format\": \"dc+sd-jwt\" } ]"));
		assertThrows(ConditionError.class, () -> cond.execute(env(dataset)));
	}

	private static JsonObject dataset() {
		return JsonParser.parseString(VALID).getAsJsonObject();
	}

	private static Environment env(JsonObject dataset) {
		Environment env = new Environment();
		env.putObject(VCIEnsureIssuerInfoContainsOneRegistrarDataset.ENV_KEY, dataset);
		return env;
	}
}
