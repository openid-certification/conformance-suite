package net.openid.conformance.condition.client;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VerifyNewJwksHasNewSigningKey_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private VerifyNewJwksHasNewSigningKey cond;

	@Before
	public void setUp() throws Exception {
		cond = new VerifyNewJwksHasNewSigningKey();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {
		env.putObject("original_jwks", new JsonParser().parse("{\n" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"kid\": \"authlete-fapidev-api-20180524\",\n" +
			"      \"n\": \"nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q\"\n" +
			"    }\n" +
			"  ]\n" +
			"}").getAsJsonObject());

		env.putObject("new_jwks", new JsonParser().parse("{" +
			"  \"keys\": [" +
			"    {" +
			"      \"kty\": \"RSA\"," +
			"      \"e\": \"AQAB\"," +
			"      \"use\": \"sig\"," +
			"      \"kid\": \"fapi-test-suite\"," +
			"      \"alg\": \"RS256\"," +
			"      \"n\": \"qVGMeRQ8KG9ZVQFXa2YxCiapfesToXILzIvmHqNOJ8EbVwZKcwZ1iHpAzaZhnrSHJBU7Fgz9PEeTAqY-2FrRLE2xz3pW_LCg3y4fb1Sfi5GBSl1es0e4UD4Ie0g5SfQeZIpHfGM9zZx8rmsiDLRUyBsB8kHyUG6Ul5pHpAUHhrtghCpzL5lvnVvCM3-apeSyOdquS09tjwGUPUWifrKr0X1xEoNm7rsChZXQbB-MwqJG-ZGw51udn5-3mMunqp-B37jXdo4tYYOg0kKrpZSqQdRPANmTbge3LIN7uYprFVmGDMvXwiLMF9Unkeudox6nuIy3MWSeFRWpcrSQ_7s5p4mSupZxWHWrN_qSM-H9mTosvPg32m7XjQvlEJhuXqn13VYd8ZhmYSqV7s_x8bveZJLBnNPAkjPDktz8eUsdX3S8X6MNoYh4Ch7rsE_FO21B3FcMwtFql11z3_3PCAQOB3lzggy1hs-4Zb1As84R1Fmy0WKHTPAKPlJtLNFKz_bVf7xRbrt8BlPbkF4HduQixi-twkWRkUz8_-zCzWhz6s9y5ltQgs0_rl9aYSAt5z8UarP1PNfQIYGBmhwqGHi2LmrYFRjBEHRIsDXJcUGWgkfKMsiECs-mT-6AIw2L5DZIMIo8aroEnVpS24Ln2fwpdrRjO-b65VW-5cFOM6gERCc\""
			+
			"    }," +
			"    {\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"kid\": \"authlete-fapidev-api-20180524\",\n" +
			"      \"n\": \"nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q\"\n" +
			"    }\n" +
			"  ]" +
			"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testOneKeyNoRotation() {
		env.putObject("original_jwks", new JsonParser().parse("{\n" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"kid\": \"authlete-fapidev-api-20180524\",\n" +
			"      \"n\": \"nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q\"\n" +
			"    }\n" +
			"  ]\n" +
			"}").getAsJsonObject());

		env.putObject("new_jwks", env.getObject("original_jwks"));

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testOnlyKidChanged() {
		// a "new" key with a new kid, but the 'n' exponent is the same so it's not actually a new key
		env.putObject("original_jwks", new JsonParser().parse("{\n" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"kid\": \"authlete-fapidev-api-20180524\",\n" +
			"      \"n\": \"nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q\"\n" +
			"    }\n" +
			"  ]\n" +
			"}").getAsJsonObject());

		env.putObject("new_jwks", new JsonParser().parse("{" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"kid\": \"authlete-fapidev-api-20180524-new\",\n" +
			"      \"n\": \"nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q\"\n" +
			"    }\n" +
			"  ]\n" +
			"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testSameKidDifferentPublicExponent() {
		// here the key ('n') has changed, but the kid is the same, which should be a fail as the kid has to change
		// for the client to know it has to refetch the jwks
		env.putObject("original_jwks", new JsonParser().parse("{\n" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"kid\": \"authlete-fapidev-api-20180524\",\n" +
			"      \"n\": \"nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q\"\n" +
			"    }\n" +
			"  ]\n" +
			"}").getAsJsonObject());

		env.putObject("new_jwks", new JsonParser().parse("{" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"kid\": \"authlete-fapidev-api-20180524\",\n" +
			"      \"n\": \"xwQ72P9z9OYshiQ-ntDYaPnnfwG6u9JAdLMZ5o0dmjlcyrvwQRdoFIKPnO65Q8mh6F_LDSxjxa2Yzo_wdjhbPZLjfUJXgCzm54cClXzT5twzo7lzoAfaJlkTsoZc2HFWqmcri0BuzmTFLZx2Q7wYBm0pXHmQKF0V-C1O6NWfd4mfBhbM-I1tHYSpAMgarSm22WDMDx-WWI7TEzy2QhaBVaENW9BKaKkJklocAZCxk18WhR0fckIGiWiSM5FcU1PY2jfGsTmX505Ub7P5Dz75Ygqrutd5tFrcqyPAtPTFDk8X1InxkkUwpP3nFU5o50DGhwQolGYKPGtQ-ZtmbOfcWQ\"\n" +
			"    }\n" +
			"  ]\n" +
			"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test
	public void testLargeJwks() {
		// taken from Filip's public node oidc provider
		env.putObject("original_jwks", new JsonParser().parse("{\n" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"n\": \"xwQ72P9z9OYshiQ-ntDYaPnnfwG6u9JAdLMZ5o0dmjlcyrvwQRdoFIKPnO65Q8mh6F_LDSxjxa2Yzo_wdjhbPZLjfUJXgCzm54cClXzT5twzo7lzoAfaJlkTsoZc2HFWqmcri0BuzmTFLZx2Q7wYBm0pXHmQKF0V-C1O6NWfd4mfBhbM-I1tHYSpAMgarSm22WDMDx-WWI7TEzy2QhaBVaENW9BKaKkJklocAZCxk18WhR0fckIGiWiSM5FcU1PY2jfGsTmX505Ub7P5Dz75Ygqrutd5tFrcqyPAtPTFDk8X1InxkkUwpP3nFU5o50DGhwQolGYKPGtQ-ZtmbOfcWQ\",\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"kid\": \"r1LkbBo3925Rb2ZFFrKyU3MVex9T2817Kx0vbi6i_Kc\",\n" +
			"      \"use\": \"sig\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"n\": \"mXauIvyeUFA74P2vcmgAWSCMw6CP6-MJ6EvFuRARfLLJEi49AzQvJl_4pwDvLkZcCqS7OqPE1ufNyDH6oQPEc7JuukHMY02EgwqHjJ6GG6FQqJuiWlKB_l-7c9y9r4bh4r58xdZc6T5dFVSNT2VcIVoSjq9VmzwpaTKCUyVeZYHZhnLfWMm9rKU5WSz75siG-_jbudItsfhEwA59kvi4So2IV9TxHwW50i4IcTB1gXwG1olNgiX3-Mq1Iw5VGPzMo2hQXI3q1y-ZjhSwhvG5dje9J8htBEWdVYk4f6cv19IE9gEx7T-2vIVw5FCpAmmfFuRebec49c7zjfr0EyTI4w\",\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"kid\": \"w5kPRdJWODnYjihMgqs0tHkKk-e5OxU4DnSCZDkF_h0\",\n" +
			"      \"use\": \"enc\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"P-256\",\n" +
			"      \"x\": \"FWZ9rSkLt6Dx9E3pxLybhdM6xgR5obGsj5_pqmnz5J4\",\n" +
			"      \"y\": \"_n8G69C-A2Xl4xUW2lF0i8ZGZnk_KPYrhv4GbTGu5G4\",\n" +
			"      \"kty\": \"EC\",\n" +
			"      \"kid\": \"MFZeG102dQiqbANoaMlW_Jmf7fOZmtRsHt77JFhTpF0\",\n" +
			"      \"use\": \"sig\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"P-256\",\n" +
			"      \"x\": \"Eb3RtGgBGOEz33yu46aha_RU6pyBaYNlu6SawlWGGHQ\",\n" +
			"      \"y\": \"tUncttzF6Ud4Abfn1N2A1Rz2MBbJSdI0zuKS28BNb-U\",\n" +
			"      \"kty\": \"EC\",\n" +
			"      \"kid\": \"mlSUkq-ELqZiWl9zs9ZKkbcjIvgajGgnXfPWUZn9lEc\",\n" +
			"      \"use\": \"enc\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"P-384\",\n" +
			"      \"x\": \"P1npwyTJ2p20D9_r2u31DU7tfDEufaVcSJJcDOuO6QyqrXvjyMvf8e5xv3XxE39l\",\n" +
			"      \"y\": \"tmq2S12MVdKUQTmd0AxVEOji1ihR_vZAhTLKojD2XW_2EJH7ydiaz2oxrnkC0mvI\",\n" +
			"      \"kty\": \"EC\",\n" +
			"      \"kid\": \"rqHXKVLLF2RxqFgXWfEZE578gM-IhelOjugVfb_BMZ4\",\n" +
			"      \"use\": \"sig\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"P-384\",\n" +
			"      \"x\": \"UhkqvxbxMCGtkg_-6W0gqkr21fgY3LSaNbquU7CYEDwBwGCd6iK6Bu5PVUxraulY\",\n" +
			"      \"y\": \"CXrg3mxUkN5D4bPfiLfnD1jMYGSDxn2Zeh-8_OOstX21WNZJ9_i-iFZR3pIXyH0z\",\n" +
			"      \"kty\": \"EC\",\n" +
			"      \"kid\": \"rV1Hjt_79O_m1oJ7Jz0QgKHDa2iwb8p4kvMU0L99wjg\",\n" +
			"      \"use\": \"enc\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"P-521\",\n" +
			"      \"x\": \"AIjEl5H8w2Rf_iqIP8WT7v5-FlBlBGYy5sMJs1XOxWz4RRARIEOemEY45g10sEPzZ4qe7oyjCUDK5FY1WwjRvgHK\",\n" +
			"      \"y\": \"AaKN94cn1ApvvfpOWO9VpJm-lLzOUR8XxOrKYfPqcLs0zEqSPiGdWA5CoNL5ck1q-CXD09ysQSmNkzFGaig2Mnop\",\n" +
			"      \"kty\": \"EC\",\n" +
			"      \"kid\": \"RG_hu6lggazoCOu2wsrn3icSvhAXuGyL55f2GAaH2NA\",\n" +
			"      \"use\": \"sig\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"P-521\",\n" +
			"      \"x\": \"AXFcu6lqcxoyFUU14xTw0I5cfCR2q0jqOXwU_EKjA5mIxUpue58IIrfrIh4IauV3co2SziD6Uf1SWe8l11Y4-BoJ\",\n" +
			"      \"y\": \"AREzsMJu3VveUPMaJ2QWmjucwzZH4FqufXzS2IW-MGqViyDNTg2BgX-2VCJvdTo0zbhvRvBC1ghJNrVnH5M92JQ6\",\n" +
			"      \"kty\": \"EC\",\n" +
			"      \"kid\": \"MPcTmIIPYRnLt9s_TdBrpV27HcNVDi9aZpB0eJvAxzE\",\n" +
			"      \"use\": \"enc\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"Ed25519\",\n" +
			"      \"x\": \"lDkysGJKRmJeUp8ncTyGraHPHHiIfdxSajxGm7Srla8\",\n" +
			"      \"kty\": \"OKP\",\n" +
			"      \"kid\": \"CLjPrbijCB2z9dScRNpM1mSGOQVOIByTmd18Ft2eiAQ\",\n" +
			"      \"use\": \"sig\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"Ed448\",\n" +
			"      \"x\": \"BG1zKFg6A_Rzix4pA08oYN5xHqhKIiREXZ59NZoA8p3xhgjh-tm8nc-6udtiL5ZNhWDbnRSq4jQA\",\n" +
			"      \"kty\": \"OKP\",\n" +
			"      \"kid\": \"kU2PiegZOPUKcsJATItJArz18oWWfEH-Ma52K_8nGaE\",\n" +
			"      \"use\": \"sig\"\n" +
			"    }\n" +
			"  ]\n" +
			"}").getAsJsonObject());

		// same as original but with 'foo' added manually to a few to make some keys different
		env.putObject("new_jwks", new JsonParser().parse("{\n" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"n\": \"xwQ72P9z9OYshiQ-ntDYaPnnfwG6u9JAdLMZ5o0dmjlcyrvwQRdoFIKPnO65Q8mh6F_LDSxjxa2Yzo_wdjhbPZLjfUJXgCzm54cClXzT5twzo7lzoAfaJlkTsoZc2HFWqmcri0BuzmTFLZx2Q7wYBm0pXHmQKF0V-C1O6NWfd4mfBhbM-I1tHYSpAMgarSm22WDMDx-WWI7TEzy2QhaBVaENW9BKaKkJklocAZCxk18WhR0fckIGiWiSM5FcU1PY2jfGsTmX505Ub7P5Dz75Ygqrutd5tFrcqyPAtPTFDk8X1InxkkUwpP3nFU5o50DGhwQolGYKPGtQ-ZtmbOfcWQfoo\",\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"kid\": \"r1LkbBo3925Rb2ZFFrKyU3MVex9T2817Kx0vbi6i_Kcfoo\",\n" +
			"      \"use\": \"sig\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"n\": \"mXauIvyeUFA74P2vcmgAWSCMw6CP6-MJ6EvFuRARfLLJEi49AzQvJl_4pwDvLkZcCqS7OqPE1ufNyDH6oQPEc7JuukHMY02EgwqHjJ6GG6FQqJuiWlKB_l-7c9y9r4bh4r58xdZc6T5dFVSNT2VcIVoSjq9VmzwpaTKCUyVeZYHZhnLfWMm9rKU5WSz75siG-_jbudItsfhEwA59kvi4So2IV9TxHwW50i4IcTB1gXwG1olNgiX3-Mq1Iw5VGPzMo2hQXI3q1y-ZjhSwhvG5dje9J8htBEWdVYk4f6cv19IE9gEx7T-2vIVw5FCpAmmfFuRebec49c7zjfr0EyTI4wfoo\",\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"kid\": \"w5kPRdJWODnYjihMgqs0tHkKk-e5OxU4DnSCZDkF_h0foo\",\n" +
			"      \"use\": \"enc\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"P-256\",\n" +
			"      \"x\": \"FWZ9rSkLt6Dx9E3pxLybhdM6xgR5obGsj5_pqmnz5J4foo\",\n" +
			"      \"y\": \"_n8G69C-A2Xl4xUW2lF0i8ZGZnk_KPYrhv4GbTGu5G4\",\n" +
			"      \"kty\": \"EC\",\n" +
			"      \"kid\": \"MFZeG102dQiqbANoaMlW_Jmf7fOZmtRsHt77JFhTpF0foo\",\n" +
			"      \"use\": \"sig\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"P-256\",\n" +
			"      \"x\": \"Eb3RtGgBGOEz33yu46aha_RU6pyBaYNlu6SawlWGGHQfoo\",\n" +
			"      \"y\": \"tUncttzF6Ud4Abfn1N2A1Rz2MBbJSdI0zuKS28BNb-U\",\n" +
			"      \"kty\": \"EC\",\n" +
			"      \"kid\": \"mlSUkq-ELqZiWl9zs9ZKkbcjIvgajGgnXfPWUZn9lEcfoo\",\n" +
			"      \"use\": \"enc\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"P-384\",\n" +
			"      \"x\": \"P1npwyTJ2p20D9_r2u31DU7tfDEufaVcSJJcDOuO6QyqrXvjyMvf8e5xv3XxE39l\",\n" +
			"      \"y\": \"tmq2S12MVdKUQTmd0AxVEOji1ihR_vZAhTLKojD2XW_2EJH7ydiaz2oxrnkC0mvI\",\n" +
			"      \"kty\": \"EC\",\n" +
			"      \"kid\": \"rqHXKVLLF2RxqFgXWfEZE578gM-IhelOjugVfb_BMZ4\",\n" +
			"      \"use\": \"sig\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"P-384\",\n" +
			"      \"x\": \"UhkqvxbxMCGtkg_-6W0gqkr21fgY3LSaNbquU7CYEDwBwGCd6iK6Bu5PVUxraulY\",\n" +
			"      \"y\": \"CXrg3mxUkN5D4bPfiLfnD1jMYGSDxn2Zeh-8_OOstX21WNZJ9_i-iFZR3pIXyH0z\",\n" +
			"      \"kty\": \"EC\",\n" +
			"      \"kid\": \"rV1Hjt_79O_m1oJ7Jz0QgKHDa2iwb8p4kvMU0L99wjg\",\n" +
			"      \"use\": \"enc\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"P-521\",\n" +
			"      \"x\": \"AIjEl5H8w2Rf_iqIP8WT7v5-FlBlBGYy5sMJs1XOxWz4RRARIEOemEY45g10sEPzZ4qe7oyjCUDK5FY1WwjRvgHK\",\n" +
			"      \"y\": \"AaKN94cn1ApvvfpOWO9VpJm-lLzOUR8XxOrKYfPqcLs0zEqSPiGdWA5CoNL5ck1q-CXD09ysQSmNkzFGaig2Mnop\",\n" +
			"      \"kty\": \"EC\",\n" +
			"      \"kid\": \"RG_hu6lggazoCOu2wsrn3icSvhAXuGyL55f2GAaH2NA\",\n" +
			"      \"use\": \"sig\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"P-521\",\n" +
			"      \"x\": \"AXFcu6lqcxoyFUU14xTw0I5cfCR2q0jqOXwU_EKjA5mIxUpue58IIrfrIh4IauV3co2SziD6Uf1SWe8l11Y4-BoJ\",\n" +
			"      \"y\": \"AREzsMJu3VveUPMaJ2QWmjucwzZH4FqufXzS2IW-MGqViyDNTg2BgX-2VCJvdTo0zbhvRvBC1ghJNrVnH5M92JQ6\",\n" +
			"      \"kty\": \"EC\",\n" +
			"      \"kid\": \"MPcTmIIPYRnLt9s_TdBrpV27HcNVDi9aZpB0eJvAxzE\",\n" +
			"      \"use\": \"enc\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"Ed25519\",\n" +
			"      \"x\": \"lDkysGJKRmJeUp8ncTyGraHPHHiIfdxSajxGm7Srla8\",\n" +
			"      \"kty\": \"OKP\",\n" +
			"      \"kid\": \"CLjPrbijCB2z9dScRNpM1mSGOQVOIByTmd18Ft2eiAQ\",\n" +
			"      \"use\": \"sig\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"crv\": \"Ed448\",\n" +
			"      \"x\": \"BG1zKFg6A_Rzix4pA08oYN5xHqhKIiREXZ59NZoA8p3xhgjh-tm8nc-6udtiL5ZNhWDbnRSq4jQA\",\n" +
			"      \"kty\": \"OKP\",\n" +
			"      \"kid\": \"kU2PiegZOPUKcsJATItJArz18oWWfEH-Ma52K_8nGaE\",\n" +
			"      \"use\": \"sig\"\n" +
			"    }\n" +
			"  ]\n" +
			"}").getAsJsonObject());

		cond.execute(env);
	}

}
