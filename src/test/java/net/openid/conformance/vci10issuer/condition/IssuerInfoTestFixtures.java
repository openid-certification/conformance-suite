package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.ECKey;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;
import net.openid.conformance.util.SuiteWrprc;
import net.openid.conformance.util.TestKeysAndCerts;

/** The suite's own issuer_info and signed issuer metadata, as the issuer-side checks receive them. */
final class IssuerInfoTestFixtures {

	static final String PID_VCT = "urn:eudi:pid:1";
	static final String PID_DOCTYPE = "eu.europa.ec.eudi.pid.1";

	private IssuerInfoTestFixtures() {
	}

	/** SD-JWT VC and mdoc PID, as TS 119 475 Credential objects. */
	static JsonArray providesAttestations() {
		return JsonParser.parseString("""
			[
			  { "format": "dc+sd-jwt", "meta": { "vct_values": ["%s"] } },
			  { "format": "mso_mdoc", "meta": { "doctype_value": "%s" } }
			]
			""".formatted(PID_VCT, PID_DOCTYPE)).getAsJsonArray();
	}

	static JsonArray suiteIssuerInfo() {
		return SuiteWrprc.issuerInfo(providesAttestations());
	}

	/** A credential_configurations_supported advertising the SD-JWT VC and mdoc PID. */
	static JsonObject credentialConfigurations() {
		return JsonParser.parseString("""
			{
			  "pid_sd_jwt": { "format": "dc+sd-jwt", "vct": "%s" },
			  "pid_mdoc": { "format": "mso_mdoc", "doctype": "%s" }
			}
			""".formatted(PID_VCT, PID_DOCTYPE)).getAsJsonObject();
	}

	/**
	 * An environment as {@code VCIDecodeSignedCredentialIssuerMetadata} leaves it for metadata carrying
	 * {@code issuerInfo}, signed with the suite's access certificate.
	 */
	static Environment envWithSignedMetadata(JsonElement issuerInfo) {
		JsonObject payload = new JsonObject();
		payload.addProperty("credential_issuer", "https://issuer.example.com/");
		payload.add("credential_configurations_supported", credentialConfigurations());
		if (issuerInfo != null) {
			payload.add("issuer_info", issuerInfo);
		}

		ECKey key = TestKeysAndCerts.getWrpac().getJwk();
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(new JOSEObjectType("openidvci-issuer-metadata+jwt"))
			.x509CertChain(key.getX509CertChain())
			.build();
		String jws = JWTUtil.signCompact(header, payload, key);

		Environment env = new Environment();
		JsonObject response = new JsonObject();
		response.addProperty("body", jws);
		env.putObject("credential_issuer_metadata_endpoint_response", response);
		JsonObject vci = new JsonObject();
		vci.add("credential_issuer_metadata_signed", payload.deepCopy());
		vci.add("credential_issuer_metadata", payload.deepCopy());
		env.putObject("vci", vci);
		return env;
	}

	/** {@link #envWithSignedMetadata} with issuer_info extracted. */
	static Environment envWithIssuerInfo(JsonArray issuerInfo) {
		Environment env = envWithSignedMetadata(issuerInfo);
		JsonObject wrapper = new JsonObject();
		wrapper.add("issuer_info", issuerInfo);
		env.putObject(VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata.ENV_KEY, wrapper);
		return env;
	}

	/** An issuer_info whose registration certificate has {@code payload}, signed by the suite's WRPRC signer. */
	static JsonArray issuerInfoWithRegistrationCertificatePayload(JsonObject payload) {
		ECKey key = TestKeysAndCerts.getWrprcSigner().getJwk();
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(new JOSEObjectType("rc-wrp+jwt"))
			.x509CertChain(key.getX509CertChain())
			.build();
		JsonArray issuerInfo = suiteIssuerInfo();
		for (JsonElement element : issuerInfo) {
			JsonObject obj = element.getAsJsonObject();
			if ("registration_cert".equals(OIDFJSON.getString(obj.get("format")))) {
				obj.addProperty("data", JWTUtil.signCompact(header, payload, key));
			}
		}
		return issuerInfo;
	}
}
