package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateClientJWKsPrivatePart_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateClientJWKsPrivatePart cond;

	private JsonObject client;

	@BeforeAll
	public static void addBouncyCastle() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateClientJWKsPrivatePart();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		client = new JsonObject();
	}

	@Test
	public void testEvaluate_RSAPrivateKeyNoError() {
		client.add("jwks", JsonParser.parseString("{" +
			"  \"keys\": [" +
			"    {" +
			"      \"kty\": \"RSA\"," +
			"      \"d\": \"ePRRljk3VGde-JOxwH5LMAY4dERXTLCs7x1NcbuVXF7dyggrbQpDMhCrdH2UJQyvsb4c7H6w9LvosC7UnDgcTzrdd-rccz02XtiTCjP4mXqDUv1qZkYoy-HUq0UH7-WdRJNnx9sZTmWox-yucUbtfdmVRVuI355vNfG3ldsl6kC3DRNKH8nLay0eiS1x62p5S00Fz7TY_hzRqgexsjKu_UNQYDpQcOBVR9qi_mp3Yp8MdsJ2DoBrFJnDTTrG9uNlos1n1kBw-MVQQIPyHKIcElS5b1D-uc53MyxmWUAhiSnf0gpYgk3bYHccAmi_vsRqajLTt0w6hbNozhFht94mq3aLjGQJjt78zSu42hrUqTR-FLHwSoIGilS9SbdYypyYku0ocinwVb51IraDIwJY0_b42aHxE5an9HNaephGvPYPq0jA0xXehLSHqtS9D49OlxcnAvRkyn3TnPbxrdPAaZ6sJEHC8jpdYgKZ9LM7QRAFxwIk3Bxrf4rcEoC_mk5f5in4JYlYmx7VfXSzmSTXLfAWbqpLYRgtaCTFZ-HBLU1VJiAr2nkzw6cewwpxUCrowADO_w0GYRIU3BWamY2i--9wbI2nRUlxaROSMYJx5-A4sWDNPKJWQpX1SUyBpgTO82tHicZ2wzVEqMA4-np_leFBXFvAaMSuIxJ-aQlkLyE\","
			+
			"      \"e\": \"AQAB\"," +
			"      \"use\": \"sig\"," +
			"      \"kid\": \"fapi-test-suite\"," +
			"      \"alg\": \"RS256\"," +
			"      \"n\": \"qVGMeRQ8KG9ZVQFXa2YxCiapfesToXILzIvmHqNOJ8EbVwZKcwZ1iHpAzaZhnrSHJBU7Fgz9PEeTAqY-2FrRLE2xz3pW_LCg3y4fb1Sfi5GBSl1es0e4UD4Ie0g5SfQeZIpHfGM9zZx8rmsiDLRUyBsB8kHyUG6Ul5pHpAUHhrtghCpzL5lvnVvCM3-apeSyOdquS09tjwGUPUWifrKr0X1xEoNm7rsChZXQbB-MwqJG-ZGw51udn5-3mMunqp-B37jXdo4tYYOg0kKrpZSqQdRPANmTbge3LIN7uYprFVmGDMvXwiLMF9Unkeudox6nuIy3MWSeFRWpcrSQ_7s5p4mSupZxWHWrN_qSM-H9mTosvPg32m7XjQvlEJhuXqn13VYd8ZhmYSqV7s_x8bveZJLBnNPAkjPDktz8eUsdX3S8X6MNoYh4Ch7rsE_FO21B3FcMwtFql11z3_3PCAQOB3lzggy1hs-4Zb1As84R1Fmy0WKHTPAKPlJtLNFKz_bVf7xRbrt8BlPbkF4HduQixi-twkWRkUz8_-zCzWhz6s9y5ltQgs0_rl9aYSAt5z8UarP1PNfQIYGBmhwqGHi2LmrYFRjBEHRIsDXJcUGWgkfKMsiECs-mT-6AIw2L5DZIMIo8aroEnVpS24Ln2fwpdrRjO-b65VW-5cFOM6gERCc\""
			+
			"    }" +
			"  ]" +
			"}").getAsJsonObject());
		env.putObject("client", client);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_encryptionKeyOnlyNoError() {
		// This happens when testing with client_secret_basic but using encrypted id_tokens
		client.add("jwks", JsonParser.parseString("""
				{
				            "keys": [
				                {
				                    "p": "0_PYObuWtoSKNPp4vKSbZUbVPxn0Pw5Z7r6SRfnYyE8mPaXvptdmQxGazBzt4kABfRHN1aUvnEH8aF2F3HlIP0938wyXWUN-0WvMDcdm1OZYW4bWCjom1JBS8IrvjCoaFzQsWzL-K-1xtTjIu8dSIFco4su9r3vn2YRlCqw-_A8",
				                    "kty": "RSA",
				                    "q": "qqUV6TYILpRiBmN28uqUFeUIEdbo1618EMIJyCRoRH6m2A-F1mXBchEALY8pyU8sLMYxzvfTtHM7JTA3zaR42v-sySvq2pkcwlvyVFHyGzfcc5yHhxxxFTcD48a5SCz0ah0Ua0DKUNXByRkpXCFjAxvWmIn2Oi48GQKRfBURz70",
				                    "d": "eUGjAoaBszhxfytcv7n6M-AaesYPGKplMxTLhI275rtnYgjzv1Cof2suhuZ2pmjqThI3HsAOu0VaHFEeEIQbB7KgwGyZxFU4gwo0aLwtl4arRVFb37J36FkDuAh5ehsNnZPfgPWfGDerRTjuyNBzb4yxWdf8j0N1j6rFN0dDggz31IOyDunGvgUPaSNdSWau65HJq0T4EajoRzhUGEIxMBvLjJehUtxIup0vuPqYsHuZPRXdOAURvHZHI-xNS1xVIxAvIr37OVFREWtuaJ5-cM4NlR9HuxfMU1cvrbkdr3fzzG5jJRmbt5WGe8dHfdaehwG9bLS8OaFpo7W7Hwf_eQ",
				                    "e": "AQAB",
				                    "use": "enc",
				                    "kid": "certification_client_13",
				                    "qi": "e3_GKMA5SqMC5N3QFdd_yCGj4MdwQiH7cjZKoSfHEqxzgBAfDe6tY6N7kfGwl9dRyAMPOA1RJU_IF-aI9UjbX7I5Rb7Ury-FjudQkCvORKBWaZLzv1kAbPBIYPZ3BZU3lOjlFOQaV22s_SBWwx-FnKuwlwHwBHDzvygnToQXVeU",
				                    "dp": "rbjrvUY4HZk8_nddhqEUjUoldvb67sQAimLA2YITYPseyOC9MO7T7pz7V9lOUWdM0QpKv5YJE_YxwAkHstHlmZ61Hg1v78YWp-fG9HQ_oLi8Kyi1PuSy9v7kPCxkc2n-wI4O3SFKmw6faH4GaYLRmZhW9q0v6CmSuwJ9HuZXKZk",
				                    "alg": "RSA1_5",
				                    "dq": "X3nWdJFiqo0i-2gTWX6eNFHc3f5ccLAERmwKhQy5ufkS6LhmrbppLaUNcHA1dQjzMmoB3EcVEFWYtgnwbwKjDAUZa8VCteQND0HaqArhZxEuKxFdUt869h-98Wdyq0tbxTNYBVpO2EOBFT8awQ19FHQy8U3fklXkEio5tC8ltBE",
				                    "n": "jUib5MowvBJatOuT5GBoDWjg4uos-H4Uu8xFpFBqVwcflqDqq0TwQdj3o7ikTs8r-aFmpq0KRPQRbiAnCjNKrFS2O11q2zvueCi0bdYAaIS67uKeCXM-N1btNlSUg2i63P5DAkIPACKt0fyazwBOWyoJLY-CtPiUm1kramPcO3wvUKOPhObYf4847tBerkHm5NkxmJDSxET8sdzQMMncdyEtuTlGQGz9ruxYjhiPiqDaNs7uZ2Y5Y5_8iz1ZZFJEEv1d7z75cSJvheD2rsgVETbkD7-gQAxksp1ZH124ZucfnX-G_-CTq5un8ZCsQswt2OwV-5KzgnzErWtIEE84Ew"
				                }
				            ]
				        }""").getAsJsonObject());
		env.putObject("client", client);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_encryptionKeyFirstAndSigningKeySecondNoError() {
		client.add("jwks", JsonParser.parseString("""
				{
				  "keys": [
				    {
				      "p": "0_PYObuWtoSKNPp4vKSbZUbVPxn0Pw5Z7r6SRfnYyE8mPaXvptdmQxGazBzt4kABfRHN1aUvnEH8aF2F3HlIP0938wyXWUN-0WvMDcdm1OZYW4bWCjom1JBS8IrvjCoaFzQsWzL-K-1xtTjIu8dSIFco4su9r3vn2YRlCqw-_A8",
				      "kty": "RSA",
				      "q": "qqUV6TYILpRiBmN28uqUFeUIEdbo1618EMIJyCRoRH6m2A-F1mXBchEALY8pyU8sLMYxzvfTtHM7JTA3zaR42v-sySvq2pkcwlvyVFHyGzfcc5yHhxxxFTcD48a5SCz0ah0Ua0DKUNXByRkpXCFjAxvWmIn2Oi48GQKRfBURz70",
				      "d": "eUGjAoaBszhxfytcv7n6M-AaesYPGKplMxTLhI275rtnYgjzv1Cof2suhuZ2pmjqThI3HsAOu0VaHFEeEIQbB7KgwGyZxFU4gwo0aLwtl4arRVFb37J36FkDuAh5ehsNnZPfgPWfGDerRTjuyNBzb4yxWdf8j0N1j6rFN0dDggz31IOyDunGvgUPaSNdSWau65HJq0T4EajoRzhUGEIxMBvLjJehUtxIup0vuPqYsHuZPRXdOAURvHZHI-xNS1xVIxAvIr37OVFREWtuaJ5-cM4NlR9HuxfMU1cvrbkdr3fzzG5jJRmbt5WGe8dHfdaehwG9bLS8OaFpo7W7Hwf_eQ",
				      "e": "AQAB",
				      "use": "enc",
				      "kid": "enc_key_first",
				      "qi": "e3_GKMA5SqMC5N3QFdd_yCGj4MdwQiH7cjZKoSfHEqxzgBAfDe6tY6N7kfGwl9dRyAMPOA1RJU_IF-aI9UjbX7I5Rb7Ury-FjudQkCvORKBWaZLzv1kAbPBIYPZ3BZU3lOjlFOQaV22s_SBWwx-FnKuwlwHwBHDzvygnToQXVeU",
				      "dp": "rbjrvUY4HZk8_nddhqEUjUoldvb67sQAimLA2YITYPseyOC9MO7T7pz7V9lOUWdM0QpKv5YJE_YxwAkHstHlmZ61Hg1v78YWp-fG9HQ_oLi8Kyi1PuSy9v7kPCxkc2n-wI4O3SFKmw6faH4GaYLRmZhW9q0v6CmSuwJ9HuZXKZk",
				      "alg": "RSA1_5",
				      "dq": "X3nWdJFiqo0i-2gTWX6eNFHc3f5ccLAERmwKhQy5ufkS6LhmrbppLaUNcHA1dQjzMmoB3EcVEFWYtgnwbwKjDAUZa8VCteQND0HaqArhZxEuKxFdUt869h-98Wdyq0tbxTNYBVpO2EOBFT8awQ19FHQy8U3fklXkEio5tC8ltBE",
				      "n": "jUib5MowvBJatOuT5GBoDWjg4uos-H4Uu8xFpFBqVwcflqDqq0TwQdj3o7ikTs8r-aFmpq0KRPQRbiAnCjNKrFS2O11q2zvueCi0bdYAaIS67uKeCXM-N1btNlSUg2i63P5DAkIPACKt0fyazwBOWyoJLY-CtPiUm1kramPcO3wvUKOPhObYf4847tBerkHm5NkxmJDSxET8sdzQMMncdyEtuTlGQGz9ruxYjhiPiqDaNs7uZ2Y5Y5_8iz1ZZFJEEv1d7z75cSJvheD2rsgVETbkD7-gQAxksp1ZH124ZucfnX-G_-CTq5un8ZCsQswt2OwV-5KzgnzErWtIEE84Ew"
				    },
				    {
				      "kty": "EC",
				      "d": "fRqbe7uuYBrzCLqC1Z2rxPIskrf3PrpbKAS5RdoRh_s",
				      "use": "sig",
				      "crv": "P-256",
				      "alg": "ES256",
				      "kid": "signing_key_second",
				      "x": "RsJ58leViXVAIvcR0jx7LfnALhm_0qcns3h4v6b8Pdk",
				      "y": "7Y0pNoArqzvFS_Li45WK3MfUf_YJaxWVVCbfEHPtdo0"
				    }
				  ]
				}""").getAsJsonObject());
		env.putObject("client", client);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_RSAPrivateKeyErrorKeysNotMatch() {
		assertThrows(ConditionError.class, () -> {
			client.add("jwks", JsonParser.parseString("{" +
				"  \"keys\": [" +
				"    {" +
				"      \"kty\": \"RSA\"," +
				"      \"d\": \"ePRRljk4VGde-JOxwH5LMAY4dERXTLCs7x1NcbuVXF7dyggrbQpDMhCrdH2UJQyvsb4c7H6w9LvosC7UnDgcTzrdd-rccz02XtiTCjP4mXqDUv1qZkYoy-HUq0UH7-WdRJNnx9sZTmWox-yucUbtfdmVRVuI355vNfG3ldsl6kC3DRNKH8nLay0eiS1x62p5S00Fz7TY_hzRqgexsjKu_UNQYDpQcOBVR9qi_mp3Yp8MdsJ2DoBrFJnDTTrG9uNlos1n1kBw-MVQQIPyHKIcElS5b1D-uc53MyxmWUAhiSnf0gpYgk3bYHccAmi_vsRqajLTt0w6hbNozhFht94mq3aLjGQJjt78zSu42hrUqTR-FLHwSoIGilS9SbdYypyYku0ocinwVb51IraDIwJY0_b42aHxE5an9HNaephGvPYPq0jA0xXehLSHqtS9D49OlxcnAvRkyn3TnPbxrdPAaZ6sJEHC8jpdYgKZ9LM7QRAFxwIk3Bxrf4rcEoC_mk5f5in4JYlYmx7VfXSzmSTXLfAWbqpLYRgtaCTFZ-HBLU1VJiAr2nkzw6cewwpxUCrowADO_w0GYRIU3BWamY2i--9wbI2nRUlxaROSMYJx5-A4sWDNPKJWQpX1SUyBpgTO82tHicZ2wzVEqMA4-np_leFBXFvAaMSuIxJ-aQlkLyE\","
				+
				"      \"e\": \"AQAB\"," +
				"      \"use\": \"sig\"," +
				"      \"kid\": \"fapi-test-suite\"," +
				"      \"alg\": \"RS256\"," +
				"      \"n\": \"qVGMeRQ8KG9ZVQFXa2YxCiapfesToXILzIvmHqNOJ8EbVwZKcwZ1iHpAzaZhnrSHJBU7Fgz9PEeTAqY-2FrRLE2xz3pW_LCg3y4fb1Sfi5GBSl1es0e4UD4Ie0g5SfQeZIpHfGM9zZx8rmsiDLRUyBsB8kHyUG6Ul5pHpAUHhrtghCpzL5lvnVvCM3-apeSyOdquS09tjwGUPUWifrKr0X1xEoNm7rsChZXQbB-MwqJG-ZGw51udn5-3mMunqp-B37jXdo4tYYOg0kKrpZSqQdRPANmTbge3LIN7uYprFVmGDMvXwiLMF9Unkeudox6nuIy3MWSeFRWpcrSQ_7s5p4mSupZxWHWrN_qSM-H9mTosvPg32m7XjQvlEJhuXqn13VYd8ZhmYSqV7s_x8bveZJLBnNPAkjPDktz8eUsdX3S8X6MNoYh4Ch7rsE_FO21B3FcMwtFql11z3_3PCAQOB3lzggy1hs-4Zb1As84R1Fmy0WKHTPAKPlJtLNFKz_bVf7xRbrt8BlPbkF4HduQixi-twkWRkUz8_-zCzWhz6s9y5ltQgs0_rl9aYSAt5z8UarP1PNfQIYGBmhwqGHi2LmrYFRjBEHRIsDXJcUGWgkfKMsiECs-mT-6AIw2L5DZIMIo8aroEnVpS24Ln2fwpdrRjO-b65VW-5cFOM6gERCc\""
				+
				"    }" +
				"  ]" +
				"}").getAsJsonObject());
			env.putObject("client", client);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_RSAPrivateKeyMissingD() {
		assertThrows(ConditionError.class, () -> {
			client.add("jwks", JsonParser.parseString("{" +
				"  \"keys\": [" +
				"    {" +
				"      \"kty\": \"RSA\"," +
				"      \"e\": \"AQAB\"," +
				"      \"use\": \"sig\"," +
				"      \"kid\": \"fapi-test-suite\"," +
				"      \"alg\": \"RS256\"," +
				"      \"n\": \"qVGMeRQ8KG9ZVQFXa2YxCiapfesToXILzIvmHqNOJ8EbVwZKcwZ1iHpAzaZhnrSHJBU7Fgz9PEeTAqY-2FrRLE2xz3pW_LCg3y4fb1Sfi5GBSl1es0e4UD4Ie0g5SfQeZIpHfGM9zZx8rmsiDLRUyBsB8kHyUG6Ul5pHpAUHhrtghCpzL5lvnVvCM3-apeSyOdquS09tjwGUPUWifrKr0X1xEoNm7rsChZXQbB-MwqJG-ZGw51udn5-3mMunqp-B37jXdo4tYYOg0kKrpZSqQdRPANmTbge3LIN7uYprFVmGDMvXwiLMF9Unkeudox6nuIy3MWSeFRWpcrSQ_7s5p4mSupZxWHWrN_qSM-H9mTosvPg32m7XjQvlEJhuXqn13VYd8ZhmYSqV7s_x8bveZJLBnNPAkjPDktz8eUsdX3S8X6MNoYh4Ch7rsE_FO21B3FcMwtFql11z3_3PCAQOB3lzggy1hs-4Zb1As84R1Fmy0WKHTPAKPlJtLNFKz_bVf7xRbrt8BlPbkF4HduQixi-twkWRkUz8_-zCzWhz6s9y5ltQgs0_rl9aYSAt5z8UarP1PNfQIYGBmhwqGHi2LmrYFRjBEHRIsDXJcUGWgkfKMsiECs-mT-6AIw2L5DZIMIo8aroEnVpS24Ln2fwpdrRjO-b65VW-5cFOM6gERCc\""
				+
				"    }" +
				"  ]" +
				"}").getAsJsonObject());
			env.putObject("client", client);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_DKeyNotBase64UrlEncodedError() {
		assertThrows(ConditionError.class, () -> {
			client.add("jwks", JsonParser.parseString("{" +
				"  \"keys\": [" +
				"    {" +
				"      \"kty\": \"RSA\"," +
				"      \"d\": \"ePRRljk4VGde+JOxwH5LMAY4dERXTLCs7x1NcbuVXF7dyggrbQpDMhCrdH2UJQyvsb4c7H6w9LvosC7UnDgcTzrdd-rccz02XtiTCjP4mXqDUv1qZkYoy-HUq0UH7-WdRJNnx9sZTmWox-yucUbtfdmVRVuI355vNfG3ldsl6kC3DRNKH8nLay0eiS1x62p5S00Fz7TY_hzRqgexsjKu_UNQYDpQcOBVR9qi_mp3Yp8MdsJ2DoBrFJnDTTrG9uNlos1n1kBw-MVQQIPyHKIcElS5b1D-uc53MyxmWUAhiSnf0gpYgk3bYHccAmi_vsRqajLTt0w6hbNozhFht94mq3aLjGQJjt78zSu42hrUqTR-FLHwSoIGilS9SbdYypyYku0ocinwVb51IraDIwJY0_b42aHxE5an9HNaephGvPYPq0jA0xXehLSHqtS9D49OlxcnAvRkyn3TnPbxrdPAaZ6sJEHC8jpdYgKZ9LM7QRAFxwIk3Bxrf4rcEoC_mk5f5in4JYlYmx7VfXSzmSTXLfAWbqpLYRgtaCTFZ-HBLU1VJiAr2nkzw6cewwpxUCrowADO_w0GYRIU3BWamY2i--9wbI2nRUlxaROSMYJx5-A4sWDNPKJWQpX1SUyBpgTO82tHicZ2wzVEqMA4-np_leFBXFvAaMSuIxJ-aQlkLyE\","
				+
				"      \"e\": \"AQAB\"," +
				"      \"use\": \"sig\"," +
				"      \"kid\": \"fapi-test-suite\"," +
				"      \"alg\": \"RS256\"," +
				"      \"n\": \"qVGMeRQ8KG9ZVQFXa2YxCiapfesToXILzIvmHqNOJ8EbVwZKcwZ1iHpAzaZhnrSHJBU7Fgz9PEeTAqY-2FrRLE2xz3pW_LCg3y4fb1Sfi5GBSl1es0e4UD4Ie0g5SfQeZIpHfGM9zZx8rmsiDLRUyBsB8kHyUG6Ul5pHpAUHhrtghCpzL5lvnVvCM3-apeSyOdquS09tjwGUPUWifrKr0X1xEoNm7rsChZXQbB-MwqJG-ZGw51udn5-3mMunqp-B37jXdo4tYYOg0kKrpZSqQdRPANmTbge3LIN7uYprFVmGDMvXwiLMF9Unkeudox6nuIy3MWSeFRWpcrSQ_7s5p4mSupZxWHWrN_qSM-H9mTosvPg32m7XjQvlEJhuXqn13VYd8ZhmYSqV7s_x8bveZJLBnNPAkjPDktz8eUsdX3S8X6MNoYh4Ch7rsE_FO21B3FcMwtFql11z3_3PCAQOB3lzggy1hs-4Zb1As84R1Fmy0WKHTPAKPlJtLNFKz_bVf7xRbrt8BlPbkF4HduQixi-twkWRkUz8_-zCzWhz6s9y5ltQgs0_rl9aYSAt5z8UarP1PNfQIYGBmhwqGHi2LmrYFRjBEHRIsDXJcUGWgkfKMsiECs-mT-6AIw2L5DZIMIo8aroEnVpS24Ln2fwpdrRjO-b65VW-5cFOM6gERCc\""
				+
				"    }" +
				"  ]" +
				"}").getAsJsonObject());
			env.putObject("client", client);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_ECKeyNoError() {
		client.add("jwks", JsonParser.parseString("{" +
			"  \"keys\": [" +
			"    {" +
			"      \"kty\": \"EC\"," +
			"      \"d\": \"fRqbe7uuYBrzCLqC1Z2rxPIskrf3PrpbKAS5RdoRh_s\"," +
			"      \"e\": \"AQAB\"," +
			"      \"use\": \"sig\"," +
			"      \"crv\":\"P-256\"," +
			"      \"alg\":\"ES256\"," +
			"      \"kid\": \"fapi-test-suite\"," +
			"      \"x\":\"RsJ58leViXVAIvcR0jx7LfnALhm_0qcns3h4v6b8Pdk\"," +
			"      \"y\":\"7Y0pNoArqzvFS_Li45WK3MfUf_YJaxWVVCbfEHPtdo0\"" +
			"    }" +
			"  ]" +
			"}").getAsJsonObject());

		env.putObject("client", client);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_ECKeyErrorKeysNotMatch() {
		assertThrows(ConditionError.class, () -> {
			client.add("jwks", JsonParser.parseString("{" +
				"  \"keys\": [" +
				"    {" +
				"      \"kty\": \"EC\"," +
				"      \"d\": \"fRqbe8uuYBrzCLqC1Z2rxPIskrf3PrpbKAS5RdoRh_s\"," +
				"      \"e\": \"AQAB\"," +
				"      \"use\": \"sig\"," +
				"      \"crv\":\"P-256\"," +
				"      \"alg\":\"ES256\"," +
				"      \"kid\": \"fapi-test-suite\"," +
				"      \"x\":\"RsJ58leViXVAIvcR0jx7LfnALhm_0qcns3h4v6b8Pdk\"," +
				"      \"y\":\"7Y0pNoArqzvFS_Li45WK3MfUf_YJaxWVVCbfEHPtdo0\"" +
				"    }" +
				"  ]" +
				"}").getAsJsonObject());

			env.putObject("client", client);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_Ed25519KeyWithX5cNoError() throws Exception {
		// Regression guard for https://gitlab.com/openid/conformance-suite/-/work_items/1096 (#1095):
		// an Ed25519 (OKP) client signing key whose JWK carries an x5c with the matching Ed25519
		// certificate must validate. The original failure was that the bundled BouncyCastle could not
		// parse the Ed25519 X.509 certificate referenced by x5c.
		client.add("jwks", ed25519JwksWithX5c());
		env.putObject("client", client);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_Ed25519KeyMismatchedX5cError() throws Exception {
		// The bare key ("x") must match the x5c certificate (RFC 7517 section 4.7). Nimbus enforces
		// this for RSA/EC but not OKP (OctetKeyPair.matches() is a stub), so the suite checks it itself;
		// an Ed25519 key whose x5c certificate is for a different key must be rejected.
		KeyPair jwkKeyPair = KeyPairGenerator.getInstance("Ed25519", "BC").generateKeyPair();
		KeyPair certKeyPair = KeyPairGenerator.getInstance("Ed25519", "BC").generateKeyPair();
		client.add("jwks", ed25519JwksWithX5c(jwkKeyPair, certKeyPair));
		env.putObject("client", client);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	/**
	 * Build a JWKS containing a single Ed25519 (OKP) signing key whose JWK includes an x5c holding a
	 * self-signed certificate over the same key, so the bare key provably matches the certificate.
	 */
	private static JsonObject ed25519JwksWithX5c() throws Exception {
		KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519", "BC").generateKeyPair();
		return ed25519JwksWithX5c(keyPair, keyPair);
	}

	/**
	 * Build a JWKS with a single Ed25519 (OKP) signing key whose bare key material comes from
	 * {@code jwkKeyPair} and whose x5c contains a self-signed certificate over {@code certKeyPair}.
	 * Passing two different key pairs produces a JWK whose bare key does not match its certificate.
	 */
	private static JsonObject ed25519JwksWithX5c(KeyPair jwkKeyPair, KeyPair certKeyPair) throws Exception {
		// Self-signed Ed25519 certificate over certKeyPair
		X500Name dn = new X500Name("CN=ed25519-x5c-test");
		Date notBefore = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
		Date notAfter = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));
		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
			dn, BigInteger.valueOf(System.nanoTime()), notBefore, notAfter, dn, certKeyPair.getPublic());
		ContentSigner signer = new JcaContentSignerBuilder("Ed25519").setProvider("BC").build(certKeyPair.getPrivate());
		X509CertificateHolder holder = builder.build(signer);
		X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);

		// Derive the bare OKP key material (x/d) from jwkKeyPair
		Ed25519PrivateKeyParameters priv = (Ed25519PrivateKeyParameters)
			PrivateKeyFactory.createKey(jwkKeyPair.getPrivate().getEncoded());
		OctetKeyPair okp = new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(priv.generatePublicKey().getEncoded()))
			.d(Base64URL.encode(priv.getEncoded()))
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.EdDSA)
			.keyID("ed25519-x5c-test")
			.x509CertChain(List.of(com.nimbusds.jose.util.Base64.encode(cert.getEncoded())))
			.build();

		JsonArray keys = new JsonArray();
		keys.add(JsonParser.parseString(okp.toJSONString()).getAsJsonObject());
		JsonObject jwks = new JsonObject();
		jwks.add("keys", keys);
		return jwks;
	}

	@Test
	public void testEvaluate_missingJWKs() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("client", client);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingKeys() {
		assertThrows(ConditionError.class, () -> {
			client.add("jwks", new JsonObject());
			env.putObject("client", client);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingE() {
		assertThrows(ConditionError.class, () -> {

			client.add("jwks", JsonParser.parseString("{" +
				"  \"keys\": [" +
				"    {" +
				"      \"kty\": \"RSA\"," +
				"      \"d\": \"ePRRljk3VGde-JOxwH5LMAY4dERXTLCs7x1NcbuVXF7dyggrbQpDMhCrdH2UJQyvsb4c7H6w9LvosC7UnDgcTzrdd-rccz02XtiTCjP4mXqDUv1qZkYoy-HUq0UH7-WdRJNnx9sZTmWox-yucUbtfdmVRVuI355vNfG3ldsl6kC3DRNKH8nLay0eiS1x62p5S00Fz7TY_hzRqgexsjKu_UNQYDpQcOBVR9qi_mp3Yp8MdsJ2DoBrFJnDTTrG9uNlos1n1kBw-MVQQIPyHKIcElS5b1D-uc53MyxmWUAhiSnf0gpYgk3bYHccAmi_vsRqajLTt0w6hbNozhFht94mq3aLjGQJjt78zSu42hrUqTR-FLHwSoIGilS9SbdYypyYku0ocinwVb51IraDIwJY0_b42aHxE5an9HNaephGvPYPq0jA0xXehLSHqtS9D49OlxcnAvRkyn3TnPbxrdPAaZ6sJEHC8jpdYgKZ9LM7QRAFxwIk3Bxrf4rcEoC_mk5f5in4JYlYmx7VfXSzmSTXLfAWbqpLYRgtaCTFZ-HBLU1VJiAr2nkzw6cewwpxUCrowADO_w0GYRIU3BWamY2i--9wbI2nRUlxaROSMYJx5-A4sWDNPKJWQpX1SUyBpgTO82tHicZ2wzVEqMA4-np_leFBXFvAaMSuIxJ-aQlkLyE\","
				+
				"      \"use\": \"sig\"," +
				"      \"kid\": \"fapi-test-suite\"," +
				"      \"alg\": \"RS256\"," +
				"      \"n\": \"qVGMeRQ8KG9ZVQFXa2YxCiapfesToXILzIvmHqNOJ8EbVwZKcwZ1iHpAzaZhnrSHJBU7Fgz9PEeTAqY-2FrRLE2xz3pW_LCg3y4fb1Sfi5GBSl1es0e4UD4Ie0g5SfQeZIpHfGM9zZx8rmsiDLRUyBsB8kHyUG6Ul5pHpAUHhrtghCpzL5lvnVvCM3-apeSyOdquS09tjwGUPUWifrKr0X1xEoNm7rsChZXQbB-MwqJG-ZGw51udn5-3mMunqp-B37jXdo4tYYOg0kKrpZSqQdRPANmTbge3LIN7uYprFVmGDMvXwiLMF9Unkeudox6nuIy3MWSeFRWpcrSQ_7s5p4mSupZxWHWrN_qSM-H9mTosvPg32m7XjQvlEJhuXqn13VYd8ZhmYSqV7s_x8bveZJLBnNPAkjPDktz8eUsdX3S8X6MNoYh4Ch7rsE_FO21B3FcMwtFql11z3_3PCAQOB3lzggy1hs-4Zb1As84R1Fmy0WKHTPAKPlJtLNFKz_bVf7xRbrt8BlPbkF4HduQixi-twkWRkUz8_-zCzWhz6s9y5ltQgs0_rl9aYSAt5z8UarP1PNfQIYGBmhwqGHi2LmrYFRjBEHRIsDXJcUGWgkfKMsiECs-mT-6AIw2L5DZIMIo8aroEnVpS24Ln2fwpdrRjO-b65VW-5cFOM6gERCc\""
				+
				"    }" +
				"  ]" +
				"}").getAsJsonObject());
			env.putObject("client", client);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingN() {
		assertThrows(ConditionError.class, () -> {

			client.add("jwks", JsonParser.parseString("{" +
				"  \"keys\": [" +
				"    {" +
				"      \"kty\": \"RSA\"," +
				"      \"d\": \"ePRRljk3VGde-JOxwH5LMAY4dERXTLCs7x1NcbuVXF7dyggrbQpDMhCrdH2UJQyvsb4c7H6w9LvosC7UnDgcTzrdd-rccz02XtiTCjP4mXqDUv1qZkYoy-HUq0UH7-WdRJNnx9sZTmWox-yucUbtfdmVRVuI355vNfG3ldsl6kC3DRNKH8nLay0eiS1x62p5S00Fz7TY_hzRqgexsjKu_UNQYDpQcOBVR9qi_mp3Yp8MdsJ2DoBrFJnDTTrG9uNlos1n1kBw-MVQQIPyHKIcElS5b1D-uc53MyxmWUAhiSnf0gpYgk3bYHccAmi_vsRqajLTt0w6hbNozhFht94mq3aLjGQJjt78zSu42hrUqTR-FLHwSoIGilS9SbdYypyYku0ocinwVb51IraDIwJY0_b42aHxE5an9HNaephGvPYPq0jA0xXehLSHqtS9D49OlxcnAvRkyn3TnPbxrdPAaZ6sJEHC8jpdYgKZ9LM7QRAFxwIk3Bxrf4rcEoC_mk5f5in4JYlYmx7VfXSzmSTXLfAWbqpLYRgtaCTFZ-HBLU1VJiAr2nkzw6cewwpxUCrowADO_w0GYRIU3BWamY2i--9wbI2nRUlxaROSMYJx5-A4sWDNPKJWQpX1SUyBpgTO82tHicZ2wzVEqMA4-np_leFBXFvAaMSuIxJ-aQlkLyE\","
				+
				"      \"e\": \"AQAB\"," +
				"      \"use\": \"sig\"," +
				"      \"kid\": \"fapi-test-suite\"," +
				"      \"alg\": \"RS256\"" +
				"    }" +
				"  ]" +
				"}").getAsJsonObject());
			env.putObject("client", client);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_twoKeys() {
		assertThrows(ConditionError.class, () -> {
			// a jwks with two valid private keys; we reject this as we need exactly one key, as otherwise we have no way
			// to know which key the users wants us to use
			client.add("jwks", JsonParser.parseString("{" +
				"  \"keys\": [" +
				"    {" +
				"      \"kty\": \"RSA\"," +
				"      \"e\": \"AQAB\"," +
				"      \"use\": \"sig\"," +
				"      \"kid\": \"fapi-test-suite\"," +
				"      \"alg\": \"RS256\"," +
				"      \"d\": \"ePRRljk3VGde-JOxwH5LMAY4dERXTLCs7x1NcbuVXF7dyggrbQpDMhCrdH2UJQyvsb4c7H6w9LvosC7UnDgcTzrdd-rccz02XtiTCjP4mXqDUv1qZkYoy-HUq0UH7-WdRJNnx9sZTmWox-yucUbtfdmVRVuI355vNfG3ldsl6kC3DRNKH8nLay0eiS1x62p5S00Fz7TY_hzRqgexsjKu_UNQYDpQcOBVR9qi_mp3Yp8MdsJ2DoBrFJnDTTrG9uNlos1n1kBw-MVQQIPyHKIcElS5b1D-uc53MyxmWUAhiSnf0gpYgk3bYHccAmi_vsRqajLTt0w6hbNozhFht94mq3aLjGQJjt78zSu42hrUqTR-FLHwSoIGilS9SbdYypyYku0ocinwVb51IraDIwJY0_b42aHxE5an9HNaephGvPYPq0jA0xXehLSHqtS9D49OlxcnAvRkyn3TnPbxrdPAaZ6sJEHC8jpdYgKZ9LM7QRAFxwIk3Bxrf4rcEoC_mk5f5in4JYlYmx7VfXSzmSTXLfAWbqpLYRgtaCTFZ-HBLU1VJiAr2nkzw6cewwpxUCrowADO_w0GYRIU3BWamY2i--9wbI2nRUlxaROSMYJx5-A4sWDNPKJWQpX1SUyBpgTO82tHicZ2wzVEqMA4-np_leFBXFvAaMSuIxJ-aQlkLyE\"," +
				"      \"n\": \"qVGMeRQ8KG9ZVQFXa2YxCiapfesToXILzIvmHqNOJ8EbVwZKcwZ1iHpAzaZhnrSHJBU7Fgz9PEeTAqY-2FrRLE2xz3pW_LCg3y4fb1Sfi5GBSl1es0e4UD4Ie0g5SfQeZIpHfGM9zZx8rmsiDLRUyBsB8kHyUG6Ul5pHpAUHhrtghCpzL5lvnVvCM3-apeSyOdquS09tjwGUPUWifrKr0X1xEoNm7rsChZXQbB-MwqJG-ZGw51udn5-3mMunqp-B37jXdo4tYYOg0kKrpZSqQdRPANmTbge3LIN7uYprFVmGDMvXwiLMF9Unkeudox6nuIy3MWSeFRWpcrSQ_7s5p4mSupZxWHWrN_qSM-H9mTosvPg32m7XjQvlEJhuXqn13VYd8ZhmYSqV7s_x8bveZJLBnNPAkjPDktz8eUsdX3S8X6MNoYh4Ch7rsE_FO21B3FcMwtFql11z3_3PCAQOB3lzggy1hs-4Zb1As84R1Fmy0WKHTPAKPlJtLNFKz_bVf7xRbrt8BlPbkF4HduQixi-twkWRkUz8_-zCzWhz6s9y5ltQgs0_rl9aYSAt5z8UarP1PNfQIYGBmhwqGHi2LmrYFRjBEHRIsDXJcUGWgkfKMsiECs-mT-6AIw2L5DZIMIo8aroEnVpS24Ln2fwpdrRjO-b65VW-5cFOM6gERCc\""
				+
				"    }," +
				"    {" +
				"      \"kty\": \"EC\"," +
				"      \"use\": \"sig\"," +
				"      \"crv\": \"P-256\"," +
				"      \"kid\": \"mrflibble\"," +
				"      \"d\": \"8SRXn85T6pRhrQ15qkJd-ZCmgzvRBpPhKNxv5U9W6Fk\"," +
				"      \"x\": \"rCEAb67rCh4INKUdzeYR-msNdWFq-gBS-AfKFGmNp9E\"," +
				"      \"y\": \"YmJrJEmyQSnFJThQT1l8JJgubRRoyX0l9A6LNO7LQo8\"," +
				"      \"alg\": \"ES256\"" +
				"    }" +
				"  ]" +
				"}").getAsJsonObject());
			env.putObject("client", client);

			cond.execute(env);
		});
	}
}
