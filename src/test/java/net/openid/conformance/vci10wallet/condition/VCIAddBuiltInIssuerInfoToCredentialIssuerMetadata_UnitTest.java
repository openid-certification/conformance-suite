package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VCIAddBuiltInIssuerInfoToCredentialIssuerMetadata_UnitTest {

	private VCIAddBuiltInIssuerInfoToCredentialIssuerMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIAddBuiltInIssuerInfoToCredentialIssuerMetadata();
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
	}

	@Test
	public void addsIssuerInfoWhenTheBuiltInAccessCertificateSigns() {
		Environment env = env(new JsonObject());
		cond.execute(env);

		JsonArray issuerInfo = env.getObject("credential_issuer_metadata").getAsJsonArray("issuer_info");
		assertThat(issuerInfo).hasSize(2);
		assertThat(issuerInfo.get(0).getAsJsonObject().get("format").toString()).isEqualTo("\"registrar_dataset\"");
		assertThat(issuerInfo.get(1).getAsJsonObject().get("format").toString()).isEqualTo("\"registration_cert\"");
		assertThat(issuerInfo.get(0).getAsJsonObject().getAsJsonObject("data").get("providesAttestations")).isEqualTo(JsonParser.parseString("""
			[
			  { "format": "dc+sd-jwt", "meta": { "vct_values": ["urn:eudi:pid:1"] } },
			  { "format": "mso_mdoc", "meta": { "doctype_value": "eu.europa.ec.eudi.pid.1" } }
			]
			"""));
	}

	@Test
	public void addsNothingWhenATesterKeySigns() throws Exception {
		JsonObject config = new JsonObject();
		JsonObject credential = new JsonObject();
		credential.add("signing_jwk", JsonParser.parseString(new ECKeyGenerator(Curve.P_256).keyID("tester").generate().toJSONString()));
		config.add("credential", credential);
		Environment env = env(config);
		cond.execute(env);
		assertThat(env.getObject("credential_issuer_metadata").has("issuer_info")).isFalse();
	}

	@Test
	public void registersEachFormatAndTypeOnce() {
		JsonArray provides = VCIAddBuiltInIssuerInfoToCredentialIssuerMetadata.providesAttestations(JsonParser.parseString("""
			{
			  "a": { "format": "dc+sd-jwt", "vct": "urn:eudi:pid:1" },
			  "b": { "format": "dc+sd-jwt", "vct": "urn:eudi:pid:1", "scope": "pid2" },
			  "c": { "format": "jwt_vc_json" }
			}
			""").getAsJsonObject());
		assertThat(provides).hasSize(1);
	}

	private static Environment env(JsonObject config) {
		Environment env = new Environment();
		env.putObject("config", config);
		env.putObject("credential_issuer_metadata", JsonParser.parseString("""
			{
			  "credential_issuer": "https://issuer.example.com/",
			  "credential_configurations_supported": {
			    "pid_sd_jwt": { "format": "dc+sd-jwt", "vct": "urn:eudi:pid:1" },
			    "pid_mdoc": { "format": "mso_mdoc", "doctype": "eu.europa.ec.eudi.pid.1" }
			  }
			}
			""").getAsJsonObject());
		return env;
	}
}
