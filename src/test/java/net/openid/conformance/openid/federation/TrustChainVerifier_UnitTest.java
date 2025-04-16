package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.produce.JWSSignerFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.extensions.MultiJWSSignerFactory;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes.RP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class TrustChainVerifier_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private TrustChainVerifier validator;

	@BeforeEach
	void setUp() {
	}

	static String LEAF = "https://leaf.com";
	static String INTERMEDIATE = "https://intermediate.com";
	static String TRUST_ANCHOR = "https://trust-anchor.com";

	static String LEAF_KEY = """
		{
		    "p": "7mJGUYWA71G1dDOOMrG7gYOw52n2zF3VzW4VfNdIDMTpqrMO8WWTvjFw4-hM3eS8XrFKJVDmCpSAsSav1nZlz3itvcXkFpUhpod8G_0rVo8JSesXq9paonr8WEESfI5sgibh1E0wgPWvP5yH7GJOKsXocn0VyRN4FzYnI7uGWOs",
		    "kty": "RSA",
		    "q": "4FRouq6_ecsXgv2zSSItzsrxtAsf3MfSTlglMFNx5ups5ahfrjt99jpcG4lI2jsZtIkM9FK5AF3gKgTovqIak0vmwwltib3Y8WbNtpqpsprLKupwAawgkj7-ab2w-Qme2SFbd6Ms7vZBrZDUR8QHW-nqX_bOgqURo6tCTxDRMP0",
		    "d": "BC2-dRCXVwj6EJxu5KbG14HkGp28ICcaOOp3pFqHELYJhRfziffsL_qpVeNQcfeMpRvJjPqL1qxCF2MSttJHmanIw8mpLCbyguaGvCHACVpjD1Al9vTlrXtpxq5GkZYkBqZ-YuAP78wiLvU50QtXq6tGCRbV_HY0N4NkXCIODKa8lYe-3FnRUyoDYgmFqMYyO_g4PjG4xgN-lJDDKCjB3sMYccGmpGs7kkSyzrlhQoNtq5JxdIBzwnO6tYCmXCS6wboo8l85BZom7owi3ANswT6Oy6veu6HpqfBmOUKITXBIfQed3bf5mf4Kj7J_E_Ixc_jysMokFW02HVxIrQ4cqQ",
		    "e": "AQAB",
		    "use": "sig",
		    "kid": "leaf-key",
		    "qi": "Y2Oqi_WKWysHE4yCNZWpzq0zXIFNy0pp3o8J9MV3cJMClFpPhPRGqEbd77ow4oQ_gRrskontCRnzFzf32525ukjRqlgwZXGRsAlpfxEevh3O7E6ztuBOOMqqJeAxWOpcontGxrCjVffd_xyqFyaFS0UBt5Yw7NaDt7vh7XmO5Ok",
		    "dp": "ropUNEJM2m14L4HEHwgVY0n6ECZ85rvZ3JU6tHKoNXlEIZEDYupd18c5ghXHmjxtU-P5hwMJvDtpUswhuSRtfRA-HhIz4_kDb3wJ-jBPOAx43597cH_rFsZ312Kl138socs4VIrD9dhtnWTN_N0poJXRkWpWtihEKLKky7v1LUs",
		    "alg": "RS256",
		    "dq": "229WHr6PSGj0b8sRVEmdu3njYvW6zjGcs8wfXJ1Tfsjzitbf-UPynChMLxdkbyx-oY2qYViF5Sju_MtejBNvoYQOj9wqDSHrE4A3XBcARliKwkkfFVO1bLk4DmFiusjBXK33lhjE_F9gsZJJRIYpmKxA-mwnZ75mec4th7_wkVU",
		    "n": "0OSXTCpB7bTXG6FkYr4YRODgRRgpxBitZ27T1XbOcqTaiJYXarU94bQZ4g4f4lFaJfeJbMjl4GKQ0la4UKVICzSchgnzd1V2f-jjCH3JiiOeslKnXhfyZ7TfBIBXdFMcFATjkACMkUuQ6KW9qSKz2L1a3ytdZcXxGqz8MnX6q5u6ESiVG5WuPBwoNxhq5aceWaizwHTGYtQpEH-_gczmHgzKttZj49PNUX3tj7FxzljxQDhyq__ytuqNsW00l3Hqkf0ayX5rkRvkzw7uJ81f_op04PxVXG3KRmQ8XQgb9X-HR3qXYQFU98hrE2aCWC5ayIGGDPOAjgbPGuY7zEzwPw"
		}
		""";

	static String INTERMEDIATE_KEY = """
		{
		    "p": "-GoCSXl_r4nYeSeV85KSpn0POzwE2AL1SZ2jga1y9tDWhmirx5871oHxWvG5vVicQxe5Ls40m0eVafQbx1Yq6i2nesEeToB5kHKud-LzYaVX2SkFxvGO4h3yDhAb9R5GSk2F_-_t544t7XToH_IodcaeIXME2MKOW4p53jEeoSU",
		    "kty": "RSA",
		    "q": "vAYhuVmEA_XUDsZCF4aZ66QS6LYMLfKtpN_a6Q7bRk3jg0d4dd-jXzuvSQU9GJf6cvHJfx-NDR9YQZlYXvnhml3XMI4v3JZrsFnpRp3DOFXGCzU2S0prfAHOeFPUnCCHQbTGv97afKxkyFeWKjnH_3k3r07DyAaUXP_dTwR81_0",
		    "d": "PqW-eXZM_WAiuZHnHSQYaxn0-eohoXDXCG_bnXcg9DInabeIA3KBYmNnbYjOnPra25fO5slhx-Y85i6E6ZuIodj71OGK68DVfCh-FN3TO3glWeOglGarpyXIZXqmHXSE1Elan1F92doFrUXvZ_Z8oiM5Vp5WsSWvvZZcXT43iJjk02yGIGcg_lhJD6DCJ72be39k9zvuMhiXeeRyi29iv9w2HJafze6EYnrR9IZs9TE3nMBRfoW9LIFsOWHMXkOFt-JG40x17mbsZxEhrzf2Xs3iUcclwK1BBv4HsAVHYC9sClhC684XR7e3LXeUMSk9hTapMeJ9YhdVaYKTulUSEQ",
		    "e": "AQAB",
		    "use": "sig",
		    "kid": "intermediate-key",
		    "qi": "CUdVgv1--_szaWiZ0HTdzh03mCKapQ8EHpJKwMJdKPPZo2hbmM_2fZ30jI2bukRmu1ZpKID5YpebiPvvfrgSptqxq3UQZKgQLrGF4btx-jRfX36i85lPpyjRiQ_ZEgmajnzPgjZ2TMYMc7hvS7JfnAeQKSD_flu1Y0WcD1L6Cds",
		    "dp": "cc3fDlojLQAxtA92mwfjN3LDr5dngK6aQQd_CMG1kW1LWnhJekJw32zYbZmgRiZ6MCKGK_M8FodM5CV5NHE9Z9tShuJCSCSDjLF9TF8ksO9Wt54bj3DKzLx7UAYBSOJ9wTySzxXUH-j7EXYlgb0m3A4KE7jl14gKP9XIzwohxiE",
		    "alg": "RS256",
		    "dq": "fIoPMmbhc4ITuMmHI3ALabJet_dFL_YLICcKJ86oXPPgW6cuWd0MqFqvKs2mfzDDOfl-_o3WbrexTJoWl6opzYaUr1uuxgx4PfH_-r8qfJkLuYbAdLRRZ20G5lqYe-Vr0ZC9CH_C9kt14eFp3IQzPbt7r8mbElWRVdxAKUOOsb0",
		    "n": "tnPM44i2svYde-IcaDKS52W9IP4MqxThffTrjJW9NrLDGfXCH9BdnTHcLOqI4HKUcUrZnKSZI5i2bzr_lJz47aVOBzpqM2UpEXZZl5Zt7qH8Cp0iXjWsfwVaYEFLjQnpdh8uaVKdyAEtgibBWYqUvYbh4p9xG7pu0bvg3rsl-MyJeTkQOYR5PCLiAV0QQejoSHWue0Br8rAFU-96rCC2iqoK4Vot9kIP1cJGDr6zCHgvtLYF9Ni2y7qWb50xPvjNb-4Y2NNXaOfD2oX7pI5NVh5NkmlX6XF8Mb0TBjjBFnjGCWEX0LSb8pKbSGaZuQ4rTw8IBIZOuVSuA_yc5odUkQ"
		}
		""";

	static String TRUST_ANCHOR_KEY = """
		{
		    "p": "yBg2wwB7ncHQvmZTToYmvKZegvWcN8r7U6Y0pfNX3f8M5LsGdzCivuilwzsHpz3JTRB8I6TCNZRvCa7E35OhFUuXrH7-mDn54XbFY_NrvAW9sr3nYjJETojv8PjADkEiPVC3qMssUfi_2Q2ewC5zt-_eTy6bU0uXZlpMmsvzAF0",
		    "kty": "RSA",
		    "q": "tpWlBjhHWJwMY92OhfcERAxrjt1BZl-0yVCDU-ckmhS7dvQwnoAGirl7DcjOWcSqXngVIMB6ZmyKdQ0wqS8d_KVvDg22LdXzcmbwRwtymifk8ZARnKzG-5X0idhTeGYIclq16za5p-C4bR8GjevTK-X3iL7dA73gE2_F_fSvQ8U",
		    "d": "H7meJuktHAXwJLRHnXbbrZ1W0NsDxAOshSDzxS2x1Rg0KvAE4HsIVIe59Td93bIFTMV0EaerIJF6o4VUWmuLg8Fzdd3wSNTqsWxBsRET3c6uxb78EY17-EQ_6tiYhRrLIelWx7MgTfKuxiaLLaI8M81d1uqZz8VDpORTJGAGAJBhxdzTXMBL5WHf_pBj2VkXHq70dDMHsbCxEipifFW-AeijWgPnLJek8P8sgJYd9YlxnkuneRUhd_TR_sOHbuH8_QFPPXoN4SJuMc9XByQoF8qNku-x5kgir2i2ju568agqLJUGA_J-pyylDsjxdWb6kb32dHULWAg2OiwLaXHP8Q",
		    "e": "AQAB",
		    "use": "sig",
		    "kid": "trust-anchor-key",
		    "qi": "vZ6dWKVIc4JWLcs6v5MN7WM667_r8iYX15wxbR8TfdUOqu3-2tWhzyU_yQaRrN7WcIuj_DJZ4zaArRkLtNz9DXKUOLCpuY1GiYbYZfVw-qqgtlxxHLDx66fmNQecgksGRIV6PJ9zWTCkxo_tWfYjWKOltAEWmGiJZ8wQXavkk0Q",
		    "dp": "eAdWajlS7fpCjsRw-qysdgPSs_pDgJBpnO8Zq-bCca-6e0bZhQwOxDhonyk82QzOQUU_Ql5mBaAJz6QgAwHa4hxbWqU_ce9tPttxYxi1Kq_C8ILMVvJSWU35qPJ-us5FjMRrt3xRFMcDxl9OOHhlABSKpbMOh7PTmRlX1I-9MB0",
		    "alg": "RS256",
		    "dq": "pyZ2DiylfERXv3EMUi4idJ2eNgruHWeOnSIdKEmaI4jJWgwRGnmAA63sLmFgL6QwiA2M4g_zviKt2py5qJje7fURpx78YhfVKC1-2LttLh1Jpa_Lv9AY1ieg2DT3rWhTYUUT-AKAkmTS0esuV-zFxRFhvIlm0k8YfRK9glFnxjU",
		    "n": "jrYuAvmxMfBvPT6K84zSKgOOmLqe72uQnEWrYK_WuICBYcD8MhzL5ITiepHK62v1pY4Lr958uiDqSBP1heA6vzsLUljqLA4wgOLwS_UxO8VAO0g7wLMLt0HzBCxo5ZzvjD8zEEmE-5tc4Vxco-6r67cAX7zRtJoOn02dOZo7b_YCS1zn4GxhkKi9-XSuyxjFjG6TsqFhc71bnxRp7_9vlYZYrHF-89ELOvw0w7XtiMIhOSGYin_Cjq070H3tl_MP4rFw0M0U1ryyAEhDvGqG0L7wPWhKezQAk2GfsXK5-18d5D2Hy2975AkaW8XMlrnysigm78xdGbrrgGrebqqekQ"
		}
		""";

	private String createDummyJwt(String issuer, String subject, String keyToUseForJwksClaim, String keyToUseForSigning) throws ParseException, JOSEException {
		JWK signKey = JWK.parse(keyToUseForSigning);
		Algorithm algorithm = signKey.getAlgorithm();
		JWSAlgorithm alg = JWSAlgorithm.parse(algorithm.getName());
		JWSSignerFactory jwsSignerFactory = MultiJWSSignerFactory.getInstance();
		JWSSigner signer = jwsSignerFactory.createJWSSigner(signKey, alg);

		JWSHeader header = new JWSHeader.Builder(alg).keyID(signKey.getKeyID()).build();

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", issuer);
		claims.addProperty("sub", subject);
		claims.addProperty("iat", System.currentTimeMillis() / 1000);
		claims.addProperty("exp", (System.currentTimeMillis() / 1000) + 3600);
		JWK jwksKey = JWK.parse(keyToUseForJwksClaim);
		JWKSet jwkSet = new JWKSet(jwksKey);
		claims.add("jwks", JsonParser.parseString(jwkSet.toString(true)));

		JWTClaimsSet claimSet = JWTClaimsSet.parse(claims.toString());
		SignedJWT signJWT = new SignedJWT(header, claimSet);
		signJWT.sign(signer);

		return signJWT.serialize();
	}

	@Test
	void test_empty_chain() {
		List<String> emptyChain = new ArrayList<>();

		TrustChainVerifier.VerificationResult result = TrustChainVerifier.verifyTrustChain(RP, TRUST_ANCHOR, emptyChain);

		assertFalse(result.isVerified());
		assertEquals("Trust chain is null or empty", result.getError());
	}

	@Test
	void test_only_two_jwts_in_chain() throws Exception {
		List<String> singleTokenChain = new ArrayList<>();
		String jwt = createDummyJwt(LEAF, LEAF, LEAF_KEY, LEAF_KEY);
		singleTokenChain.add(jwt);

		TrustChainVerifier.VerificationResult result = TrustChainVerifier.verifyTrustChain(RP, TRUST_ANCHOR, singleTokenChain);

		assertFalse(result.isVerified());
		assertEquals("Trust chain must contain at least three tokens", result.getError());
	}

	@Test
	void happy_path_test() throws Exception {
		String leafConfig = createDummyJwt(LEAF, LEAF, LEAF_KEY, LEAF_KEY);
		String leafStatement = createDummyJwt(INTERMEDIATE, LEAF, LEAF_KEY, INTERMEDIATE_KEY);
		String intermediateStatement = createDummyJwt(TRUST_ANCHOR, INTERMEDIATE, INTERMEDIATE_KEY, TRUST_ANCHOR_KEY);
		String trustAnchorConfig = createDummyJwt(TRUST_ANCHOR, TRUST_ANCHOR, TRUST_ANCHOR_KEY, TRUST_ANCHOR_KEY);
		List<String> chain = List.of(leafConfig, leafStatement, intermediateStatement, trustAnchorConfig);

		TrustChainVerifier.VerificationResult result = TrustChainVerifier.verifyTrustChain(LEAF, TRUST_ANCHOR, chain);

		System.out.println(result.getError());
		assertTrue(result.isVerified());
	}

	@Test
	void leaf_entity_statement_signed_with_the_wrong_key_test() throws Exception {
		String leafConfig = createDummyJwt(LEAF, LEAF, LEAF_KEY, LEAF_KEY);
		String wrongKey = LEAF_KEY;
		String leafStatement = createDummyJwt(INTERMEDIATE, LEAF, LEAF_KEY, wrongKey);
		String intermediateStatement = createDummyJwt(TRUST_ANCHOR, INTERMEDIATE, INTERMEDIATE_KEY, TRUST_ANCHOR_KEY);
		String trustAnchorConfig = createDummyJwt(TRUST_ANCHOR, TRUST_ANCHOR, TRUST_ANCHOR_KEY, TRUST_ANCHOR_KEY);
		List<String> chain = List.of(leafConfig, leafStatement, intermediateStatement, trustAnchorConfig);

		TrustChainVerifier.VerificationResult result = TrustChainVerifier.verifyTrustChain(LEAF, TRUST_ANCHOR, chain);

		assertFalse(result.isVerified());
		assertEquals("Failed to verify JWT signature with the provided key at index 1", result.getError());
	}

}
