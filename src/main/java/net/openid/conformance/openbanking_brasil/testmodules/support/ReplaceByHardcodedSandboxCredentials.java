package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ReplaceByHardcodedSandboxCredentials extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		JsonObject clientValue = (JsonObject) env.getElementFromObject("config" , "client");

		if(clientValue == null) {
			throw error("Client has not been specified");
		}

		String clientString = "{\"client\":{" +
			                             "\"jwks\":{" +
													"\"keys\":[" +
														"{" +
																"\"p\":\"8ZaCcG1L5KClXOOLX8L2RAkxv9TANmO3Grc0-5qCPBfcv7ZQEQITyo6sHr3Si4WiCtY4y_SOS1OHaKFgKPmFhqFj_shwh5s8xJ3K8F0jjNjQ2fePG0dL5Yn2n0Qv5b2JixgTqA_7aFwPjeG8lxuwEBrAoasiLmjTjXTGT7WACZ0\"," +
																"\"alg\":\"PS256\"," +
																"\"kty\":\"RSA\"," +
																"\"q\":\"29fU73oM13uqRb5mSMewLpNqCnYzibAOtgvm-NM5an_RHr-FxN0WEa7K97XLLii_Mrdgsx985eTViPfCHm8D8BdfsO61nDPraNcBiNSo7A4w7o18aTsdZ2VirpE8ytdWZDwxSf-o4eKG6I-2-k04c2RIWzjeS6rk1CuoNV7x8BM\"," +
																"\"d\":\"cRLQsLlcU9sE4sn8kYqjJSw1XgaejjpRKgvYEb6yTYprqC5ifJkSXiuDfnaujCHWhgNP-LpEeFIAwsnd1r_wXaVUiU_g_LWOLnF1p-wbAB4osnib6JKqpgbcMXVKIK89TZ7-xqX8Mx_BFDWQZaRaNp0Uy19e8ZEGTck3qr_mBFwtN8H3elp5LdK7EyQE84TsU4DwXv5Y2KbyH0vWfdU5xNIMK0vyRJjfNt_ZbMyVodz7BjXQBA5KNsQ4ZMJQ08QKUuIggnMxVU0Y43CHQDoIz0IsyTr05YPfkRl4n-p9HicPRv1vuhoMjE7CLf3PiISwwkPATLtc8ZzVmCJHOnCeyQ\"," +
																"\"e\":\"AQAB\"," +
																"\"kid\":\"oVqcW9LjCHjyWPF-DpmgFStHinbiVm5DQeqksmLK3Yg\"," +
																"\"qi\":\"cn0dfdtLuMi6kxtcJfBnCBRG3K95HKOTKHVXL91aSRmhi5VnU5lLu2aOSntrdL7GCZMYI-Nmc2ENm5zy3YpEloKs89QQM_HsPylk1-UX42oe2GI6rtpux3J6e8xYYb8C_VRWb_jJVlFj9oFW6hAQ1U8gN8taAkp-yDKLqHUOuCs\"," +
																"\"dp\":\"wgUnYtVUQxwFUjFoaJLyJrffnXlmmQfCMRF72qk--LYNUmY_rTkm3eFxOAspAWZkOiHLYXlZogNGV3BrAt1KMFKDNUaSoUW6KwmyxxF2EJK8QKFB7B15RbJkP4qKkQ6EXRIMD0gul6R_1Wm6hPz2jCiAFhaTkAQeCpgDAFb6qj0\"," +
																"\"dq\":\"hd4YcLbOSQyps4xsNlc6ZjInuTUezvHhE2OOAuiEJuCsE2Amcaj6vkvEljAiB_qR6q0Veh1rbYX5rUzI7MVHDZT-FPxWpEZNS-rYqVxtfEhr7WdEYcO4dPScsZYJAa88kQ7CKMBAM0RASC7zCdmpzUI8eSfY3RS0AKqQ4-brxfc\"," +
																"\"n\":\"z3dv8FyicV0gePjHNkjk1A3dbYUIJ67dVeeEpu0Zz21I3ysDp7LgBbfs-n1jP37lu09Np8LNTBw-9gj98o2SV1I6mjlAqIBW82BRmYJpQcfF7DbF-hHeejzdPCKSJn8zZq88wKfmx6jSR_Y6febfC681bwL9rWvFIriP4rKuHkg1ZVVpnDT1777lWYsG3SgKkEL5pSBBrddlB1qFi3eTQZDSIqtsw3xAXK2cEIKZ4lhqv_uZxHhZMspK6fKQnPwJc8jkeeswdmy7Vs6-P1FdMlb6dm_K81rz4lVwY4p4GG1jDlLw1uJeV-zsHgM06gY68Z5TG0k92_kqdFikNFDmpw\"" +
														"}" +
													"]" +
										"}}}" ;
		String mtlsString = "{\"mtls\":{" +
													"\"cert\":\"-----BEGIN CERTIFICATE-----\\nMIIHGTCCBgGgAwIBAgIUGlsGeCMK3Q6baf2crqT285TXQmUwDQYJKoZIhvcNAQEL\\nBQAwcTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\\nFTATBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNB\\nTkRCT1ggSXNzdWluZyBDQSAtIEcxMB4XDTIyMDQxMjE5NTgwMFoXDTIzMDUxMjE5\\nNTgwMFowggEvMQswCQYDVQQGEwJCUjELMAkGA1UECBMCU1AxEjAQBgNVBAcTCVNB\\nTyBQQVVMTzEmMCQGA1UEChMdT3BlbiBCYW5raW5nIEJyYXNpbCAtIENoaWNhZ28x\\nLTArBgNVBAsTJGQ3Mzg0YmQwLTg0MmYtNDNjNS1iZTAyLTlkMmIyZDVlZmMyYzEl\\nMCMGA1UEAxMcd3d3Lm9wZW5iYW5raW5nYnJhc2lsLm9yZy5icjEXMBUGA1UEBRMO\\nNDMxNDI2NjYwMDAxOTcxHTAbBgNVBA8TFFByaXZhdGUgT3JnYW5pemF0aW9uMRMw\\nEQYLKwYBBAGCNzwCAQMTAkJSMTQwMgYKCZImiZPyLGQBARMkMzQ3M2RhODItZTIz\\nMy00ZGE5LWE2ZDUtZDg5ODM4M2UyYjUxMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A\\nMIIBCgKCAQEA+tq5cS7S66QQXrgjai8BScpyYAcKi2WPf0v5ulIKdElJvKKJNNZv\\n/Jr9kKyILkKt7Ah1nRQ64pcCwFROJyBvvBHr2zX5k+dbRzmMHsfSbCfzcikrdxKI\\nE7ALgtdr3mcPXgQXZJ3w+tmM/Rh4u26qrl9BNiAUx0ybA/xRHbsYhOysoy31gbl0\\npD2BeeUiwG6fVjVvlJhX1eRkKGkOPObV4F6qFmuAS9cbhhvGZynpD5xCyl4lJqNs\\npR05GVNYxzIPuDTL2ZXyj2Kl7AXpni3CdaMe5/fdNlGvTxzSIXMMff7BtkzE5EZr\\n4MjES+/g9Q68J5Oe+f33ps2u8apIDMKBJQIDAQABo4IC5zCCAuMwDAYDVR0TAQH/\\nBAIwADAdBgNVHQ4EFgQULwEAlcLoVtr2SZlKgLTlQz5biBgwHwYDVR0jBBgwFoAU\\nhn9YrRf1grZOtAWz+7DOEUPfTL4wTAYIKwYBBQUHAQEEQDA+MDwGCCsGAQUFBzAB\\nhjBodHRwOi8vb2NzcC5zYW5kYm94LnBraS5vcGVuYmFua2luZ2JyYXNpbC5vcmcu\\nYnIwSwYDVR0fBEQwQjBAoD6gPIY6aHR0cDovL2NybC5zYW5kYm94LnBraS5vcGVu\\nYmFua2luZ2JyYXNpbC5vcmcuYnIvaXNzdWVyLmNybDAnBgNVHREEIDAeghx3d3cu\\nb3BlbmJhbmtpbmdicmFzaWwub3JnLmJyMA4GA1UdDwEB/wQEAwIFoDATBgNVHSUE\\nDDAKBggrBgEFBQcDAjCCAagGA1UdIASCAZ8wggGbMIIBlwYKKwYBBAGDui9kATCC\\nAYcwggE2BggrBgEFBQcCAjCCASgMggEkVGhpcyBDZXJ0aWZpY2F0ZSBpcyBzb2xl\\nbHkgZm9yIHVzZSB3aXRoIFJhaWRpYW0gU2VydmljZXMgTGltaXRlZCBhbmQgb3Ro\\nZXIgcGFydGljaXBhdGluZyBvcmdhbmlzYXRpb25zIHVzaW5nIFJhaWRpYW0gU2Vy\\ndmljZXMgTGltaXRlZHMgVHJ1c3QgRnJhbWV3b3JrIFNlcnZpY2VzLiBJdHMgcmVj\\nZWlwdCwgcG9zc2Vzc2lvbiBvciB1c2UgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBv\\nZiB0aGUgUmFpZGlhbSBTZXJ2aWNlcyBMdGQgQ2VydGljaWNhdGUgUG9saWN5IGFu\\nZCByZWxhdGVkIGRvY3VtZW50cyB0aGVyZWluLjBLBggrBgEFBQcCARY/aHR0cDov\\nL3JlcG9zaXRvcnkuc2FuZGJveC5wa2kub3BlbmJhbmtpbmdicmFzaWwub3JnLmJy\\nL3BvbGljaWVzMA0GCSqGSIb3DQEBCwUAA4IBAQCaOUfQsU7RedZiEhumFoNedag3\\nD/IUYOuMC2QfL6KbLqPuFDxWy0u2L6Mowpj1Eo2fRbmLuPXO4V0oGNRXJOUBn05G\\nebFPJX7OVsXXFufcX+fSwu7ABlQ2rXEy6heeGB4Ag+NybG/fKOI7XC1SwDy98FS8\\nIm17wx8mEnRS7F2EhMhg5uRJBl6Ew8nN5fBSCuvc6ESBQJE3D10eCrJcEklna/ak\\nj5g/iLqGGP0vSdKAp3S8fTKIyVyyZ/ZyCZBnCh868Vr2MmqBB6Lm3PmFjI701nL1\\n/k+RHPyi7Ag++S8EyCKKA8H22REiy9PgmRN6YVLH2nzJ4p2IIW6MYyvSQkf0\\n-----END CERTIFICATE-----\"," +
													"\"key\":\"-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQD62rlxLtLrpBBe\\nuCNqLwFJynJgBwqLZY9/S/m6Ugp0SUm8ook01m/8mv2QrIguQq3sCHWdFDrilwLA\\nVE4nIG+8EevbNfmT51tHOYwex9JsJ/NyKSt3EogTsAuC12veZw9eBBdknfD62Yz9\\nGHi7bqquX0E2IBTHTJsD/FEduxiE7KyjLfWBuXSkPYF55SLAbp9WNW+UmFfV5GQo\\naQ485tXgXqoWa4BL1xuGG8ZnKekPnELKXiUmo2ylHTkZU1jHMg+4NMvZlfKPYqXs\\nBemeLcJ1ox7n9902Ua9PHNIhcwx9/sG2TMTkRmvgyMRL7+D1Drwnk575/femza7x\\nqkgMwoElAgMBAAECggEBAIDET9aqi3iMIh8M9JjoAghH1Eg4tQ/zFSLp9AnRXS5u\\nBFzBLh8iSB/py2k9NzZP4gk8GmuEW+m+TJo/bFKnUtO+e0xuO2vRochA4Q1X00rT\\ngyLBwV8A8zrv7ii+vc9PJdND1GJAtNd/XuD4c9j6qMCTgtzuwNvBFf2Iwna5eI+a\\nDaRi9GgTiuO8pK/Qp3NTCY0PaXOBg7cjFW1xoUuBptOxVwmDKcZSMeljwOyZ077O\\nQOtGbYcWwNfAfXpoAL9c6v634L8F+aiE9yY036R89PahnTBvmabNYZsW2iB7Aw+7\\n9Luq0ZAFx4vlwrQPv3kmzVbQjPFgMkIqVVQ77G16e4ECgYEA/x0iCh4kM5FlG9UU\\nmjMUdMGndyiHKmZ3yJ97/0rpb9eregSmUd4KkTHp11wbPf/qRD50bHslsWGUyLM7\\nXZejzZQUrn7WaLDEwj5BIfid/1jH1uCmNlnVNkgDe3CclpHhYtesajv4EdlcyPqj\\nHzRPQCdVga72kUWa6ZzzP1Q8SeECgYEA+7nNufqD8ymptTsW7eYtYE/kWExTuSFb\\nc2t32tUkQlOMXWTvkMgJKG61w0yxgcIhBdkO36xwsk2i3bLXki0GMW7PAZwr44Yn\\n+JTcm3BZfGQGuhNhzI6VuAZ+i/ys8fx3zSsbn3dTuEdB/IlXoItwg58JT+f6q+wr\\nwDhvZ+xoh8UCgYBJa2v5d3U5thVgLjGwsxSnCXiVrX9A7553iZaZAUkvW+VIx3/K\\nPdnSsYe36BUbAcS9ATBmbAaMByZXoGVsaRDGQGC3W19/X6gwstPx3+gwra9NoveS\\n2sWmypDd/KnEF5XC6YkAReP91w2B9cfuWhKYxvkvylfBtUOBCh9jUW3MYQKBgQCs\\n2vMdUZyIPsTNsUzWgigIqfPIemlHiFNvF44PFCu46/xIKcakKei3/gYMnT6LNw5M\\nILgo+hXNKqi7ClMjio1lJo05ss8khvwto7M1fdnFJD7GSgbvW74Nx0/gutrbtJ1j\\nosVuJJe1xCDY1Er68KWLjIrtdwPYSO3mKhq+R0WkbQKBgFhrwyUx760rPCRBc+OA\\n23uMYZzMVd8p1On0/Glt9AUboHKl718fXHj6BuCgQkqgbKAWaljAkrC3HHepfIS8\\nNreUVueCVwynN6Lu7kqT8D8+s5eaccQI0vkgwGdnWIQXFSoCnHKCZNmiAIoca1WW\\n6yOkTI09tF8cpsMHE1hLPE1d\\n-----END PRIVATE KEY-----\\n\"," +
													"\"ca\":\"-----BEGIN CERTIFICATE-----\\nMIIEajCCA1KgAwIBAgIUdIYzEFdw7QJcrySyq6IiEwZfTfAwDQYJKoZIhvcNAQEL\\nBQAwazELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\\nFTATBgNVBAsTDE9wZW4gQmFua2luZzEnMCUGA1UEAxMeT3BlbiBCYW5raW5nIFJv\\nb3QgU0FOREJPWCAtIEcxMB4XDTIwMTIxMTEwMDAwMFoXDTIzMTIxMTEwMDAwMFow\\ncTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwxFTAT\\nBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNBTkRC\\nT1ggSXNzdWluZyBDQSAtIEcxMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKC\\nAQEA6fX+272mHX5QAcDaWkVHFWjnDIcORNUJU3OuNyeuOYhlvXJWydrXe3O+cV+P\\nS39faMj/nfem3GfJBE7Xn0bWA/8ksxSfrg1BUBJDge4YBBw+VflI3A0g1fk9wJ3H\\nGInsvV4serRJ/ISJTfs0uRNugX+RrbkT/T0tup4vGd3Kl2sbwUdDjokuJNJHANeO\\nDRkQ+ra+9Wht71FBlc07yPf7qtpaWHm6aS3s47OJD35ixkG4xiZuHsScxcVtlo1V\\nW98P2cQfH9H2lll4wWlPTVHpPThB2EYrPhwcxDh8kHkkOHNkyHO/fYM47u7H4VeQ\\nV75LXWKa7iWmZg+WhFb8TXSr/wIDAQABo4H/MIH8MA4GA1UdDwEB/wQEAwIBBjAP\\nBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBSGf1itF/WCtk60BbP7sM4RQ99MvjAf\\nBgNVHSMEGDAWgBSHE+yWPmLsIRwMSlY68iUM45TpyzBMBggrBgEFBQcBAQRAMD4w\\nPAYIKwYBBQUHMAGGMGh0dHA6Ly9vY3NwLnNhbmRib3gucGtpLm9wZW5iYW5raW5n\\nYnJhc2lsLm9yZy5icjBLBgNVHR8ERDBCMECgPqA8hjpodHRwOi8vY3JsLnNhbmRi\\nb3gucGtpLm9wZW5iYW5raW5nYnJhc2lsLm9yZy5ici9pc3N1ZXIuY3JsMA0GCSqG\\nSIb3DQEBCwUAA4IBAQBy4928pVPeiHItbneeOAsDoc4Obv5Q4tn0QpqTlSeCSBbH\\nIURfEr/WaS8sv0JTbIPQEfiO/UtaN8Qxh7j5iVqTwTwgVaE/vDkHxGOen5YxAuyV\\n1Fpm4W4oQyybiA6puHEBcteuiYZHppGSMus3bmFYTPE+9B0+W914VZeHDujJ2Y3Y\\nMc32Q+PC+Zmv8RfaXp7+QCNYSXR5Ts3q3IesWGmlvAM5tLQi75JmzdWXJ1uKU4u3\\nNrw5jY4UaOlvB5Re2BSmcjxdLT/5pApzkS+tO6lICnPAtk/Y6dOJ0YxQBMImtliY\\np02yfwRaqP8WJ4CnwUHil3ZRt8U9I+psU8b4WV/3\\n-----END CERTIFICATE-----\\n-----BEGIN CERTIFICATE-----\\nMIIDpjCCAo6gAwIBAgIUS3mWeRx1uG/SMl/ql55VwRtNz7wwDQYJKoZIhvcNAQEL\\nBQAwazELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\\nFTATBgNVBAsTDE9wZW4gQmFua2luZzEnMCUGA1UEAxMeT3BlbiBCYW5raW5nIFJv\\nb3QgU0FOREJPWCAtIEcxMB4XDTIwMTIxMTEwMDAwMFoXDTI1MTIxMDEwMDAwMFow\\nazELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwxFTAT\\nBgNVBAsTDE9wZW4gQmFua2luZzEnMCUGA1UEAxMeT3BlbiBCYW5raW5nIFJvb3Qg\\nU0FOREJPWCAtIEcxMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAp50j\\njNh0wu8ioziC1HuWqOfgXwxeiePiRGw5tKDqKIbC7XV1ghEcDiymTHHWWJSQ1LEs\\nmYpZVwaos5Mrz2xJwytg8K5eqFqa7QvfOOul29bnzEFk+1gX/0nOYws3Lba9E7S+\\nuPaUmfElF4r2lcCNL2f3F87RozqZf+DQBdGUzAt9n+ipY1JpqfI3KF/5qgRkPoIf\\nJD+aj2Y1D6eYjs5uMRLU8FMYt0CCfv/Ak6mq4Y9/7CaMKp5qjlrrDux00IDpxoXG\\nKx5cK0KgACb2UBZ98oDQxcGrbRIyp8VGmv68BkEQcm7NljP863uBVxtnVTpRwQ1x\\nwYEbmSSyoonXy575wQIDAQABo0IwQDAOBgNVHQ8BAf8EBAMCAQYwDwYDVR0TAQH/\\nBAUwAwEB/zAdBgNVHQ4EFgQUhxPslj5i7CEcDEpWOvIlDOOU6cswDQYJKoZIhvcN\\nAQELBQADggEBAFoYqwoH7zvr4v0SQ/hWx/bWFRIcV/Rf6rEWGyT/moVAEjPbGH6t\\nyHhbxh3RdGcPY7Pzn797lXDGRu0pHv+GAHUA1v1PewCp0IHYukmN5D8+Qumem6by\\nHyONyUASMlY0lUOzx9mHVBMuj6u6kvn9xjL6xsPS+Cglv/3SUXUR0mMCYf963xnF\\nBIRLTRlbykgJomUptVl/F5U/+8cD+lB/fcZPoQVI0kK0VV51jAODSIhS6vqzQzH4\\ncpUmcPh4dy+7RzdTTktxOTXTqAy9/Yx+fk18O9qSQw1MKa9dDZ4YLnAQS2fJJqIE\\n1DXIta0LpqM4pMoRMXvp9SLU0atVZLEu6Sc=\\n-----END CERTIFICATE-----\\n\"" +
										"}}" ;
		String directoryString = "{\"directory\":{" +
													"\"discoveryUrl\":\"https://auth.sandbox.directory.openbankingbrasil.org.br/.well-known/openid-configuration\"," +
													"\"client_id\":\"3473da82-e233-4da9-a6d5-d898383e2b51\"," +
													"\"apibase\":\"https://matls-api.sandbox.directory.openbankingbrasil.org.br/\"," +
													"\"keystore\":\"https://keystore.sandbox.directory.openbankingbrasil.org.br/\"}}";

		JsonObject clientJson = JsonParser.parseString(clientString).getAsJsonObject();
		JsonObject clientValues = (JsonObject) clientJson.get("client");

		JsonObject mtlsJson = JsonParser.parseString(mtlsString).getAsJsonObject();
		JsonObject mtlsValues = (JsonObject) mtlsJson.get("mtls");

		JsonObject directoryJson = JsonParser.parseString(directoryString).getAsJsonObject();
		JsonObject directoryValues = (JsonObject) directoryJson.get("directory");

		env.putObject("config", "client", clientValues);
		env.putObject("config", "mtls", mtlsValues);
		env.putObject("config", "directory", directoryValues);

		logSuccess("Successfully replaced client by Sandbox Credentials one", clientJson);
		logSuccess("Successfully replaced MTLS by Sandbox Credentials one", mtlsValues);
		logSuccess("Successfully replaced directory by Sandbox Credentials one",directoryValues);
		return env;
	}
}