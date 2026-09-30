package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.VP1FinalValidateVpFormatsSupportedInClientMetadata;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.BrainpoolSignatureProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DataItemExtensionsKt;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseLabel;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.AsymmetricKey;
import org.multipaz.crypto.Crypto;
import org.multipaz.crypto.EcCurve;
import org.multipaz.crypto.EcPrivateKey;
import org.multipaz.crypto.EcPrivateKeyJvmKt;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class AddVP1FinalIsoMdocClientMetadataWithAllSupportedAlgsToAuthorizationRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddVP1FinalIsoMdocClientMetadataWithAllSupportedAlgsToAuthorizationRequest cond;

	@BeforeAll
	public static void installCryptoProviders() {
		// as the running suite does at start up; multipaz needs it to verify brainpool signatures
		BrainpoolSignatureProvider.ensureInstalled();
	}

	@BeforeEach
	public void setUp() {
		cond = new AddVP1FinalIsoMdocClientMetadataWithAllSupportedAlgsToAuthorizationRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putObject("authorization_endpoint_request", new JsonObject());
	}

	private static final JsonArray SIGNATURE_ALGORITHMS = toJsonArray(
		AddVP1FinalIsoMdocClientMetadataWithAllSupportedAlgsToAuthorizationRequest.VERIFIABLE_SIGNATURE_ALGORITHMS);

	private static JsonArray toJsonArray(List<Integer> values) {
		JsonArray array = new JsonArray();
		values.forEach(array::add);
		return array;
	}

	private JsonObject addClientMetadata(String responseMode, String encryptionKeyCurve) {
		env.putString("response_mode", responseMode);
		env.putObjectFromJsonString("client_public_jwks", """
			{
				"keys": [
					{ "kty": "EC", "crv": "P-256", "use": "sig", "kid": "sig", "x": "x", "y": "y" },
					{ "kty": "EC", "crv": "%s", "use": "enc", "kid": "enc", "alg": "ECDH-ES", "x": "x", "y": "y" }
				]
			}
			""".formatted(encryptionKeyCurve));

		cond.execute(env);

		return env.getElementFromObject("authorization_endpoint_request",
			"client_metadata.vp_formats_supported.mso_mdoc").getAsJsonObject();
	}

	private static JsonArray signatureAlgorithmsAnd(int... others) {
		JsonArray expected = SIGNATURE_ALGORITHMS.deepCopy();
		for (int other : others) {
			expected.add(other);
		}
		return expected;
	}

	@ParameterizedTest
	@ValueSource(strings = {"direct_post", "dc_api"})
	public void testListsOnlySignatureAlgorithmsForAnUnencryptedResponse(String responseMode) {
		JsonObject msoMdoc = addClientMetadata(responseMode, "P-256");

		assertEquals(Set.of("issuerauth_alg_values", "deviceauth_alg_values"), msoMdoc.keySet());
		assertEquals(SIGNATURE_ALGORITHMS, msoMdoc.get("issuerauth_alg_values"));
		assertEquals(SIGNATURE_ALGORITHMS, msoMdoc.get("deviceauth_alg_values"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"direct_post.jwt", "dc_api.jwt"})
	public void testAlsoListsDeviceMacOverP256WhenTheResponseIsEncryptedToAP256Key(String responseMode) {
		JsonObject msoMdoc = addClientMetadata(responseMode, "P-256");

		assertEquals(SIGNATURE_ALGORITHMS, msoMdoc.get("issuerauth_alg_values"));
		// OID4VP 1.0 Final Appendix B.2.2 Table 2: HMAC 256/256 using ECDH with Curve P-256
		assertEquals(signatureAlgorithmsAnd(-65537), msoMdoc.get("deviceauth_alg_values"));
	}

	@Test
	public void testListsNoDeviceMacWhenTheEncryptionKeyIsNotOnP256() {
		// ParseCredentialAsMdoc can only use a P-256 key as the mdoc reader key
		JsonObject msoMdoc = addClientMetadata("direct_post.jwt", "P-384");

		assertEquals(SIGNATURE_ALGORITHMS, msoMdoc.get("deviceauth_alg_values"));
	}

	@Test
	public void testVerifierTestsAcceptTheClientMetadata() {
		addClientMetadata("direct_post.jwt", "P-256");

		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, env.getObject("authorization_endpoint_request"));
		env.putString("credential_format", "iso_mdl");
		VP1FinalValidateVpFormatsSupportedInClientMetadata verifierTestCheck = new VP1FinalValidateVpFormatsSupportedInClientMetadata();
		verifierTestCheck.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		assertDoesNotThrow(() -> verifierTestCheck.execute(env));
	}

	/**
	 * Each listed COSE algorithm identifier with every curve ISO/IEC 18013-5 9.1.2.4 pairs with
	 * that algorithm.
	 */
	static Stream<Arguments> listedAlgorithmsAndCurves() {
		return Stream.of(
			Arguments.of(-7, EcCurve.P256),
			Arguments.of(-7, EcCurve.BRAINPOOLP256R1),
			Arguments.of(-9, EcCurve.P256),
			Arguments.of(-35, EcCurve.P384),
			Arguments.of(-35, EcCurve.BRAINPOOLP320R1),
			Arguments.of(-35, EcCurve.BRAINPOOLP384R1),
			Arguments.of(-36, EcCurve.P521),
			Arguments.of(-36, EcCurve.BRAINPOOLP512R1),
			Arguments.of(-8, EcCurve.ED25519),
			Arguments.of(-8, EcCurve.ED448));
	}

	private static EcPrivateKey createKey(EcCurve curve) throws Exception {
		if (!curve.name().startsWith("BRAINPOOL")) {
			return BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
				(scope, continuation) -> Crypto.INSTANCE.createEcPrivateKey(curve, continuation));
		}
		// SunEC cannot generate brainpool keys, so generate them with BouncyCastle explicitly
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", BouncyCastleProviderSingleton.getInstance());
		generator.initialize(new ECGenParameterSpec(curve.getSECGName()));
		KeyPair pair = generator.generateKeyPair();
		return EcPrivateKeyJvmKt.toEcPrivateKey(pair.getPrivate(), pair.getPublic(), curve);
	}

	@ParameterizedTest
	@MethodSource("listedAlgorithmsAndCurves")
	public void testSuiteVerifiesSignatureCarryingListedAlgorithm(int coseAlg, EcCurve curve) throws Exception {
		EcPrivateKey key = createKey(curve);
		AsymmetricKey signingKey = AsymmetricKey.Companion.anonymous(key, curve.getDefaultSigningAlgorithmFullySpecified());
		Map<CoseLabel, DataItem> protectedHeaders = Map.of(
			new CoseNumberLabel(Cose.COSE_LABEL_ALG), DataItemExtensionsKt.toDataItem(coseAlg));
		CoseSign1 signature = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
			(scope, continuation) -> Cose.INSTANCE.coseSign1Sign(signingKey, "payload".getBytes(StandardCharsets.UTF_8), true,
				protectedHeaders, Map.of(), continuation));

		// the mdoc parsing resolves the algorithm from the COSE header in the same way
		Algorithm algorithm = Algorithm.Companion.fromCoseAlgorithmIdentifier(coseAlg);
		assertDoesNotThrow(() -> BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
			(scope, continuation) -> Cose.INSTANCE.coseSign1Check(key.getPublicKey(), null, signature, algorithm, continuation)));
	}

	@Test
	public void testEveryListedAlgorithmIsCoveredByTheVerificationTest() {
		Set<Integer> verified = listedAlgorithmsAndCurves()
			.map(arguments -> (Integer) arguments.get()[0])
			.collect(Collectors.toSet());
		List<Integer> listed =
			AddVP1FinalIsoMdocClientMetadataWithAllSupportedAlgsToAuthorizationRequest.VERIFIABLE_SIGNATURE_ALGORITHMS;
		assertEquals(verified, Set.copyOf(listed));
	}
}
