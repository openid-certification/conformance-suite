package net.openid.conformance.condition.client;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VerifyNewJwksHasNewSigningKey_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private VerifyNewJwksHasNewSigningKey cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new VerifyNewJwksHasNewSigningKey();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {
		env.putObject("original_jwks", JsonParser.parseString("""
				{
				  "keys": [
				    {
				      "kty": "RSA",
				      "e": "AQAB",
				      "kid": "authlete-fapidev-api-20180524",
				      "n": "nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q"
				    }
				  ]
				}""").getAsJsonObject());

		env.putObject("new_jwks", JsonParser.parseString("""
				{  "keys": [    {      "kty": "RSA",      "e": "AQAB",      "use": "sig",      "kid": "fapi-test-suite",      "alg": "RS256",      "n": "qVGMeRQ8KG9ZVQFXa2YxCiapfesToXILzIvmHqNOJ8EbVwZKcwZ1iHpAzaZhnrSHJBU7Fgz9PEeTAqY-2FrRLE2xz3pW_LCg3y4fb1Sfi5GBSl1es0e4UD4Ie0g5SfQeZIpHfGM9zZx8rmsiDLRUyBsB8kHyUG6Ul5pHpAUHhrtghCpzL5lvnVvCM3-apeSyOdquS09tjwGUPUWifrKr0X1xEoNm7rsChZXQbB-MwqJG-ZGw51udn5-3mMunqp-B37jXdo4tYYOg0kKrpZSqQdRPANmTbge3LIN7uYprFVmGDMvXwiLMF9Unkeudox6nuIy3MWSeFRWpcrSQ_7s5p4mSupZxWHWrN_qSM-H9mTosvPg32m7XjQvlEJhuXqn13VYd8ZhmYSqV7s_x8bveZJLBnNPAkjPDktz8eUsdX3S8X6MNoYh4Ch7rsE_FO21B3FcMwtFql11z3_3PCAQOB3lzggy1hs-4Zb1As84R1Fmy0WKHTPAKPlJtLNFKz_bVf7xRbrt8BlPbkF4HduQixi-twkWRkUz8_-zCzWhz6s9y5ltQgs0_rl9aYSAt5z8UarP1PNfQIYGBmhwqGHi2LmrYFRjBEHRIsDXJcUGWgkfKMsiECs-mT-6AIw2L5DZIMIo8aroEnVpS24Ln2fwpdrRjO-b65VW-5cFOM6gERCc"    },    {
				      "kty": "RSA",
				      "e": "AQAB",
				      "kid": "authlete-fapidev-api-20180524",
				      "n": "nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q"
				    }
				  ]}""").getAsJsonObject());

		cond.execute(env);
	}

	@Test
	public void testOneKeyNoRotation() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("original_jwks", JsonParser.parseString("""
				{
				  "keys": [
				    {
				      "kty": "RSA",
				      "e": "AQAB",
				      "kid": "authlete-fapidev-api-20180524",
				      "n": "nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q"
				    }
				  ]
				}""").getAsJsonObject());

			env.putObject("new_jwks", env.getObject("original_jwks"));

			cond.execute(env);
		});
	}

	@Test
	public void testOnlyKidChanged() {
		assertThrows(ConditionError.class, () -> {
			// a "new" key with a new kid, but the 'n' exponent is the same so it's not actually a new key
			env.putObject("original_jwks", JsonParser.parseString("""
				{
				  "keys": [
				    {
				      "kty": "RSA",
				      "e": "AQAB",
				      "kid": "authlete-fapidev-api-20180524",
				      "n": "nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q"
				    }
				  ]
				}""").getAsJsonObject());

			env.putObject("new_jwks", JsonParser.parseString("""
				{  "keys": [
				    {
				      "kty": "RSA",
				      "e": "AQAB",
				      "kid": "authlete-fapidev-api-20180524-new",
				      "n": "nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q"
				    }
				  ]
				}""").getAsJsonObject());

			cond.execute(env);
		});
	}

	@Test
	public void testSameKidDifferentPublicExponent() {
		assertThrows(ConditionError.class, () -> {
			// here the key ('n') has changed, but the kid is the same, which should be a fail as the kid has to change
			// for the client to know it has to refetch the jwks
			env.putObject("original_jwks", JsonParser.parseString("""
				{
				  "keys": [
				    {
				      "kty": "RSA",
				      "e": "AQAB",
				      "kid": "authlete-fapidev-api-20180524",
				      "n": "nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q"
				    }
				  ]
				}""").getAsJsonObject());

			env.putObject("new_jwks", JsonParser.parseString("""
				{  "keys": [
				    {
				      "kty": "RSA",
				      "e": "AQAB",
				      "kid": "authlete-fapidev-api-20180524",
				      "n": "xwQ72P9z9OYshiQ-ntDYaPnnfwG6u9JAdLMZ5o0dmjlcyrvwQRdoFIKPnO65Q8mh6F_LDSxjxa2Yzo_wdjhbPZLjfUJXgCzm54cClXzT5twzo7lzoAfaJlkTsoZc2HFWqmcri0BuzmTFLZx2Q7wYBm0pXHmQKF0V-C1O6NWfd4mfBhbM-I1tHYSpAMgarSm22WDMDx-WWI7TEzy2QhaBVaENW9BKaKkJklocAZCxk18WhR0fckIGiWiSM5FcU1PY2jfGsTmX505Ub7P5Dz75Ygqrutd5tFrcqyPAtPTFDk8X1InxkkUwpP3nFU5o50DGhwQolGYKPGtQ-ZtmbOfcWQ"
				    }
				  ]
				}""").getAsJsonObject());

			cond.execute(env);
		});
	}

	@Test
	public void testLargeJwks() {
		// taken from Filip's public node oidc provider
		env.putObject("original_jwks", JsonParser.parseString("""
				{
				  "keys": [
				    {
				      "e": "AQAB",
				      "n": "xwQ72P9z9OYshiQ-ntDYaPnnfwG6u9JAdLMZ5o0dmjlcyrvwQRdoFIKPnO65Q8mh6F_LDSxjxa2Yzo_wdjhbPZLjfUJXgCzm54cClXzT5twzo7lzoAfaJlkTsoZc2HFWqmcri0BuzmTFLZx2Q7wYBm0pXHmQKF0V-C1O6NWfd4mfBhbM-I1tHYSpAMgarSm22WDMDx-WWI7TEzy2QhaBVaENW9BKaKkJklocAZCxk18WhR0fckIGiWiSM5FcU1PY2jfGsTmX505Ub7P5Dz75Ygqrutd5tFrcqyPAtPTFDk8X1InxkkUwpP3nFU5o50DGhwQolGYKPGtQ-ZtmbOfcWQ",
				      "kty": "RSA",
				      "kid": "r1LkbBo3925Rb2ZFFrKyU3MVex9T2817Kx0vbi6i_Kc",
				      "use": "sig"
				    },
				    {
				      "e": "AQAB",
				      "n": "mXauIvyeUFA74P2vcmgAWSCMw6CP6-MJ6EvFuRARfLLJEi49AzQvJl_4pwDvLkZcCqS7OqPE1ufNyDH6oQPEc7JuukHMY02EgwqHjJ6GG6FQqJuiWlKB_l-7c9y9r4bh4r58xdZc6T5dFVSNT2VcIVoSjq9VmzwpaTKCUyVeZYHZhnLfWMm9rKU5WSz75siG-_jbudItsfhEwA59kvi4So2IV9TxHwW50i4IcTB1gXwG1olNgiX3-Mq1Iw5VGPzMo2hQXI3q1y-ZjhSwhvG5dje9J8htBEWdVYk4f6cv19IE9gEx7T-2vIVw5FCpAmmfFuRebec49c7zjfr0EyTI4w",
				      "kty": "RSA",
				      "kid": "w5kPRdJWODnYjihMgqs0tHkKk-e5OxU4DnSCZDkF_h0",
				      "use": "enc"
				    },
				    {
				      "crv": "P-256",
				      "x": "FWZ9rSkLt6Dx9E3pxLybhdM6xgR5obGsj5_pqmnz5J4",
				      "y": "_n8G69C-A2Xl4xUW2lF0i8ZGZnk_KPYrhv4GbTGu5G4",
				      "kty": "EC",
				      "kid": "MFZeG102dQiqbANoaMlW_Jmf7fOZmtRsHt77JFhTpF0",
				      "use": "sig"
				    },
				    {
				      "crv": "P-256",
				      "x": "Eb3RtGgBGOEz33yu46aha_RU6pyBaYNlu6SawlWGGHQ",
				      "y": "tUncttzF6Ud4Abfn1N2A1Rz2MBbJSdI0zuKS28BNb-U",
				      "kty": "EC",
				      "kid": "mlSUkq-ELqZiWl9zs9ZKkbcjIvgajGgnXfPWUZn9lEc",
				      "use": "enc"
				    },
				    {
				      "crv": "P-384",
				      "x": "P1npwyTJ2p20D9_r2u31DU7tfDEufaVcSJJcDOuO6QyqrXvjyMvf8e5xv3XxE39l",
				      "y": "tmq2S12MVdKUQTmd0AxVEOji1ihR_vZAhTLKojD2XW_2EJH7ydiaz2oxrnkC0mvI",
				      "kty": "EC",
				      "kid": "rqHXKVLLF2RxqFgXWfEZE578gM-IhelOjugVfb_BMZ4",
				      "use": "sig"
				    },
				    {
				      "crv": "P-384",
				      "x": "UhkqvxbxMCGtkg_-6W0gqkr21fgY3LSaNbquU7CYEDwBwGCd6iK6Bu5PVUxraulY",
				      "y": "CXrg3mxUkN5D4bPfiLfnD1jMYGSDxn2Zeh-8_OOstX21WNZJ9_i-iFZR3pIXyH0z",
				      "kty": "EC",
				      "kid": "rV1Hjt_79O_m1oJ7Jz0QgKHDa2iwb8p4kvMU0L99wjg",
				      "use": "enc"
				    },
				    {
				      "crv": "P-521",
				      "x": "AIjEl5H8w2Rf_iqIP8WT7v5-FlBlBGYy5sMJs1XOxWz4RRARIEOemEY45g10sEPzZ4qe7oyjCUDK5FY1WwjRvgHK",
				      "y": "AaKN94cn1ApvvfpOWO9VpJm-lLzOUR8XxOrKYfPqcLs0zEqSPiGdWA5CoNL5ck1q-CXD09ysQSmNkzFGaig2Mnop",
				      "kty": "EC",
				      "kid": "RG_hu6lggazoCOu2wsrn3icSvhAXuGyL55f2GAaH2NA",
				      "use": "sig"
				    },
				    {
				      "crv": "P-521",
				      "x": "AXFcu6lqcxoyFUU14xTw0I5cfCR2q0jqOXwU_EKjA5mIxUpue58IIrfrIh4IauV3co2SziD6Uf1SWe8l11Y4-BoJ",
				      "y": "AREzsMJu3VveUPMaJ2QWmjucwzZH4FqufXzS2IW-MGqViyDNTg2BgX-2VCJvdTo0zbhvRvBC1ghJNrVnH5M92JQ6",
				      "kty": "EC",
				      "kid": "MPcTmIIPYRnLt9s_TdBrpV27HcNVDi9aZpB0eJvAxzE",
				      "use": "enc"
				    },
				    {
				      "crv": "Ed25519",
				      "x": "lDkysGJKRmJeUp8ncTyGraHPHHiIfdxSajxGm7Srla8",
				      "kty": "OKP",
				      "kid": "CLjPrbijCB2z9dScRNpM1mSGOQVOIByTmd18Ft2eiAQ",
				      "use": "sig"
				    },
				    {
				      "crv": "Ed448",
				      "x": "BG1zKFg6A_Rzix4pA08oYN5xHqhKIiREXZ59NZoA8p3xhgjh-tm8nc-6udtiL5ZNhWDbnRSq4jQA",
				      "kty": "OKP",
				      "kid": "kU2PiegZOPUKcsJATItJArz18oWWfEH-Ma52K_8nGaE",
				      "use": "sig"
				    }
				  ]
				}""").getAsJsonObject());

		// same as original but with 'foo' added manually to a few to make some keys different
		env.putObject("new_jwks", JsonParser.parseString("""
				{
				  "keys": [
				    {
				      "e": "AQAB",
				      "n": "xwQ72P9z9OYshiQ-ntDYaPnnfwG6u9JAdLMZ5o0dmjlcyrvwQRdoFIKPnO65Q8mh6F_LDSxjxa2Yzo_wdjhbPZLjfUJXgCzm54cClXzT5twzo7lzoAfaJlkTsoZc2HFWqmcri0BuzmTFLZx2Q7wYBm0pXHmQKF0V-C1O6NWfd4mfBhbM-I1tHYSpAMgarSm22WDMDx-WWI7TEzy2QhaBVaENW9BKaKkJklocAZCxk18WhR0fckIGiWiSM5FcU1PY2jfGsTmX505Ub7P5Dz75Ygqrutd5tFrcqyPAtPTFDk8X1InxkkUwpP3nFU5o50DGhwQolGYKPGtQ-ZtmbOfcWQfoo",
				      "kty": "RSA",
				      "kid": "r1LkbBo3925Rb2ZFFrKyU3MVex9T2817Kx0vbi6i_Kcfoo",
				      "use": "sig"
				    },
				    {
				      "e": "AQAB",
				      "n": "mXauIvyeUFA74P2vcmgAWSCMw6CP6-MJ6EvFuRARfLLJEi49AzQvJl_4pwDvLkZcCqS7OqPE1ufNyDH6oQPEc7JuukHMY02EgwqHjJ6GG6FQqJuiWlKB_l-7c9y9r4bh4r58xdZc6T5dFVSNT2VcIVoSjq9VmzwpaTKCUyVeZYHZhnLfWMm9rKU5WSz75siG-_jbudItsfhEwA59kvi4So2IV9TxHwW50i4IcTB1gXwG1olNgiX3-Mq1Iw5VGPzMo2hQXI3q1y-ZjhSwhvG5dje9J8htBEWdVYk4f6cv19IE9gEx7T-2vIVw5FCpAmmfFuRebec49c7zjfr0EyTI4wfoo",
				      "kty": "RSA",
				      "kid": "w5kPRdJWODnYjihMgqs0tHkKk-e5OxU4DnSCZDkF_h0foo",
				      "use": "enc"
				    },
				    {
				      "crv": "P-256",
				      "x": "FWZ9rSkLt6Dx9E3pxLybhdM6xgR5obGsj5_pqmnz5J5",
				      "y": "_n8G69C-A2Xl4xUW2lF0i8ZGZnk_KPYrhv4GbTGu5G4",
				      "kty": "EC",
				      "kid": "MFZeG102dQiqbANoaMlW_Jmf7fOZmtRsHt77JFhTpF1",
				      "use": "sig"
				    },
				    {
				      "crv": "P-256",
				      "x": "Eb3RtGgBGOEz33yu46aha_RU6pyBaYNlu6SawlWGGHQfoo",
				      "y": "tUncttzF6Ud4Abfn1N2A1Rz2MBbJSdI0zuKS28BNb-U",
				      "kty": "EC",
				      "kid": "mlSUkq-ELqZiWl9zs9ZKkbcjIvgajGgnXfPWUZn9lEcfoo",
				      "use": "enc"
				    },
				    {
				      "crv": "P-384",
				      "x": "P1npwyTJ2p20D9_r2u31DU7tfDEufaVcSJJcDOuO6QyqrXvjyMvf8e5xv3XxE39l",
				      "y": "tmq2S12MVdKUQTmd0AxVEOji1ihR_vZAhTLKojD2XW_2EJH7ydiaz2oxrnkC0mvI",
				      "kty": "EC",
				      "kid": "rqHXKVLLF2RxqFgXWfEZE578gM-IhelOjugVfb_BMZ4",
				      "use": "sig"
				    },
				    {
				      "crv": "P-384",
				      "x": "UhkqvxbxMCGtkg_-6W0gqkr21fgY3LSaNbquU7CYEDwBwGCd6iK6Bu5PVUxraulY",
				      "y": "CXrg3mxUkN5D4bPfiLfnD1jMYGSDxn2Zeh-8_OOstX21WNZJ9_i-iFZR3pIXyH0z",
				      "kty": "EC",
				      "kid": "rV1Hjt_79O_m1oJ7Jz0QgKHDa2iwb8p4kvMU0L99wjg",
				      "use": "enc"
				    },
				    {
				      "crv": "P-521",
				      "x": "AIjEl5H8w2Rf_iqIP8WT7v5-FlBlBGYy5sMJs1XOxWz4RRARIEOemEY45g10sEPzZ4qe7oyjCUDK5FY1WwjRvgHK",
				      "y": "AaKN94cn1ApvvfpOWO9VpJm-lLzOUR8XxOrKYfPqcLs0zEqSPiGdWA5CoNL5ck1q-CXD09ysQSmNkzFGaig2Mnop",
				      "kty": "EC",
				      "kid": "RG_hu6lggazoCOu2wsrn3icSvhAXuGyL55f2GAaH2NA",
				      "use": "sig"
				    },
				    {
				      "crv": "P-521",
				      "x": "AXFcu6lqcxoyFUU14xTw0I5cfCR2q0jqOXwU_EKjA5mIxUpue58IIrfrIh4IauV3co2SziD6Uf1SWe8l11Y4-BoJ",
				      "y": "AREzsMJu3VveUPMaJ2QWmjucwzZH4FqufXzS2IW-MGqViyDNTg2BgX-2VCJvdTo0zbhvRvBC1ghJNrVnH5M92JQ6",
				      "kty": "EC",
				      "kid": "MPcTmIIPYRnLt9s_TdBrpV27HcNVDi9aZpB0eJvAxzE",
				      "use": "enc"
				    },
				    {
				      "crv": "Ed25519",
				      "x": "lDkysGJKRmJeUp8ncTyGraHPHHiIfdxSajxGm7Srla8",
				      "kty": "OKP",
				      "kid": "CLjPrbijCB2z9dScRNpM1mSGOQVOIByTmd18Ft2eiAQ",
				      "use": "sig"
				    },
				    {
				      "crv": "Ed448",
				      "x": "BG1zKFg6A_Rzix4pA08oYN5xHqhKIiREXZ59NZoA8p3xhgjh-tm8nc-6udtiL5ZNhWDbnRSq4jQA",
				      "kty": "OKP",
				      "kid": "kU2PiegZOPUKcsJATItJArz18oWWfEH-Ma52K_8nGaE",
				      "use": "sig"
				    }
				  ]
				}""").getAsJsonObject());

		cond.execute(env);
	}


	@Test
	public void testInvalidKeys() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("original_jwks", JsonParser.parseString(
				"""
						{
						  "keys": [
						    {
						      "crv": "P-256",
						      "x": "FWZ9rSkLt6Dx9E3pxLybhdM6xgR5obGsj5_pqmnz5JFoo",
						      "y": "_n8G69C-A2Xl4xUW2lF0i8ZGZnk_KPYrhv4GbTGu5GFoo",
						      "kty": "EC",
						      "kid": "MFZeG102dQiqbANoaMlW_Jmf7fOZmtRsHt77JFhTpF1",
						      "use": "sig"
						    }
						  ]
						}"""
			).getAsJsonObject());

			env.putObject("new_jwks", env.getObject("original_jwks"));

			cond.execute(env);
		});
	}

	@Test
	public void testSameExponent() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("original_jwks", JsonParser.parseString(
				"""
						{
						  "keys": [
						    {
						      "crv": "P-384",
						      "x": "UhkqvxbxMCGtkg_-6W0gqkr21fgY3LSaNbquU7CYEDwBwGCd6iK6Bu5PVUxraulY",
						      "y": "CXrg3mxUkN5D4bPfiLfnD1jMYGSDxn2Zeh-8_OOstX21WNZJ9_i-iFZR3pIXyH0z",
						      "kty": "EC",
						      "kid": "rV1Hjt_79O_m1oJ7Jz0QgKHDa2iwb8p4kvMU0L99wjg",
						      "use": "sig"
						    }
						  ]
						}"""
			).getAsJsonObject());

			env.putObject("new_jwks", JsonParser.parseString(
				"""
						{
						  "keys": [
						    {
						      "crv": "P-384",
						      "x": "UhkqvxbxMCGtkg_-6W0gqkr21fgY3LSaNbquU7CYEDwBwGCd6iK6Bu5PVUxraulY",
						      "y": "CXrg3mxUkN5D4bPfiLfnD1jMYGSDxn2Zeh-8_OOstX21WNZJ9_i-iFZR3pIXyH0z",
						      "kty": "EC",
						      "kid": "rV1Hjt_79O_m1oJ7Jz0QgKHDa2iwb8p4kvMU0L99wjgFoo",
						      "use": "sig"
						    }
						  ]
						}"""
			).getAsJsonObject());

			cond.execute(env);
		});
	}


	@Test
	public void testOriginalSigningKeyWithChangedKeyOps() {
		env.putObject("original_jwks", JsonParser.parseString("""
				{
				  "keys": [
				    {
				      "kty": "RSA",
				      "x5t#S256": "pQRhNcATHi9--fTzR8y46jyKkVlQuYWwx1WtLi5i1Z0",
				      "e": "AQAB",
				      "use": "sig",
				      "kid": "authnation",
				      "x5c": [
				        "MIIDbTCCAlWgAwIBAgIIUviusM0IxhwwDQYJKoZIhvcNAQELBQAwZTELMAkGA1UEBhMCREUxDzANBgNVBAgTBlNheG9ueTEQMA4GA1UEBxMHR2VybWFueTEMMAoGA1UEChMDTUVXMQswCQYDVQQLEwJTSzEYMBYGA1UEAxMPYWFhLm1ldy10ZWNoLmlvMB4XDTIwMDcxNDEyMTIxN1oXDTIwMTAxMjEyMTIxN1owZTELMAkGA1UEBhMCREUxDzANBgNVBAgTBlNheG9ueTEQMA4GA1UEBxMHR2VybWFueTEMMAoGA1UEChMDTUVXMQswCQYDVQQLEwJTSzEYMBYGA1UEAxMPYWFhLm1ldy10ZWNoLmlvMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnFR3XnCMJyc0StJpwLOJYxXCvmUU+XRQI6mUU+enbJcWtUlQEceuMhfTMTXeodO0maxSkAiXl4ILlsXIuVo5aDwKCrc5yYy/Mjlx/yM8ZQ137FY9GW1UEBtuCDeDfz74oNK7VxLUAr5MlxxatUH9GUDrYdKO3SsQ+g0KzFeUVZYCXCVXJIJVWE29mll8s5P9H1M4Gfhew3+88YacgO5/GarLp6RIu91bqqOcoq5iSPfHFVEgHW6B6s8iaAtfHJ8gxB0Hdar5d0IhBdetUOpVk6HlHzyiOSFG1Z5db8l8wNB2/5wsqv4Oo+cV+c1Sv3OgWtuhsfL2frFxoBhviAyYhQIDAQABoyEwHzAdBgNVHQ4EFgQUbfWw2f5vOKVDs45zwoVUb0S7RswwDQYJKoZIhvcNAQELBQADggEBAIp2yacMHVT611nv7/9i/1mK9PMxjnPaZq0Skg5URz5yyy5Ey3yRAKWKJ6amnmuKR6/l+AYyJxL3eZ29/vNY5vGueX+LvBDd91kic78lPE62e2KqmY1+YxsouxTGt/G8TsIXVpSnyHT2WuNQSFTtL8TpcyMYY2i8gb4yCB+qr68S0X/btNXD0zxwMNEGLgIoNitfegJBis1GG4hn4Z7bMJBK2Bc5FErjN5RyYq8A0Kg9Bc/lxFDdkjkbDrNJBNqd0DnZdB0Bw9cosVTpo8BDOPn1UcMt3edjKjGBld+XVsBs9mdgvvV+jAMhr/9TdCz2B9oM9bI1u86CSSXAxtMO/BQ="
				      ],
				      "key_ops": [
				        "verify"
				      ],
				      "n": "nFR3XnCMJyc0StJpwLOJYxXCvmUU-XRQI6mUU-enbJcWtUlQEceuMhfTMTXeodO0maxSkAiXl4ILlsXIuVo5aDwKCrc5yYy_Mjlx_yM8ZQ137FY9GW1UEBtuCDeDfz74oNK7VxLUAr5MlxxatUH9GUDrYdKO3SsQ-g0KzFeUVZYCXCVXJIJVWE29mll8s5P9H1M4Gfhew3-88YacgO5_GarLp6RIu91bqqOcoq5iSPfHFVEgHW6B6s8iaAtfHJ8gxB0Hdar5d0IhBdetUOpVk6HlHzyiOSFG1Z5db8l8wNB2_5wsqv4Oo-cV-c1Sv3OgWtuhsfL2frFxoBhviAyYhQ"
				    },
				    {
				      "kty": "RSA",
				      "x5t#S256": "ZEFNZkxWkGCkfNedvQrg-03b6x2KN1RfZkmh9IOEJn4",
				      "e": "AQAB",
				      "use": "sig",
				      "kid": "authnation01",
				      "x5c": [
				        "MIIDbjCCAlagAwIBAgIJANpibpKX6nXUMA0GCSqGSIb3DQEBCwUAMGUxCzAJBgNVBAYTAkRFMQ8wDQYDVQQIEwZTYXhvbnkxEDAOBgNVBAcTB0dlcm1hbnkxDDAKBgNVBAoTA01FVzELMAkGA1UECxMCU0sxGDAWBgNVBAMTD2FhYS5tZXctdGVjaC5pbzAeFw0yMDA3MTQxMjEyMjBaFw0yMDEwMTIxMjEyMjBaMGUxCzAJBgNVBAYTAkRFMQ8wDQYDVQQIEwZTYXhvbnkxEDAOBgNVBAcTB0dlcm1hbnkxDDAKBgNVBAoTA01FVzELMAkGA1UECxMCU0sxGDAWBgNVBAMTD2FhYS5tZXctdGVjaC5pbzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALjsWJhewp2l5kcPgOd/YPjSpn2qnXyj0sYWP/7O4wEXK8tTGqzl4QNipWR4Xxzc9HrhMRPfZ0IQlqU4Zx6knuh52iXu0TKxg7A2kAf7p0T3jedpvKRcWv3bd7OQ3r6CFoyHWZoyw+rl2oxs2LtXfgKanlyFMj76JLz15Z4ogIGy5GPJ3DPjmNhvUYeZ2E7jB4aMK6QRIVO4RC8l54W6oVRwNxFDe7SpgC3GApbq6zbE/B9u/og5si7JnSOucq6aVIxJTA8QTbg36dJZ+rLCYL/IQwnRnbRo3Ly+RMuf2Dj1pgQspEtOnL9jsxm3iFmRiR5DHihvtjms4qiJQpJqPZcCAwEAAaMhMB8wHQYDVR0OBBYEFJKtD/XwTFyqAwMkRongBAsWMLhzMA0GCSqGSIb3DQEBCwUAA4IBAQAp73UivwRtZdSsYdBqKhf5BBVrWwdyCYdlQCBHjKVAbVfI+jSODh9CgeV4qJGYEXrSLHi99gZ54Zip9EBcLI6OHSM0OKwjFWtLikig1pWTFXS6EFxmb0V2mRBJI5+N8PXwErzZwwmkS07BO8NOVEc/j5uEbOXZY05R0It2sI+ukfLJWxP4Q94lToSJQYrvOz5kSO9yZMwdlcNF3hzRFl7RjAqDMHwYo48sv2Z9t3bV15pC+jAzI02FdkKrEkWu0DwOa7o9NZgMgWTKQ7wkae3T7Y2ED0JhwEfyizH8nbKXpYKIdwrLXGQT2MfIRVRk4mLRg+sWA4ImkD4APxZcQAdN"
				      ],
				      "key_ops": [
				        "verify"
				      ],
				      "n": "uOxYmF7CnaXmRw-A539g-NKmfaqdfKPSxhY__s7jARcry1MarOXhA2KlZHhfHNz0euExE99nQhCWpThnHqSe6HnaJe7RMrGDsDaQB_unRPeN52m8pFxa_dt3s5DevoIWjIdZmjLD6uXajGzYu1d-ApqeXIUyPvokvPXlniiAgbLkY8ncM-OY2G9Rh5nYTuMHhowrpBEhU7hELyXnhbqhVHA3EUN7tKmALcYClurrNsT8H27-iDmyLsmdI65yrppUjElMDxBNuDfp0ln6ssJgv8hDCdGdtGjcvL5Ey5_YOPWmBCykS06cv2OzGbeIWZGJHkMeKG-2OaziqIlCkmo9lw"
				    },
				    {
				      "kty": "RSA",
				      "x5t#S256": "ttsMvLlc6h0hXfhe3lw7VAc8dMviMWc8XCNTow51izI",
				      "e": "AQAB",
				      "use": "sig",
				      "kid": "authnation02",
				      "x5c": [
				        "MIIDbTCCAlWgAwIBAgIIPeupkk9kmXMwDQYJKoZIhvcNAQELBQAwZTELMAkGA1UEBhMCREUxDzANBgNVBAgTBlNheG9ueTEQMA4GA1UEBxMHR2VybWFueTEMMAoGA1UEChMDTUVXMQswCQYDVQQLEwJTSzEYMBYGA1UEAxMPYWFhLm1ldy10ZWNoLmlvMB4XDTIwMDcxNDEyMTIyMloXDTIwMTAxMjEyMTIyMlowZTELMAkGA1UEBhMCREUxDzANBgNVBAgTBlNheG9ueTEQMA4GA1UEBxMHR2VybWFueTEMMAoGA1UEChMDTUVXMQswCQYDVQQLEwJTSzEYMBYGA1UEAxMPYWFhLm1ldy10ZWNoLmlvMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm5j9yEcnAEH7TNGj1Mnyzs9Pdx11xIR25hACeU+dv0xHqSw7XvltRFcCUA6cErw1ortbrlfiuiiE71Q7C6zQ7rV9miu17P4KQMPSJ4/fwL5EbQEGYXTV+AHZtVWMLBNEJRQzNFVq8cMDKgEdiA0l6bobIeIYVYwN7q66/gFxNZNjP2ux9pGk4uj9QPxYQjIefoC7tvsTgYWrGoCwZK0Mv0X0D0HoE4XujutjdDqiEif8Kzmg1BghFGkwO8hrikF9d8o+f9INLl6pG/LbiN/FpWKZed/i8KnYCUQp9mv56YJCOwDFqIHhf992Dgu3tIhD7yOqVukY6UCbem9qYfaMeQIDAQABoyEwHzAdBgNVHQ4EFgQUjw+shMWiGgXNrueLHBLV+4mBHq8wDQYJKoZIhvcNAQELBQADggEBAB4LG1DvbQvMD+n0WJH0gmux76o1EJwy/rL0eKEiZ6KObshtjvrMwqV1QxkS5IPucCgRFkhGmJeEjxxvYuw2xuzI6ddXCKVIehc6YuwQZK/1xOG+givWYSJAytdVjxSZfmK69qFwn2vG111vpmMKvS2iULN4Q7QG3FG3BYTmyJ0wRQEHVSGb1OP9+R+tN5wx/U7mucD1YneM2uZfyGpGTHNGeu8Zwxl0cd/SDBjJwewhNpbKKU+H2nF3VTKdjtWRJGksQ9f/a7xw55Z4dAgcWUkAO3yWA9hlYXJI9qQ65752Gi0V9NaUKunUcKswpth6FPUyjdW02ROKm1W7w6L2fD8="
				      ],
				      "key_ops": [
				        "verify"
				      ],
				      "n": "m5j9yEcnAEH7TNGj1Mnyzs9Pdx11xIR25hACeU-dv0xHqSw7XvltRFcCUA6cErw1ortbrlfiuiiE71Q7C6zQ7rV9miu17P4KQMPSJ4_fwL5EbQEGYXTV-AHZtVWMLBNEJRQzNFVq8cMDKgEdiA0l6bobIeIYVYwN7q66_gFxNZNjP2ux9pGk4uj9QPxYQjIefoC7tvsTgYWrGoCwZK0Mv0X0D0HoE4XujutjdDqiEif8Kzmg1BghFGkwO8hrikF9d8o-f9INLl6pG_LbiN_FpWKZed_i8KnYCUQp9mv56YJCOwDFqIHhf992Dgu3tIhD7yOqVukY6UCbem9qYfaMeQ"
				    },
				    {
				      "kty": "RSA",
				      "x5t#S256": "HsPEDMf8ZT6s0dTUtb8fMw6GZa_U3vWNGhsIDHQeWH0",
				      "e": "AQAB",
				      "use": "sig",
				      "kid": "authnation03",
				      "x5c": [
				        "MIIDbTCCAlWgAwIBAgIIbPMO+CRt+UkwDQYJKoZIhvcNAQELBQAwZTELMAkGA1UEBhMCREUxDzANBgNVBAgTBlNheG9ueTEQMA4GA1UEBxMHR2VybWFueTEMMAoGA1UEChMDTUVXMQswCQYDVQQLEwJTSzEYMBYGA1UEAxMPYWFhLm1ldy10ZWNoLmlvMB4XDTIwMDcxNDEyMTIyNFoXDTIwMTAxMjEyMTIyNFowZTELMAkGA1UEBhMCREUxDzANBgNVBAgTBlNheG9ueTEQMA4GA1UEBxMHR2VybWFueTEMMAoGA1UEChMDTUVXMQswCQYDVQQLEwJTSzEYMBYGA1UEAxMPYWFhLm1ldy10ZWNoLmlvMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs1t1HxtM8/rANLGktnj7mnn4/ShPJ78YZMt0koMpOwYCDvtUUPhGVpm0XMflrz7s5TcCkN6t0Sq7R/i0FrNpDodJMwDXGJskmYLvdsAW9mtA52VlwV9ha6WbBdwtECnAHBYlvyWIxs0Py84uy3PrhNXGSAU2z82lYxCQx6yyINi+0M7fuvth1VNq6Mt0dJKBD6FlMdWCG/lZUAbgIdUFv4ZxlIehqBBH//4kZgnibu4z47/Cw0VRb5z/QV/EiYyT224jzivyWSP1klTBqJt1dk+B6C6QTrJLrH+TsOSXdODXgSnRGi36RUA75b4x1jXpsA6T95XPbzIC2BOLqfriJwIDAQABoyEwHzAdBgNVHQ4EFgQUmdTi1CLz6RzuFY4MMNTdhGfYVR0wDQYJKoZIhvcNAQELBQADggEBAGr2h7+SwmfihMIp43uW9DpXdNg8BOjCIOPLPA+Q2mGY1zUcbJv1vh+6s0S+4wl6wgfguekjF5Y7ty+aT8KmA6TA14VfWaKDU3CPrqBEGoJh74K3ycV4OtzmjoMoHm3jE3qPbbz2n9wptktVSzHv9my7ajPq95V3VZy2ADQnKzKEhH0BadDLiY3PYQ99vjTaSoB87/nYf81f7DCg8Ax3lUS2mo54rPVjQucddSFvtiebyJbMBuKaRUL0f+85KRsJAbvbYY0nxdQFawzqCCsahIBXRp8NAtfCsTHvhH+O3aNe/Z6ItMzb+VnSuTw3587QVh/1OWDrPk4w+t3drd9BALY="
				      ],
				      "key_ops": [
				        "verify"
				      ],
				      "n": "s1t1HxtM8_rANLGktnj7mnn4_ShPJ78YZMt0koMpOwYCDvtUUPhGVpm0XMflrz7s5TcCkN6t0Sq7R_i0FrNpDodJMwDXGJskmYLvdsAW9mtA52VlwV9ha6WbBdwtECnAHBYlvyWIxs0Py84uy3PrhNXGSAU2z82lYxCQx6yyINi-0M7fuvth1VNq6Mt0dJKBD6FlMdWCG_lZUAbgIdUFv4ZxlIehqBBH__4kZgnibu4z47_Cw0VRb5z_QV_EiYyT224jzivyWSP1klTBqJt1dk-B6C6QTrJLrH-TsOSXdODXgSnRGi36RUA75b4x1jXpsA6T95XPbzIC2BOLqfriJw"
				    },
				    {
				      "kty": "RSA",
				      "x5t#S256": "nOmjF5rA4ohhc8ETjoI8Qj6G7arpEbWhOzaNY7fOie8",
				      "e": "AQAB",
				      "use": "sig",
				      "kid": "4095b867-e9af-405d-b417-0661b9a68d9c",
				      "x5c": [
				        "MIICqTCCAZGgAwIBAgIRANlvuvearUFCp+jfVXQSRZ8wDQYJKoZIhvcNAQELBQAwEDEOMAwGA1UEAxMFd29ybGQwHhcNMjAwODA5MTg0OTI1WhcNMjAwODA5MTg0OTI1WjAQMQ4wDAYDVQQDEwVoZWxsbzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAObxHISpAtbWFlevFXhLPkmM0sor85iGyIwPx1MUMxNTwjdOMiXGq+V5eA20nbxH/+uh7P+VOxEKVPpF1qaVRaFe837GqIuBnxB7ZI1ZCci2OoMvd3+keCVoIWx01YwdeNZbeiqRUz0a43laelEdRXFegBf10gAqIjfDyDhmXbDX8mv+CBEwZCtgk/6UHlnANkheWU36QX1CD5LgnVeXKANM98fy5chnX04i2AObkB6Sgv9oDvuQPXrwMedY8n67zkcarFvx4RfkuYH1+pAORokfmiAVyc8lSWUaDyORmoJoAhR3P06AEFa3V/iJJB7Y4b6nGTroZPfIb6SbXKForXsCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAKtDsxMUx/QRWGphWehChPrX8YJR4A8O3sGeRYAq5f8xQuRhlXaTojhqjc7owIdEv1JtPZSkVb8tA6+GhQtjp+dKLHenEaz6SvAlKAzSsCAytXkIOSP+fg1ZxaJYpP5Yz3hU/cpCVGP5F0dsV7Uwf0x5+dPYzholukwflK/iKpzKvjTnlovgb5feK6Gf19DiOV3WuRbFtoygXJsP7DtlnU+4O3ecNOdBnbrLZDP5Lq3f6+nr3ASBJB1LWiCjajIaEW/aLvxS4g9gVeCp1Fw9lBXlVDnsZO4kbOrAu+GlQf9NIwqY0GmUIdVQoUaCQF3JnjxNKFtnWDPYTt7t6xzMjLQ=="
				      ],
				      "key_ops": [
				        "sign",
				        "verify"
				      ],
				      "n": "5vEchKkC1tYWV68VeEs-SYzSyivzmIbIjA_HUxQzE1PCN04yJcar5Xl4DbSdvEf_66Hs_5U7EQpU-kXWppVFoV7zfsaoi4GfEHtkjVkJyLY6gy93f6R4JWghbHTVjB141lt6KpFTPRrjeVp6UR1FcV6AF_XSACoiN8PIOGZdsNfya_4IETBkK2CT_pQeWcA2SF5ZTfpBfUIPkuCdV5coA0z3x_LlyGdfTiLYA5uQHpKC_2gO-5A9evAx51jyfrvORxqsW_HhF-S5gfX6kA5GiR-aIBXJzyVJZRoPI5GagmgCFHc_ToAQVrdX-IkkHtjhvqcZOuhk98hvpJtcoWitew"
				    }
				  ]
				}""").getAsJsonObject());

		env.putObject("new_jwks", JsonParser.parseString("""
				{
				  "keys": [
				    {
				      "kty": "RSA",
				      "x5t#S256": "pQRhNcATHi9--fTzR8y46jyKkVlQuYWwx1WtLi5i1Z0",
				      "e": "AQAB",
				      "use": "sig",
				      "kid": "authnation",
				      "x5c": [
				        "MIIDbTCCAlWgAwIBAgIIUviusM0IxhwwDQYJKoZIhvcNAQELBQAwZTELMAkGA1UEBhMCREUxDzANBgNVBAgTBlNheG9ueTEQMA4GA1UEBxMHR2VybWFueTEMMAoGA1UEChMDTUVXMQswCQYDVQQLEwJTSzEYMBYGA1UEAxMPYWFhLm1ldy10ZWNoLmlvMB4XDTIwMDcxNDEyMTIxN1oXDTIwMTAxMjEyMTIxN1owZTELMAkGA1UEBhMCREUxDzANBgNVBAgTBlNheG9ueTEQMA4GA1UEBxMHR2VybWFueTEMMAoGA1UEChMDTUVXMQswCQYDVQQLEwJTSzEYMBYGA1UEAxMPYWFhLm1ldy10ZWNoLmlvMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnFR3XnCMJyc0StJpwLOJYxXCvmUU+XRQI6mUU+enbJcWtUlQEceuMhfTMTXeodO0maxSkAiXl4ILlsXIuVo5aDwKCrc5yYy/Mjlx/yM8ZQ137FY9GW1UEBtuCDeDfz74oNK7VxLUAr5MlxxatUH9GUDrYdKO3SsQ+g0KzFeUVZYCXCVXJIJVWE29mll8s5P9H1M4Gfhew3+88YacgO5/GarLp6RIu91bqqOcoq5iSPfHFVEgHW6B6s8iaAtfHJ8gxB0Hdar5d0IhBdetUOpVk6HlHzyiOSFG1Z5db8l8wNB2/5wsqv4Oo+cV+c1Sv3OgWtuhsfL2frFxoBhviAyYhQIDAQABoyEwHzAdBgNVHQ4EFgQUbfWw2f5vOKVDs45zwoVUb0S7RswwDQYJKoZIhvcNAQELBQADggEBAIp2yacMHVT611nv7/9i/1mK9PMxjnPaZq0Skg5URz5yyy5Ey3yRAKWKJ6amnmuKR6/l+AYyJxL3eZ29/vNY5vGueX+LvBDd91kic78lPE62e2KqmY1+YxsouxTGt/G8TsIXVpSnyHT2WuNQSFTtL8TpcyMYY2i8gb4yCB+qr68S0X/btNXD0zxwMNEGLgIoNitfegJBis1GG4hn4Z7bMJBK2Bc5FErjN5RyYq8A0Kg9Bc/lxFDdkjkbDrNJBNqd0DnZdB0Bw9cosVTpo8BDOPn1UcMt3edjKjGBld+XVsBs9mdgvvV+jAMhr/9TdCz2B9oM9bI1u86CSSXAxtMO/BQ="
				      ],
				      "key_ops": [
				        "verify"
				      ],
				      "n": "nFR3XnCMJyc0StJpwLOJYxXCvmUU-XRQI6mUU-enbJcWtUlQEceuMhfTMTXeodO0maxSkAiXl4ILlsXIuVo5aDwKCrc5yYy_Mjlx_yM8ZQ137FY9GW1UEBtuCDeDfz74oNK7VxLUAr5MlxxatUH9GUDrYdKO3SsQ-g0KzFeUVZYCXCVXJIJVWE29mll8s5P9H1M4Gfhew3-88YacgO5_GarLp6RIu91bqqOcoq5iSPfHFVEgHW6B6s8iaAtfHJ8gxB0Hdar5d0IhBdetUOpVk6HlHzyiOSFG1Z5db8l8wNB2_5wsqv4Oo-cV-c1Sv3OgWtuhsfL2frFxoBhviAyYhQ"
				    },
				    {
				      "kty": "RSA",
				      "x5t#S256": "ZEFNZkxWkGCkfNedvQrg-03b6x2KN1RfZkmh9IOEJn4",
				      "e": "AQAB",
				      "use": "sig",
				      "kid": "authnation01",
				      "x5c": [
				        "MIIDbjCCAlagAwIBAgIJANpibpKX6nXUMA0GCSqGSIb3DQEBCwUAMGUxCzAJBgNVBAYTAkRFMQ8wDQYDVQQIEwZTYXhvbnkxEDAOBgNVBAcTB0dlcm1hbnkxDDAKBgNVBAoTA01FVzELMAkGA1UECxMCU0sxGDAWBgNVBAMTD2FhYS5tZXctdGVjaC5pbzAeFw0yMDA3MTQxMjEyMjBaFw0yMDEwMTIxMjEyMjBaMGUxCzAJBgNVBAYTAkRFMQ8wDQYDVQQIEwZTYXhvbnkxEDAOBgNVBAcTB0dlcm1hbnkxDDAKBgNVBAoTA01FVzELMAkGA1UECxMCU0sxGDAWBgNVBAMTD2FhYS5tZXctdGVjaC5pbzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALjsWJhewp2l5kcPgOd/YPjSpn2qnXyj0sYWP/7O4wEXK8tTGqzl4QNipWR4Xxzc9HrhMRPfZ0IQlqU4Zx6knuh52iXu0TKxg7A2kAf7p0T3jedpvKRcWv3bd7OQ3r6CFoyHWZoyw+rl2oxs2LtXfgKanlyFMj76JLz15Z4ogIGy5GPJ3DPjmNhvUYeZ2E7jB4aMK6QRIVO4RC8l54W6oVRwNxFDe7SpgC3GApbq6zbE/B9u/og5si7JnSOucq6aVIxJTA8QTbg36dJZ+rLCYL/IQwnRnbRo3Ly+RMuf2Dj1pgQspEtOnL9jsxm3iFmRiR5DHihvtjms4qiJQpJqPZcCAwEAAaMhMB8wHQYDVR0OBBYEFJKtD/XwTFyqAwMkRongBAsWMLhzMA0GCSqGSIb3DQEBCwUAA4IBAQAp73UivwRtZdSsYdBqKhf5BBVrWwdyCYdlQCBHjKVAbVfI+jSODh9CgeV4qJGYEXrSLHi99gZ54Zip9EBcLI6OHSM0OKwjFWtLikig1pWTFXS6EFxmb0V2mRBJI5+N8PXwErzZwwmkS07BO8NOVEc/j5uEbOXZY05R0It2sI+ukfLJWxP4Q94lToSJQYrvOz5kSO9yZMwdlcNF3hzRFl7RjAqDMHwYo48sv2Z9t3bV15pC+jAzI02FdkKrEkWu0DwOa7o9NZgMgWTKQ7wkae3T7Y2ED0JhwEfyizH8nbKXpYKIdwrLXGQT2MfIRVRk4mLRg+sWA4ImkD4APxZcQAdN"
				      ],
				      "key_ops": [
				        "verify"
				      ],
				      "n": "uOxYmF7CnaXmRw-A539g-NKmfaqdfKPSxhY__s7jARcry1MarOXhA2KlZHhfHNz0euExE99nQhCWpThnHqSe6HnaJe7RMrGDsDaQB_unRPeN52m8pFxa_dt3s5DevoIWjIdZmjLD6uXajGzYu1d-ApqeXIUyPvokvPXlniiAgbLkY8ncM-OY2G9Rh5nYTuMHhowrpBEhU7hELyXnhbqhVHA3EUN7tKmALcYClurrNsT8H27-iDmyLsmdI65yrppUjElMDxBNuDfp0ln6ssJgv8hDCdGdtGjcvL5Ey5_YOPWmBCykS06cv2OzGbeIWZGJHkMeKG-2OaziqIlCkmo9lw"
				    },
				    {
				      "kty": "RSA",
				      "x5t#S256": "ttsMvLlc6h0hXfhe3lw7VAc8dMviMWc8XCNTow51izI",
				      "e": "AQAB",
				      "use": "sig",
				      "kid": "authnation02",
				      "x5c": [
				        "MIIDbTCCAlWgAwIBAgIIPeupkk9kmXMwDQYJKoZIhvcNAQELBQAwZTELMAkGA1UEBhMCREUxDzANBgNVBAgTBlNheG9ueTEQMA4GA1UEBxMHR2VybWFueTEMMAoGA1UEChMDTUVXMQswCQYDVQQLEwJTSzEYMBYGA1UEAxMPYWFhLm1ldy10ZWNoLmlvMB4XDTIwMDcxNDEyMTIyMloXDTIwMTAxMjEyMTIyMlowZTELMAkGA1UEBhMCREUxDzANBgNVBAgTBlNheG9ueTEQMA4GA1UEBxMHR2VybWFueTEMMAoGA1UEChMDTUVXMQswCQYDVQQLEwJTSzEYMBYGA1UEAxMPYWFhLm1ldy10ZWNoLmlvMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm5j9yEcnAEH7TNGj1Mnyzs9Pdx11xIR25hACeU+dv0xHqSw7XvltRFcCUA6cErw1ortbrlfiuiiE71Q7C6zQ7rV9miu17P4KQMPSJ4/fwL5EbQEGYXTV+AHZtVWMLBNEJRQzNFVq8cMDKgEdiA0l6bobIeIYVYwN7q66/gFxNZNjP2ux9pGk4uj9QPxYQjIefoC7tvsTgYWrGoCwZK0Mv0X0D0HoE4XujutjdDqiEif8Kzmg1BghFGkwO8hrikF9d8o+f9INLl6pG/LbiN/FpWKZed/i8KnYCUQp9mv56YJCOwDFqIHhf992Dgu3tIhD7yOqVukY6UCbem9qYfaMeQIDAQABoyEwHzAdBgNVHQ4EFgQUjw+shMWiGgXNrueLHBLV+4mBHq8wDQYJKoZIhvcNAQELBQADggEBAB4LG1DvbQvMD+n0WJH0gmux76o1EJwy/rL0eKEiZ6KObshtjvrMwqV1QxkS5IPucCgRFkhGmJeEjxxvYuw2xuzI6ddXCKVIehc6YuwQZK/1xOG+givWYSJAytdVjxSZfmK69qFwn2vG111vpmMKvS2iULN4Q7QG3FG3BYTmyJ0wRQEHVSGb1OP9+R+tN5wx/U7mucD1YneM2uZfyGpGTHNGeu8Zwxl0cd/SDBjJwewhNpbKKU+H2nF3VTKdjtWRJGksQ9f/a7xw55Z4dAgcWUkAO3yWA9hlYXJI9qQ65752Gi0V9NaUKunUcKswpth6FPUyjdW02ROKm1W7w6L2fD8="
				      ],
				      "key_ops": [
				        "verify"
				      ],
				      "n": "m5j9yEcnAEH7TNGj1Mnyzs9Pdx11xIR25hACeU-dv0xHqSw7XvltRFcCUA6cErw1ortbrlfiuiiE71Q7C6zQ7rV9miu17P4KQMPSJ4_fwL5EbQEGYXTV-AHZtVWMLBNEJRQzNFVq8cMDKgEdiA0l6bobIeIYVYwN7q66_gFxNZNjP2ux9pGk4uj9QPxYQjIefoC7tvsTgYWrGoCwZK0Mv0X0D0HoE4XujutjdDqiEif8Kzmg1BghFGkwO8hrikF9d8o-f9INLl6pG_LbiN_FpWKZed_i8KnYCUQp9mv56YJCOwDFqIHhf992Dgu3tIhD7yOqVukY6UCbem9qYfaMeQ"
				    },
				    {
				      "kty": "RSA",
				      "x5t#S256": "HsPEDMf8ZT6s0dTUtb8fMw6GZa_U3vWNGhsIDHQeWH0",
				      "e": "AQAB",
				      "use": "sig",
				      "kid": "authnation03",
				      "x5c": [
				        "MIIDbTCCAlWgAwIBAgIIbPMO+CRt+UkwDQYJKoZIhvcNAQELBQAwZTELMAkGA1UEBhMCREUxDzANBgNVBAgTBlNheG9ueTEQMA4GA1UEBxMHR2VybWFueTEMMAoGA1UEChMDTUVXMQswCQYDVQQLEwJTSzEYMBYGA1UEAxMPYWFhLm1ldy10ZWNoLmlvMB4XDTIwMDcxNDEyMTIyNFoXDTIwMTAxMjEyMTIyNFowZTELMAkGA1UEBhMCREUxDzANBgNVBAgTBlNheG9ueTEQMA4GA1UEBxMHR2VybWFueTEMMAoGA1UEChMDTUVXMQswCQYDVQQLEwJTSzEYMBYGA1UEAxMPYWFhLm1ldy10ZWNoLmlvMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs1t1HxtM8/rANLGktnj7mnn4/ShPJ78YZMt0koMpOwYCDvtUUPhGVpm0XMflrz7s5TcCkN6t0Sq7R/i0FrNpDodJMwDXGJskmYLvdsAW9mtA52VlwV9ha6WbBdwtECnAHBYlvyWIxs0Py84uy3PrhNXGSAU2z82lYxCQx6yyINi+0M7fuvth1VNq6Mt0dJKBD6FlMdWCG/lZUAbgIdUFv4ZxlIehqBBH//4kZgnibu4z47/Cw0VRb5z/QV/EiYyT224jzivyWSP1klTBqJt1dk+B6C6QTrJLrH+TsOSXdODXgSnRGi36RUA75b4x1jXpsA6T95XPbzIC2BOLqfriJwIDAQABoyEwHzAdBgNVHQ4EFgQUmdTi1CLz6RzuFY4MMNTdhGfYVR0wDQYJKoZIhvcNAQELBQADggEBAGr2h7+SwmfihMIp43uW9DpXdNg8BOjCIOPLPA+Q2mGY1zUcbJv1vh+6s0S+4wl6wgfguekjF5Y7ty+aT8KmA6TA14VfWaKDU3CPrqBEGoJh74K3ycV4OtzmjoMoHm3jE3qPbbz2n9wptktVSzHv9my7ajPq95V3VZy2ADQnKzKEhH0BadDLiY3PYQ99vjTaSoB87/nYf81f7DCg8Ax3lUS2mo54rPVjQucddSFvtiebyJbMBuKaRUL0f+85KRsJAbvbYY0nxdQFawzqCCsahIBXRp8NAtfCsTHvhH+O3aNe/Z6ItMzb+VnSuTw3587QVh/1OWDrPk4w+t3drd9BALY="
				      ],
				      "key_ops": [
				        "verify"
				      ],
				      "n": "s1t1HxtM8_rANLGktnj7mnn4_ShPJ78YZMt0koMpOwYCDvtUUPhGVpm0XMflrz7s5TcCkN6t0Sq7R_i0FrNpDodJMwDXGJskmYLvdsAW9mtA52VlwV9ha6WbBdwtECnAHBYlvyWIxs0Py84uy3PrhNXGSAU2z82lYxCQx6yyINi-0M7fuvth1VNq6Mt0dJKBD6FlMdWCG_lZUAbgIdUFv4ZxlIehqBBH__4kZgnibu4z47_Cw0VRb5z_QV_EiYyT224jzivyWSP1klTBqJt1dk-B6C6QTrJLrH-TsOSXdODXgSnRGi36RUA75b4x1jXpsA6T95XPbzIC2BOLqfriJw"
				    },
				    {
				      "kty": "RSA",
				      "x5t#S256": "nOmjF5rA4ohhc8ETjoI8Qj6G7arpEbWhOzaNY7fOie8",
				      "e": "AQAB",
				      "use": "sig",
				      "kid": "4095b867-e9af-405d-b417-0661b9a68d9c",
				      "x5c": [
				        "MIICqTCCAZGgAwIBAgIRANlvuvearUFCp+jfVXQSRZ8wDQYJKoZIhvcNAQELBQAwEDEOMAwGA1UEAxMFd29ybGQwHhcNMjAwODA5MTg0OTI1WhcNMjAwODA5MTg0OTI1WjAQMQ4wDAYDVQQDEwVoZWxsbzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAObxHISpAtbWFlevFXhLPkmM0sor85iGyIwPx1MUMxNTwjdOMiXGq+V5eA20nbxH/+uh7P+VOxEKVPpF1qaVRaFe837GqIuBnxB7ZI1ZCci2OoMvd3+keCVoIWx01YwdeNZbeiqRUz0a43laelEdRXFegBf10gAqIjfDyDhmXbDX8mv+CBEwZCtgk/6UHlnANkheWU36QX1CD5LgnVeXKANM98fy5chnX04i2AObkB6Sgv9oDvuQPXrwMedY8n67zkcarFvx4RfkuYH1+pAORokfmiAVyc8lSWUaDyORmoJoAhR3P06AEFa3V/iJJB7Y4b6nGTroZPfIb6SbXKForXsCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAKtDsxMUx/QRWGphWehChPrX8YJR4A8O3sGeRYAq5f8xQuRhlXaTojhqjc7owIdEv1JtPZSkVb8tA6+GhQtjp+dKLHenEaz6SvAlKAzSsCAytXkIOSP+fg1ZxaJYpP5Yz3hU/cpCVGP5F0dsV7Uwf0x5+dPYzholukwflK/iKpzKvjTnlovgb5feK6Gf19DiOV3WuRbFtoygXJsP7DtlnU+4O3ecNOdBnbrLZDP5Lq3f6+nr3ASBJB1LWiCjajIaEW/aLvxS4g9gVeCp1Fw9lBXlVDnsZO4kbOrAu+GlQf9NIwqY0GmUIdVQoUaCQF3JnjxNKFtnWDPYTt7t6xzMjLQ=="
				      ],
				      "key_ops": [
				        "verify"
				      ],
				      "n": "5vEchKkC1tYWV68VeEs-SYzSyivzmIbIjA_HUxQzE1PCN04yJcar5Xl4DbSdvEf_66Hs_5U7EQpU-kXWppVFoV7zfsaoi4GfEHtkjVkJyLY6gy93f6R4JWghbHTVjB141lt6KpFTPRrjeVp6UR1FcV6AF_XSACoiN8PIOGZdsNfya_4IETBkK2CT_pQeWcA2SF5ZTfpBfUIPkuCdV5coA0z3x_LlyGdfTiLYA5uQHpKC_2gO-5A9evAx51jyfrvORxqsW_HhF-S5gfX6kA5GiR-aIBXJzyVJZRoPI5GagmgCFHc_ToAQVrdX-IkkHtjhvqcZOuhk98hvpJtcoWitew"
				    },
				    {
				      "kty": "RSA",
				      "x5t#S256": "dSct4y5aNfwvKfhLBO3EGloqf1TS6QjcT3LCCZLYHUA",
				      "e": "AQAB",
				      "use": "sig",
				      "kid": "2bdbcbb2-20f9-4bf3-802b-3cfaee63eaa7",
				      "x5c": [
				        "MIICqDCCAZCgAwIBAgIQJN9ocgtVQO2+dHlV1YM3TTANBgkqhkiG9w0BAQsFADAQMQ4wDAYDVQQDEwV3b3JsZDAeFw0yMDA4MDkxOTA2MzNaFw0yMDA4MDkxOTA2MzNaMBAxDjAMBgNVBAMTBWhlbGxvMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAp+pLPzjFWV6RVB7O7GlOJxSQ4HB3q6/Bjdu1epQDitCDUN18Y9jEH33fbxFEUXl4t10A51Dq7X4q+fWPLO8Y5Q5r2VlsGjuFz+ACZYPCM29GKlJVXXyur+QOAxGWZQpHTDrizqcJexxQTD7igxDaiwsTK/GbCRbi0uv1hDJgUlZ2Vhn54cou2wO+m5lcZmGTktItHcYUJvveMLn2FVpS2T9yLzkktXYiG6mD7BkJr9XyYMCOi+yh798c+W1IGh7AfS0L3oXOZ26sRUK6vn/UMax8k9BMydl6kuUzx5plWN0wX2DadLl1uohQxzdtQEpNNAole3B8j4FTunCxfM0V3wIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBvHwODslltpsb9bCLvd62p3CjjAzDkY6G6yUiGltay9FXSlGP4dy+Lfid2aGGtBa9pc/GME4H1vLI5kRkeGqTD+9Ua+MTunP5NyTBMcr0H+DigbkAB+RKhTm3v62zcuf1DyBgLLD2jrViHWh/h0Wv7NS0NxUvEQH406MWmMVF+LDXSCN68g2oFB2Jc07PeFlAaM1fQ47oHThzrRr6k2JKycRyeYhl5XJtnepXqpFI8BxKozsCcVw7/SJJUmlhh9ZxZD7fFEMbdJYPzqWs2V0f8XpmO4N2F+qgCVNj05OdaNZcMa0BteQ7Jc8j8L5SOK78qZLfEOIy6aa+vWMoihsBb"
				      ],
				      "key_ops": [
				        "sign",
				        "verify"
				      ],
				      "n": "p-pLPzjFWV6RVB7O7GlOJxSQ4HB3q6_Bjdu1epQDitCDUN18Y9jEH33fbxFEUXl4t10A51Dq7X4q-fWPLO8Y5Q5r2VlsGjuFz-ACZYPCM29GKlJVXXyur-QOAxGWZQpHTDrizqcJexxQTD7igxDaiwsTK_GbCRbi0uv1hDJgUlZ2Vhn54cou2wO-m5lcZmGTktItHcYUJvveMLn2FVpS2T9yLzkktXYiG6mD7BkJr9XyYMCOi-yh798c-W1IGh7AfS0L3oXOZ26sRUK6vn_UMax8k9BMydl6kuUzx5plWN0wX2DadLl1uohQxzdtQEpNNAole3B8j4FTunCxfM0V3w"
				    }
				  ]
				}""").getAsJsonObject());

		cond.execute(env);
	}

}
