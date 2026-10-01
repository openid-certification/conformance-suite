package net.openid.conformance.condition.as;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VP1FinalValidateVpFormatsSupportedInClientMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VP1FinalValidateVpFormatsSupportedInClientMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new VP1FinalValidateVpFormatsSupportedInClientMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setClientMetadata(String json) {
		JsonObject request = new JsonObject();
		request.add("client_metadata", JsonParser.parseString(json));
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, request);
	}

	// === dc+sd-jwt tests (OID4VP 1.0 Final B.3.4) ===

	@Test
	public void testSdJwtVc_validWithAlgValues() {
		// B.3.4 example: both alg value arrays present
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"dc+sd-jwt": {
						"sd-jwt_alg_values": ["ES256", "ES384"],
						"kb-jwt_alg_values": ["ES256", "ES384"]
					}
				}
			}
			""");
		env.putString("credential_format", "sd_jwt_vc");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testSdJwtVc_validWithOnlySdJwtAlg() {
		// B.3.4: both parameters are OPTIONAL
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"dc+sd-jwt": {
						"sd-jwt_alg_values": ["ES256"]
					}
				}
			}
			""");
		env.putString("credential_format", "sd_jwt_vc");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testSdJwtVc_validWithOnlyKbJwtAlg() {
		// B.3.4: both parameters are OPTIONAL
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"dc+sd-jwt": {
						"kb-jwt_alg_values": ["ES256"]
					}
				}
			}
			""");
		env.putString("credential_format", "sd_jwt_vc");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testSdJwtVc_validEmptyObject() {
		// B.3.4: both parameters are OPTIONAL, so an empty object is valid
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"dc+sd-jwt": {}
				}
			}
			""");
		env.putString("credential_format", "sd_jwt_vc");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testSdJwtVc_missingDcSdJwtKey() {
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"mso_mdoc": {}
				}
			}
			""");
		env.putString("credential_format", "sd_jwt_vc");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testSdJwtVc_sdJwtAlgNotArray() {
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"dc+sd-jwt": {
						"sd-jwt_alg_values": "ES256"
					}
				}
			}
			""");
		env.putString("credential_format", "sd_jwt_vc");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testSdJwtVc_sdJwtAlgEmptyArray() {
		// spec says "non-empty array" when present
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"dc+sd-jwt": {
						"sd-jwt_alg_values": []
					}
				}
			}
			""");
		env.putString("credential_format", "sd_jwt_vc");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	// === mso_mdoc tests (OID4VP 1.0 Final B.2.2) ===

	@Test
	public void testMsoMdoc_validWithAlgValues() {
		// B.2.2: issuerauth_alg_values and deviceauth_alg_values are OPTIONAL
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"mso_mdoc": {
						"issuerauth_alg_values": [-7],
						"deviceauth_alg_values": [-7]
					}
				}
			}
			""");
		env.putString("credential_format", "iso_mdl");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testMsoMdoc_validEmptyObject() {
		// B.2.2: both parameters are OPTIONAL, so empty object is valid
		// (spec example shows "mso_mdoc": {})
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"mso_mdoc": {}
				}
			}
			""");
		env.putString("credential_format", "iso_mdl");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testMsoMdoc_validWithOnlyIssuerAuthAlg() {
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"mso_mdoc": {
						"issuerauth_alg_values": [-7, -9]
					}
				}
			}
			""");
		env.putString("credential_format", "iso_mdl");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testMsoMdoc_missingMsoMdocKey() {
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"dc+sd-jwt": {}
				}
			}
			""");
		env.putString("credential_format", "iso_mdl");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testMsoMdoc_issuerauthAlgNotArray() {
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"mso_mdoc": {
						"issuerauth_alg_values": -7
					}
				}
			}
			""");
		env.putString("credential_format", "iso_mdl");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testMsoMdoc_issuerauthAlgEmptyArray() {
		// spec says "non-empty array" when present
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"mso_mdoc": {
						"issuerauth_alg_values": []
					}
				}
			}
			""");
		env.putString("credential_format", "iso_mdl");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	// === Element type and value validation tests ===

	@Test
	public void testSdJwtVc_algValueNotString() {
		// sd-jwt_alg_values must contain strings, not integers
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"dc+sd-jwt": {
						"sd-jwt_alg_values": [256]
					}
				}
			}
			""");
		env.putString("credential_format", "sd_jwt_vc");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testSdJwtVc_kbJwtAlgValueNotString() {
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"dc+sd-jwt": {
						"kb-jwt_alg_values": [true]
					}
				}
			}
			""");
		env.putString("credential_format", "sd_jwt_vc");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testSdJwtVc_unrecognizedAlgValue() {
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"dc+sd-jwt": {
						"sd-jwt_alg_values": ["NONESUCH"]
					}
				}
			}
			""");
		env.putString("credential_format", "sd_jwt_vc");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testMsoMdoc_algValueNotInteger() {
		// COSE algorithm identifiers must be integers, not strings
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"mso_mdoc": {
						"issuerauth_alg_values": ["ES256"]
					}
				}
			}
			""");
		env.putString("credential_format", "iso_mdl");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testMsoMdoc_unrecognizedCoseAlg() {
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"mso_mdoc": {
						"issuerauth_alg_values": [99999]
					}
				}
			}
			""");
		env.putString("credential_format", "iso_mdl");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testMsoMdoc_validCoseAlgES256() {
		// COSE alg -7 = ES256
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"mso_mdoc": {
						"issuerauth_alg_values": [-7]
					}
				}
			}
			""");
		env.putString("credential_format", "iso_mdl");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testMsoMdoc_validCoseAlgES512() {
		// COSE alg -36 = ES512
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"mso_mdoc": {
						"deviceauth_alg_values": [-36]
					}
				}
			}
			""");
		env.putString("credential_format", "iso_mdl");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testMsoMdoc_validCoseAlgEd25519() {
		// COSE alg -19 = Ed25519 (RFC 9864)
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"mso_mdoc": {
						"issuerauth_alg_values": [-19]
					}
				}
			}
			""");
		env.putString("credential_format", "iso_mdl");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	private void setMsoMdocAlgValues(String parameter, String values) {
		setClientMetadata("""
			{
				"vp_formats_supported": {
					"mso_mdoc": {
						"%s": %s
					}
				}
			}
			""".formatted(parameter, values));
		env.putString("credential_format", "iso_mdl");
	}

	@ParameterizedTest
	@ValueSource(ints = {-65537, -65538, -65539, -65540, -65541, -65542, -65543, -65544, -65545})
	public void testMsoMdoc_deviceAuthAcceptsDeviceMacCurveIdentifiers(int alg) {
		// B.2.2 Table 2: HMAC 256/256 using ECDH, one identifier per device key curve
		setMsoMdocAlgValues("deviceauth_alg_values", "[" + alg + "]");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testMsoMdoc_deviceAuthAcceptsDeviceMacAlongsideSignature() {
		// B.2.2 example: DeviceMac over P-256 and DeviceSignature with ESP256
		setMsoMdocAlgValues("deviceauth_alg_values", "[-65537, -9]");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testMsoMdoc_deviceAuthAcceptsHmac256() {
		// B.2.2 case 1: the value matches the alg of the DeviceMac COSE header, which
		// ISO/IEC 18013-5 9.1.3.5 fixes at HMAC 256/256 (COSE alg 5)
		setMsoMdocAlgValues("deviceauth_alg_values", "[5]");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@ParameterizedTest
	@ValueSource(ints = {-65536, -65546})
	public void testMsoMdoc_deviceAuthRejectsIdentifiersOutsideDeviceMacTable(int alg) {
		setMsoMdocAlgValues("deviceauth_alg_values", "[" + alg + "]");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testMsoMdoc_deviceAuthRejectsOtherMacAlgorithms() {
		// COSE alg 6 = HMAC 384/384, which a DeviceMac can never use
		setMsoMdocAlgValues("deviceauth_alg_values", "[6]");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@ParameterizedTest
	@ValueSource(ints = {-65537, 5})
	public void testMsoMdoc_issuerAuthRejectsDeviceMacIdentifiers(int alg) {
		// IssuerAuth is a COSE_Sign1, so only signature algorithms apply
		setMsoMdocAlgValues("issuerauth_alg_values", "[" + alg + "]");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	// === Common tests ===

	@Test
	public void testMissingVpFormatsSupported() {
		setClientMetadata("""
			{
				"jwks": {}
			}
			""");
		env.putString("credential_format", "sd_jwt_vc");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testVpFormatsSupportedNotObject() {
		setClientMetadata("""
			{
				"vp_formats_supported": "invalid"
			}
			""");
		env.putString("credential_format", "sd_jwt_vc");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testMissingClientMetadata() {
		JsonObject request = new JsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, request);
		env.putString("credential_format", "sd_jwt_vc");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
