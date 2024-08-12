package net.openid.conformance.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class JWTUtil_UnitTest {

	@BeforeEach
	public void setUp() throws Exception {

	}

	@Test
	public void jwtStringToJsonObjectForEnvironment_A128KW() {
		String client= """
				{
				  "userinfo_encrypted_response_alg": "A128KW",
				  "userinfo_encrypted_response_enc": "A192CBC-HS384",
				  "request_object_encryption_alg": "A128GCMKW",
				  "request_object_encryption_enc": "A192CBC-HS384",
				  "token_endpoint_auth_method": "client_secret_basic",
				  "response_types": [
				    "code"
				  ],
				  "request_object_signing_alg": "RS256",
				  "grant_types": [
				    "authorization_code"
				  ],
				  "redirect_uris": [
				    "https://openid-client.local/cb"
				  ],
				  "jwks": {
				    "keys": [
				      {
				        "crv": "P-256",
				        "x": "40Y6UQdg7xTNrSdTPQgB6dy41O1yPqlEDNkBHEDHy4Y",
				        "y": "JzDE35A96tiSlgO__Cpp9ai-aNQltnLL_wGoM6uw_Pw",
				        "kty": "EC",
				        "kid": "X1ehlN1mJoOV6ysYebXpDpmewbwEcCCNMcdzHM8W6pY"
				      },
				      {
				        "e": "AQAB",
				        "n": "yO0ErAp5-XLUPvXdb6dcgYEmYOJl6LJBKh3B87T9SMks2NAmYDvem7aO7_yI0c8n269f4YVLO-MBpqbo-mT9Z9LINZx7h61PTZLryrqOCw0hfNjkA2j6Lfx9HG7br_mLtq4cn-925DX_7n1y9LCQm98xnzRAedv2umtE3fGwCOU-lcp6of7t4F1lHOHntvHQpAi9KwAXtIcvYKQeDtkGJpfTPO2FZht-dRYOUuhY_GZIr35BuP1u2g34wsL2aVb1AbsrL6-AMwqwJiLfFVOEKNmK9PxXHYCqrPvAssoTUASZLwyPyukagnOOlTC0VI9VLol9U9clNE-xse89yyMN4Q",
				        "kty": "RSA",
				        "kid": "i24wz_eOr1VzBO2gpkphfaazZesJ7sQGGrdPanbqIqw"
				      }
				    ]
				  },
				"client_secret":"secret_zQzKMxfQGfCRTNvdBRlpjSKsoEOFqjRTneDsWCbPuxUMUnaaSI9256088148(\\\\&&!",
				  "client_id": "client_FdduVltZDdgOxBP86950#*$}:"
				}""";

		String requestObject = "eyJhbGciOiJBMTI4R0NNS1ciLCJlbmMiOiJBMTkyQ0JDLUhTMzg0IiwiY3R5IjoiSldUIiwidGFnIjoiUEtJcFM4" +
			"SlJtenp6bVZ6eGlOdTVoQSIsIml2IjoiUkhkeEFWblhKMGlFRU53TiJ9.JmDJpeIj0zZ1e6QUs2yIKm6GDdmN_x1esbvhEMn8vKcsbqN0JA" +
			"R7edKyAGdxdbNU.aik3wyr0Nw1C8i58J422Ug.r1J2mmMDoc2ZEbR4SqqC17P6RLR_AdEr6cwYT4xYarjYYj49BgjY9vlPfPPynh35T9irJ" +
			"OL98y86vOYW1depk24B-ulz-Pw_DCNeOjvIImdXKQPY-8NzXyRISAa5Pw6QQy5FGPxzUz6ZWRquR0eMhiz4PoN_d9Ffx4xjkUsg8zhZB7O1" +
			"2eJdbAidN2MT0PMXpXmyyR25ajcZ2OnPJ-qX6yjjhP4hL1UjmwuQXq62VPtZq9beJw91sPkIPPeJpbOuYTOjnaOavHeGB49TR3TwWJiUclP" +
			"4cdKQ4s_TP2fKn1h9dFcZRk_oVzwyhhSscLtSF2VCMDrLR0nyCECz1WETv35pDIQ_5Cfekff3H8eJmhng-6CrEHK6RPp8wlXpCFS-TD7dDH" +
			"0h0e9vbmo6bhK4YEypklpDjZK8fHoBMMEbL-7OnsGRTcOkeiNJ_xh_gjj7Y8KODx_ffp4VtzMi0mU-bZeIMtYst5an0G-AHfWAvxYxu5PSJ" +
			"_Nsmdnr1vx1UqfMH6g_06NqWSvQoUEASdwsyjTGJvzhyidA5HmC4Y8FrwX1vQbczvyoG5VidIMiSwIfooiwd_Tscacc1JaM7pWtLWW7ssDU" +
			"Dd5jMhvN4xfXvHGgVhvAZ1VC87OwvQzYDZ01NJES-QuzQlAMpGEzeRYzClNEWlfcrirJAsuCwyTTsGBO1JNckGzanFlTUxFSJs-AAMkI-7b" +
			"_101NGzVAc5I4cE9l2GfRiQVRKaahPV9T6yOmjr98QrGTONGL0P7ZWUN15rP3rUmzNQujgLwADaCITWckgG-MERmCXtGSXTPtnNp6EmrHLs" +
			"mBJ_A5mPbnv7bHHSNkDuKYlNL7_R0CFFZQwfIkVlap_rrKx0BT-f8k7x4DHli3I0leHvYX7RNUEUY5aD7cl9MTe6h3Eqg9nhyJd9zZ9lK1T" +
			"YWkFP4E1XFz9YtOkqG7skX7tw0Lo0flAoDDTEUdQKMR1DbVepIn5QlbJm0I-dvUlk62AfoxQBc2K7eF-2DaMgIAFvqSDGfJt_sloesF2AHM" +
			"tx14I0wJt-50tQ05hnqo7XVV7K8SboJNPx6jvkr-9McF143WpcMqx4GFvo-H0O2JjvGciK9A1X7FcQHjaUUQdx43E47B3TLOceauUt5bTHL" +
			"g7E5vFKVEgThD5OmSr5Ro44YXTwj0gRyiPc0IDult7OQAcjAhdAdjXT9VQ8IdQle-ZIwt8l8q1uAKx8-gZk_DZ7f98TU7hBgue7JNNoFoge" +
			"gbEZWAoLX70Z_mQYQEMPWmXwucrL2NnLkvxcG9s2yBhnH4DtydWgMlnw.aGvz3KOjer1gM3cxtVcFWtbN5M0VObxZ";

		String serverJwks = """
				{
				  "keys": [
				    {
				      "p": "0A5Z094SpEmZQpC24_rTMI7P3Q2_fMySSzYjFOkl41YVLYTru3Z0jWGFgPokFj0fP7xEStMGh3w7XqhNoAyIYmWqzYrDyvfPF2q6t_7FHYfOFv51snJCnDGGFEzSG7WF_748Bjk6FqhFBbThB5QUlRG8-LDUq-tgueX7OCHZOMk",
				      "kty": "RSA",
				      "q": "zPgQlHvsO7AMIEcLdn4BKPO_-9MdhvrBrNluDVvKMJcWNb1YbZ35J8Xsb09iotreqqZGoiBx55IJeFLiVUGlhnmUn2u1YtLvSj8RuLi13UUu1fwA4mfIfJX0G_Me2-e5b9oaxLYNXqsW5tEneXAgCwZoOTSJQ1nuaE3ygfelIvE",
				      "d": "m_udylhCSi26vuMllKL68PRWZ22Nro111Vm_7oqySNYJpz5rYiEYtdrP3on5qIJWc_G1GvC_yf54f45WNkW4tTmGAkbsc2oE_gVLDnzb1NhnUU83fdYHSNtUV5oBYJDVlc4uIC3Vd4-Rkc3a4R_hNP444eNzB1oCUt9RqZs0mexjmkTRxuy3C4UMx7S2k7DYb4CT2W_7IJdvgCzxY-9Cnl1N_DKO_50yeCEgbNXoZKxH4iBZKloKK2koiEXwkeUe5Fj6hVur7ISvxTON_rYXtoMhscqn9Nw2f_GsoSK-_kPwzMbGgnPvo3vbDRJ_4FIpt3I014z1ipp_w_u0eX34gQ",
				      "e": "AQAB",
				      "use": "sig",
				      "kid": "0038cf07-c6a4-4f55-ba97-8c2f9d921171",
				      "qi": "ja8qlHKflUj2JKvviDJb4zMHtfJQNyf0aflD4Pq25x_q2lcA3cA_rTmiol8YvqKCf8iiJ_sQGfsgVhRPdiwK3sVshK84QzLK892HIouBmx222jF4MB4h_ffqgWoHk5_qXqbncTHaChBRUyy7APmRDVCXnx2KMuOsZ-lMiFaSylY",
				      "dp": "ynWMmA08Gr678vib4JHOPSxbvgKI2kq_-Yx-6vROOEEmbpswQcXteT7zCSVhRHrKE92Cn0VmzyTnKR-iGiLX_NhuM4HbMYGf9muXs0CqVCg5Nkr8AWAKza-rh8mRlAE6mjlmc6_whOfN8tWnPRauSLlwKJj6-ykbgQr5QhA-L3E",
				      "alg": "RS256",
				      "dq": "UfT6xDSM8AzjvGBMABRAKHzWjj4LN3a6zH-gVq0WOrmfAtv0KNia0MF-Wb-3ZAD6OegEpD0u7nZhXQBfSdHW-t9QgzJRM_O-BMUVM_R_m7tXD-8U93KiItbe5fIfq7SLXGMsgu18iRcGMEL_crpXxbDJKd6M6CZkgsZwKnG4UTE",
				      "n": "ppUK9WzWDD1X6UOi8ENHNqhjmcdDIEOQsjXCyWzey81ESmfGolYn5sBYE7KgJG01cZ-ICyM7fp9THqTg1hqnBOCp6u_r4saLwgTrLbMkslAWZoHh9dpEpuR8HBOTzH9-aDKkqe9g94PbchQ38g_FJn9eEyHoF3Ln8AVs4yxsqsvzqgDh5zpborW1_qhPpwlBNBKncMZ5QhJv9cE493G79CTk20mvPFoPRETxDw3Tf5N5iS91tUfetPoHoemS7fJxOTiFY5hxM7d-z1g1CeDvmSi6hbNwsbq_qEUmD9cA69L_v54FK-csZzn19rHCT1dBETloWfAc5evDTfprP5YnOQ"
				    },
				    {
				      "kty": "EC",
				      "d": "JncnvipnUFk0_Y__kqPAewNcSHB_BIv3urhpGqZyT08",
				      "use": "sig",
				      "crv": "P-256",
				      "kid": "beb78d48-6691-482c-ab42-689caab0a045",
				      "x": "580gpdwz7xPgc6vgmQ-kq9_sBULD8BogiDHBe7XUxRU",
				      "y": "2NwyM1qDTYcU2g6G7lfcrsfEvbs1Xd6tmdMC4AzWllw",
				      "alg": "ES256"
				    },
				    {
				      "p": "5c6Y30Q80z5IWUSEDWi3udMsXBQmYy__E2wycBXVSyepOdoAhPzESdKfLYFvAsumkEBbjLlkDe4hTGyE6Nil3eCaABBVeOzhhzCRvdRYhkTIq8pf7uRK3ItvirTKh-ScJxq_hOMYApv2M6NeaShFBlkhy0_PFCo4rOPgBaRYtTU",
				      "kty": "RSA",
				      "q": "pLof4V-76YJG8T3YklmfgbxhisUGq0QvDWyJ0moQjB_FzXKbKWPNdlGyedKz_aM4YUS44xHZLDT2GkIRhrvHjBtCOPnXUpXUxrnwwU2Hv1e6uwanbLAkVEOuDkRtHoPQacb1BR8Z8lpn4ON48nrbyUS2oHzO46D53LtT5cNIb2s",
				      "d": "cepDPiBVPoH4DFMJHDO9QoAEBIKF34dZmnrhE_oqIlG0Ub0fafaGh_gyNampVFpOCu8SU1uPPINo8b4_1D0qzKFNd2tt3pK__wOhgjQKHYdR_SKScZDFZvEnGKnJtNQS9zxU-oXZC_2rEpuKBZeKneP2EVfhH33IqSwIQ1y278l9oUczb6aHB91zCMjx_lFRux_FltxePNOqdKoVJrvsltkAw2ehMgkYi32VK8a6c0nVrNEGKiwFa93xh2uCe_Pkg2ncC7iyPlsv5wOT52opTFW3iMzR54_K_SIG6XRdDJx1FAgTl83OWPGwFCC-jGcnsXkN8nHKlXpJsM97aSJaOQ",
				      "e": "AQAB",
				      "use": "sig",
				      "kid": "ee9c19a7-3a36-4c83-89de-399b993e2226",
				      "qi": "NypC8ZHZG2uqEnycHKB9s21lvARuyCYMSl7zg8HFSVR65fifOhD-07YsYD2uDO21v18u91bPre84-MKArat90wUQa-Jl9N9aensR66wciKNxVmNJSZYtmbNBVCdOnXEmClUIaaFftiXaB_c1S2P6S83U60PQlie7pifbhN7D8bM",
				      "dp": "0PQnYGsle1ZYZoK8J40d8WMoJJlVxj5wCnMI8Y2IoYtwfd_RNRq4R_-xKKlWDQ6rZuJ929j-NwRdVqYdu7KyLqr4nDI95XROJKRvAFJCI-QLKtkanZcK0roeM9rrhODDc3MIzYXH4Sd6l71mLOumx3zoE3t_6O_zFJEmszqaRjk",
				      "alg": "PS256",
				      "dq": "ETHgjA0LN3fC4gG7rqMuVEOOxwaECLKoWrVeuZkauxXB9w0khzCEzPoAMFk_MpWsF_MtfX5qdgPdQKZu3-qQDN73jaz-vf8n48qbCAKIzf9hXY53QZAtaqJZ8-FOpqXJxpDfGLbDZZnsR_xWrrEGaj8C9UOx8O7nxhPD0TMOR5s",
				      "n": "k99uqG72S0Y5yUNez79OcEJ6dTw8Wca8_bIAaPU0igA_Iu257f0nH034winJcLZ5GkbIMKBoIWX46skZKv0W9CFVropDCJPaD3vXm040pqsMsa1WNMf1nSqbyd7hcUqkuGy8PFBlvv-Vo-aHR48wqOevqvcvP_XRqNHEs4EvfSVU5qoAKhWFhR6PiWJvEUBPBNmqVr_fGGWEGwQempMe3_sjQCLsKTMhfEJdMB_uF7_CTO-9lpY9WKD4BIji_o1Z4ESSwc0Cy7X54HJwBEys0jYNyMVAK4bjb6qfJE3D-XoBrg6HyXt8kc6rDU7TM_f87PnhDf27TsoO-CcBfY24Jw"
				    },
				    {
				      "kty": "EC",
				      "d": "2kzL_53BrBTQJzbrZO4t0yok2xRf88kl-_sPv85Cz4U",
				      "use": "sig",
				      "crv": "P-256",
				      "kid": "80468a05-8aa8-4ba5-a258-3ab7139ef714",
				      "x": "7hqy8tW7fLTBeUjCzr42rmhESDnibJkVyAEpnvevlqM",
				      "y": "d-1aIG_gSw1vFsfOW9UHWtdYk2Pc2m48qyEIQGc7yzg",
				      "alg": "EdDSA"
				    }
				  ]
				}""";
		JsonObject clientJsonObject = JsonParser.parseString(client).getAsJsonObject();
		JsonObject jwksJsonObject = JsonParser.parseString(serverJwks).getAsJsonObject();
		try {
			JsonObject jsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(requestObject,clientJsonObject, jwksJsonObject);
			assertNotNull(jsonObject);
			assertNotNull(jsonObject.get("claims"));
			assertEquals(new JsonPrimitive(1579726461), jsonObject.get("claims").getAsJsonObject().get("iat"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	@Test
	public void jwtStringToJsonObjectForEnvironment_ECDHES() {
		String client= """
				{
				  "userinfo_encrypted_response_alg": "A128KW",
				  "userinfo_encrypted_response_enc": "A192CBC-HS384",
				  "request_object_encryption_alg": "ECDH-ES",
				  "request_object_encryption_enc": "A128CBC-HS256",
				  "token_endpoint_auth_method": "client_secret_basic",
				  "response_types": [
				    "code"
				  ],
				  "request_object_signing_alg": "RS256",
				  "grant_types": [
				    "authorization_code"
				  ],
				  "redirect_uris": [
				    "https://openid-client.local/cb"
				  ],
				  "jwks": {
				    "keys": [
				      {
				        "crv": "P-256",
				        "x": "uprVGDMkBaQOmCtO6TNHaMz4hQ56Al33Q3xVLFfupM4",
				        "y": "4N8A2x7s974FUJ_IF6Vtsvp5H9t0lS_6P6OVl7c8bB8",
				        "kty": "EC",
				        "kid": "9sKaAJIDOiQqWM6SiE0iOdQtKJRXDIs3F0KxVmPWlw4"
				      },
				      {
				        "e": "AQAB",
				        "n": "ywcJKD5mMVQeAarIVUJtzU6mo8JCjT7k7fDhoZMrl35NWYZR_z38g6L6RrGnXURbZ98Gph6DZc-EIKDpAYd5aPMPtdj5gAQ3ZmQSASgjJpYVKiSYcg1eaAFe_amF1ptjM3G_7GmA0RNzdFOzGNA2NUF-SNcJUdmKIeygmNL21d-H2EN2mjKs2OPhzHHn29rQyAxlkhxhlXKcBBF-4TL8aHnqOR0Fos2cmi3aoVKPoxU_V0HJh3wzMZnYVog1JwPAhE_5oMi2pC0rQTDtLyAs19g1TYl1h6Axqp1Lepz9OzDNnjMmXEPIz4yVr_XiA7jgalro0WpGdAb6J0v4uZ0zvw",
				        "kty": "RSA",
				        "kid": "ffL0kf-Arvah-q1yK7eyncx0FbYnw9OcjQlzkIGhI3Y"
				      }
				    ]
				  },
				  "client_id": "client_mtBuQNWcTzrKWEm39725\\":*'`"
				}""";

		String requestObject = "eyJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTEyOENCQy1IUzI1NiIsImN0eSI6IkpXVCIsImtpZCI6IjkzOGZiMGQ0" +
			"LTYyODktNDVlMC1iNmJlLTUzOTQ4MmY1MDIyNCIsImVwayI6eyJrdHkiOiJFQyIsImNydiI6IlAtMjU2IiwieCI6IndvYThLMFNPYU1kLVp" +
			"GMTMtaVcxcHpoaUo2eXl5M3h5UmV3YWZhenZicXciLCJ5IjoiZlhXYXJHQkQxemFkelM4ek9qMlZnRlA2ZTFRMjFRY3dBcm1EVzZoSFFkYy" +
			"J9fQ..2mWJNHPgcvHFXuWDl2LDsQ.97m2mY3FxRNK7ygNZVHBb3SHKG-91J_lXEHvL6Mq1p5ilOU7f6Moumk2NOTByzvZwU9ij4KZuOEXOx" +
			"OL2PGBfKa-mpaDLYbgG2MDm5N_hk-ulk7OnvcE_-UnHYhA8lbmgoZiPR3zk63WCiBIzlFxQMQ1BjF9zr3e-Qilw2PilnofV74s-qtBFRJ4T" +
			"7tQ_rg0rRfOVwrxmtu3ktNy4l6_IKcfBnsXmRhxlGFisaWdioZm7bK_delmmMeR_A2zmfD0WX2o4dA9DFCVJyYrFd1aKAWq-q63Z97vMI6i" +
			"q97cnrgxVYQk0TuBGFOzZ5qfntpBDXg0lq7Od0s-UqFLVVuXEgG3US9celGgxXNYl_6WZzV-wuYyygbsJj1twgPuatTA22iI8iX2IBo3mUA" +
			"6OtYYzNb3iXLEBET1z56MICFFcpzR9PsH6cWq5V1GhO94VnWkCeM6FgnwWxoY-JE42nM3mkIyl-w81LacFZDiZMLX5nhOXOtOfGxervWgyQ" +
			"xtYyduN-XCHrd2yGvey5Py8fNqxv34TnVDKlSwGUuMuxbRldzzsuuoPnLSfB-aBwY0BwJUxqhC3W4utoEwdnQdX1eWCkG1lEz67Fuf8kcqI" +
			"RDLq0jGw0i2JWvjXmq3YgI9TvUvV1F6fTcvpZOP-kVBQPw4ixd1btp6IpC3ar3GvGTB0qGGYl5M4w6Q9VXSZhHeqVfAVEPvjOu3yUwiRrNx" +
			"v8w-9LN2bfttbv0Izh4wnhz48_Qik8uw24w5RyOM9Ii3823dcNiDfBIW8wVYb36ceSYgVrLdsJBm7b6v6WvGw6v5-WzzaFDL7JKwzX340bW" +
			"z0c4kWNrgLJQdPBs6G1CtKpzgHdIdhTLjzcYQfapvXlFUPpBJZ2LlglBH97AdKeDL3W2oqpiFV120Yb3fZ5FPT3fuhh4pdT6hILgUo9CYNC" +
			"saKWwgbWyTkFg8DtOxIuJ4E6mC7gZGf3juDqjkjRPtOWooANgyH9DxwULZisTwEY6TyM6RzPo7X1xYNu7JqsbIYQyqwQ7G-zhdSNiYHHynj" +
			"fFbvGJ-XddrHFH09CofPzqEASRNwUObpYQMqVgaLsZ1B7a0leZyvdkONwiwqpwXrK4zkMrSSPodjGYjNFSBVxAiLnWhVDXlaUud8ggnKEeU" +
			"VZHHNHVuc1V-z8u21KTyDFb3rYnYmaFg052JM12LfWQg5Lqz-2qzK65H9GRvrQJ2SN1lYw7kbtQJQArM7ATyVd60j0cEK_kGwaxx8TEqRim" +
			"nxC8Cq8mNVO6Po-W0R_ShGcsthOTYSORyNd1gMqjDcXIU_Q.eOT18z-o7-Y9SQXRxBqGkg";

		String serverJwks = """
				{
				  "keys": [
				    {
				      "p": "4wyfZ-qyJXc6gXdBi5mg3jEd8FVAFe7GeW1dOB5QzASA2Gg4OssD2E-w8oN1TBdInx0Wh_DYOI2r7afPbB9PbUpRPh02FlcFO8UG0GqG_VviDxBP3FTXgmO_k9-O_2GR399EhfmAphDXNKoa2l9qOv0E_XkPSh-Y85tH0am2DOE",
				      "kty": "RSA",
				      "q": "saN1O7Rm-fhQCgkyTVBMxmXKenWD4uR5qngSpihw9meTszEqnOQ3UbsYjuA0sHaDXExWsD75hDTuRzk5KM76lM3StZvfLU-l-MRcCFzpRmy1lG3jAu-uDMU8L7xCcv0fmCFN2kLNwEcCdui9zN9llr30p4QD_kzMKdIGvBpFGRE",
				      "d": "TmQw1YazUEaTRUthF7LNGj7AftVRDozbquGCdxmtc5C-OsxPU43myG4xPedWUmrADzUffs8fSKOjYR2ZyK5TPSyUjko0vxenWRPrVU_F5J7WPFW6QEc_kJerBzxliLnW8SmlNZnGTIvFrbewxc6o-_u1HQwqZPb9U37-jExg0FMj0BlYo-bRUnGwfPZi1_YZoYQlYLGSjyQWo7pJ0ePdY9UOIcDN7K1-Lut9xoQ-BFQhnxdVtElzBKWcSNqcv8bItbDYl3_oYlZdRlXa4Muou0cS7XWiKqyn803Fj5bpHGhccxE82mqhyRm7qQR8wwFc7TtovTJKwaO4k3k5IlTuAQ",
				      "e": "AQAB",
				      "use": "enc",
				      "kid": "a31648e0-29ab-48fc-bb35-a250e45e2152",
				      "qi": "azV6-V7S4nrbfHS-xwSWIAGcDNiA-b_beiADaIDqySpEYXEfO6zFljztptc2Xl-gbv1IZwp6-9Fq4zS7ycuytHJX4U6UUlu7WZgsMC-6TsXDgfq3Qlal_K2C_bQEuHnl2OaLqRAlLP_ufsmyzCnjBBjan4jqr321o3_7yrIfYwE",
				      "dp": "1Q2OM1v26NM1kjEOz2lMm9LpLhFA_pO9qEnodHV2Ccub__Xj9b91DzDQcPDMS6d5nN0VvGBS9NZz46FZMTM8O-jFzYxpqZLktU5P85hSrUHgIZSD69OtGoxxQ9g-_gNXeASw7yrN_obE-oU4h3uLWmj8msFTiJGX29f1D5e53eE",
				      "alg": "RSA-OAEP-256",
				      "dq": "XH2aTcLplx-mNQOC0GSOBQL9APPs_pctjgfE1gTahEF70uI3qMbe0sxSxhmQ9YxPayBIRq4TZrXUlvsBevEB4balDnAhLMt6xGMg9C3EPCngBW-NsnqvKfEMHGKuSobnC2PT0F_WF1Yhv14BbQmoMGupVgtTtPWxm9jCLvCSp7E",
				      "n": "nYyzOhEzC1vjPAHXD0zK_j-IZpsUaxjdUma2R6VIanCbeDRlVZrK9MYBTc_QmJ-7Xqn0hZsy-NbzC2ojpNb64qUsozA7_R03K_8vz9pntZ0Ozry7nRAZr5aUSgLI_-ynTdPZ5ysfXZpqQvnvcm8De5zvIo99H7AhXmvuKGwQddviIsNqNEVWztj0xIQkxNl4t6SiYX8byhwUqiYTtXytKxKvNz7ORoHppZ8wNthZNglDb1Z5hjlgG7Qk9jCn6qaOBump4g0nV_750dkeHt_zktR3dpKFVWwy6E7ekgg705Oanm3WsMIGYBr2MwvyhU2h4Lk04wj_jb19ozUbXv3T8Q"
				    },
				    {
				      "kty": "EC",
				      "d": "dnzvy5_dPOPOVXhQBNgCNp9meZ9eWWXPiT2EqyQ0Els",
				      "use": "enc",
				      "crv": "P-256",
				      "kid": "938fb0d4-6289-45e0-b6be-539482f50224",
				      "x": "OpEUoXuEYOqJQIo4dSMx4lK2OTbzBDF5TbDIsggIkjY",
				      "y": "gVSkROOi6CX0iba0flBYqFoGYveOA4gbkm7eQJi53vw",
				      "alg": "ECDH-ES"
				    }
				  ]
				}""";
		JsonObject clientJsonObject = JsonParser.parseString(client).getAsJsonObject();
		JsonObject jwksJsonObject = JsonParser.parseString(serverJwks).getAsJsonObject();
		try {
			JsonObject jsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(requestObject,clientJsonObject, jwksJsonObject);
			assertNotNull(jsonObject);
			assertNotNull(jsonObject.get("claims"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
