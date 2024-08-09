package net.openid.conformance.logging;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LogApi_UnitTest {

	@BeforeEach
	public void setUp() throws Exception {
	}

	@Test
	public void createHtmlForTestLogs() {
		String jsonExport = "{\"testInfo\":{\"_id\":\"BeDwk5rd8H\",\"testId\":\"BeDwk5rd8H\",\"testName\":\"oidcc-client-t"+
			"est\",\"variant\":{\"client_auth_type\":\"tls_client_auth\",\"request_type\":\"plain_http_"+
			"request\",\"response_type\":\"code\",\"client_registration\":\"dynamic_client\",\"response"+
			"_mode\":\"default\"},\"started\":\"2020-05-22T07:01:12.101422Z\",\"config\":{\"alias\":\"ope"+
			"nid-client\",\"description\":\"test suite runner for openid-client\",\"waitTimeoutSeco"+
			"nds\":2},\"description\":\"test suite runner for openid-client\",\"alias\":\"openid-clie"+
			"nt\",\"owner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"planId\":\"eZuylVqr"+
			"BocdR\",\"status\":\"FINISHED\",\"version\":\"4.0.0\",\"summary\":\"The client is expected t"+
			"o make an authentication request (also a token request and a userinfo request wh"+
			"ere applicable)using the selected response_type and other configuration options."+
			" \",\"result\":\"PASSED\"},\"exportedFrom\":\"https://localhost.emobix.co.uk:8443\",\"expo"+
			"rtedBy\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"exportedVersion\":\"4.0"+
			".1\",\"exportedAt\":\"May 23, 2020, 7:18:51 AM\",\"results\":[{\"_id\":\"BeDwk5rd8H-pu6eXf"+
			"pkwZbTNE2rBFPLJRk7opLzN8YB\",\"msg\":\"Test instance BeDwk5rd8H created\",\"result\":\"I"+
			"NFO\",\"baseUrl\":\"https://localhost.emobix.co.uk:8443/test/a/openid-client\",\"varia"+
			"nt\":{\"client_auth_type\":\"tls_client_auth\",\"response_type\":\"code\",\"request_type\":"+
			"\"plain_http_request\",\"client_registration\":\"dynamic_client\",\"response_mode\":\"def"+
			"ault\"},\"alias\":\"openid-client\",\"description\":\"test suite runner for openid-clien"+
			"t\",\"planId\":\"eZuylVqrBocdR\",\"config\":{\"alias\":\"openid-client\",\"description\":\"tes"+
			"t suite runner for openid-client\",\"waitTimeoutSeconds\":2},\"testName\":\"oidcc-clie"+
			"nt-test\",\"testId\":\"BeDwk5rd8H\",\"src\":\"TEST-RUNNER\",\"testOwner\":{\"sub\":\"developer"+
			"\",\"iss\":\"https://developer.com\"},\"time\":1590130872116},{\"_id\":\"BeDwk5rd8H-j5T3YD"+
			"QVp943pshGM7Gp9BxgQSQMVFfj\",\"msg\":\"Generated server configuration\",\"result\":\"SUC"+
			"CESS\",\"server_configuration\":{\"issuer\":\"https://localhost.emobix.co.uk:8443/test"+
			"/a/openid-client/\",\"authorization_endpoint\":\"https://localhost.emobix.co.uk:8443"+
			"/test/a/openid-client/authorize\",\"token_endpoint\":\"https://localhost.emobix.co.u"+
			"k:8443/test/a/openid-client/token\",\"jwks_uri\":\"https://localhost.emobix.co.uk:84"+
			"43/test/a/openid-client/jwks\",\"userinfo_endpoint\":\"https://localhost.emobix.co.u"+
			"k:8443/test/a/openid-client/userinfo\",\"registration_endpoint\":\"https://localhost"+
			".emobix.co.uk:8443/test/a/openid-client/register\",\"scopes_supported\":[\"openid\",\""+
			"phone\",\"profile\",\"email\",\"address\",\"offline_access\"],\"response_types_supported\":"+
			"[\"code\",\"id_token code\",\"token code id_token\",\"id_token\",\"token id_token\",\"token"+
			" code\",\"token\"],\"response_modes_supported\":[\"query\",\"fragment\",\"form_post\"],\"tok"+
			"en_endpoint_auth_methods_supported\":[\"client_secret_basic\",\"client_secret_post\","+
			"\"client_secret_jwt\",\"private_key_jwt\"],\"token_endpoint_auth_signing_alg_values_s"+
			"upported\":[\"RS256\",\"RS384\",\"RS512\",\"PS256\",\"PS384\",\"PS512\",\"ES256\",\"ES256K\",\"ES3"+
			"84\",\"ES512\",\"EdDSA\"],\"grant_types_supported\":[\"authorization_code\",\"implicit\"],\""+
			"claims_parameter_supported\":true,\"acr_values_supported\":[\"PASSWORD\"],\"subject_ty"+
			"pes_supported\":[\"public\",\"pairwise\"],\"claim_types_supported\":[\"normal\",\"aggregat"+
			"ed\",\"distributed\"],\"claims_supported\":[\"sub\",\"name\",\"given_name\",\"family_name\",\""+
			"middle_name\",\"nickname\",\"gender\",\"birthdate\",\"preferred_username\",\"profile\",\"web"+
			"site\",\"locale\",\"updated_at\",\"address\",\"zoneinfo\",\"phone_number\",\"phone_number_ve"+
			"rified\",\"email\",\"email_verified\"],\"id_token_signing_alg_values_supported\":[\"none"+
			"\",\"RS256\",\"RS384\",\"RS512\",\"PS256\",\"PS384\",\"PS512\",\"ES256\",\"ES256K\",\"ES384\",\"ES51"+
			"2\",\"EdDSA\"],\"id_token_encryption_alg_values_supported\":[\"RSA1_5\",\"RSA-OAEP\",\"RSA"+
			"-OAEP-256\",\"ECDH-ES\",\"ECDH-ES+A128KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A256KW\",\"A128KW\""+
			",\"A192KW\",\"A256KW\",\"A128GCMKW\",\"A192GCMKW\",\"A256GCMKW\",\"dir\"],\"id_token_encrypti"+
			"on_enc_values_supported\":[\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\",\"A128G"+
			"CM\",\"A192GCM\",\"A256GCM\"],\"request_object_signing_alg_values_supported\":[\"none\",\""+
			"RS256\",\"RS384\",\"RS512\",\"PS256\",\"PS384\",\"PS512\",\"ES256\",\"ES256K\",\"ES384\",\"ES512\","+
			"\"EdDSA\"],\"request_object_encryption_alg_values_supported\":[\"RSA1_5\",\"RSA-OAEP\",\""+
			"RSA-OAEP-256\",\"ECDH-ES\",\"ECDH-ES+A128KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A256KW\",\"A128"+
			"KW\",\"A192KW\",\"A256KW\",\"A128GCMKW\",\"A192GCMKW\",\"A256GCMKW\",\"dir\"],\"request_object"+
			"_encryption_enc_values_supported\":[\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS51"+
			"2\",\"A128GCM\",\"A192GCM\",\"A256GCM\"],\"userinfo_signing_alg_values_supported\":[\"RS25"+
			"6\",\"RS384\",\"RS512\",\"PS256\",\"PS384\",\"PS512\",\"ES256\",\"ES256K\",\"ES384\",\"ES512\",\"EdD"+
			"SA\"],\"userinfo_encryption_alg_values_supported\":[\"RSA1_5\",\"RSA-OAEP\",\"RSA-OAEP-2"+
			"56\",\"ECDH-ES\",\"ECDH-ES+A128KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A256KW\",\"A128KW\",\"A192K"+
			"W\",\"A256KW\",\"A128GCMKW\",\"A192GCMKW\",\"A256GCMKW\",\"dir\"],\"userinfo_encryption_enc_"+
			"values_supported\":[\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\",\"A128GCM\",\"A1"+
			"92GCM\",\"A256GCM\"]},\"testId\":\"BeDwk5rd8H\",\"src\":\"OIDCCGenerateServerConfiguration"+
			"\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":159013087"+
			"2119},{\"_id\":\"BeDwk5rd8H-6gKUyBfdgQANZ3ckvzxezuFyPWS402Jk\",\"testId\":\"BeDwk5rd8H\""+
			",\"src\":\"SetTokenEndpointAuthMethodsSupportedToTlsClientAuthOnly\",\"testOwner\":{\"s"+
			"ub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130872122,\"msg\":\"Set t"+
			"oken_endpoint_auth_methods_supported to tls_client_auth only\"},{\"_id\":\"BeDwk5rd8"+
			"H-sUrUu6WWSe4vaQR2QVK6n2C5fN5GDvNa\",\"testId\":\"BeDwk5rd8H\",\"src\":\"ChangeTokenEndp"+
			"ointInServerConfigurationToMtls\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://d"+
			"eveloper.com\"},\"time\":1590130872149,\"msg\":\"Replaced token_endpoint with the MTLS"+
			" one\"},{\"_id\":\"BeDwk5rd8H-Qa4F14lNB4sZls99Ce2WWpx6nN757udL\",\"msg\":\"Generated ser"+
			"ver public private JWK sets\",\"server_jwks\":{\"keys\":[{\"p\":\"92NKQx2naIbszd7dmyOkNI"+
			"LSVBus4BVjLUkfk46ts6l2c1NHRGiQ3ez8YO1tU43BVpZ8REMt431JoqsLA7Hz_-2N0tEQRj4ovRBGoX"+
			"F__vzj9OYEE5si6hzGdeK_f-syXgX2Ht6vsnkemRx5H7MZK8gcKFbQlab7HcqfRfmfAEM\",\"kty\":\"RS"+
			"A\",\"q\":\"rh768jKSkXFs291cRkodvAQBo17amsT6Z-jxixP4oyJ4dQBl87GyAc5_Bch5k7NTIhXCnE77"+
			"wpHzCkaWdLj4CdYVXRtLFyNiJM1Fe6_-v8wZhrB1KQKd0kCGpmR3MeBKNQTf-EWD6oFM13QsFL-aqTfZ"+
			"HCMSbc7ulXZV-43KKZc\",\"d\":\"FZehZoc8136U2fIFTd-aWOx5RbNrrWHut-QmlZv90cb3MLNB752mLo"+
			"DsnwLHSNlA7i0_hsLVd_EIpMaFjC6Q8IplpOundIcYcKnmWjw4v6OZlGhk650eaUlEUQWbuuUdkgyt8p"+
			"HvKWRdIYUZCRH6NXdavWiVcvVkJkZ8UxBh9gHcl5m1pOD445m1mhYdLnTSkOrnFiVuLE938_sAP9ybKh"+
			"FkOgqpes2CMUFQGqSn0DWyhsu-Ay-fCqNxbmkMujykPKR3_zlab1SXabASMuDXT4Bkk7fm2sdVn5UbS8"+
			"BvvaGBeZ05Mw8UTDQZslvpR8jxMqmhPwHPF2Lsfria4onCeQ\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\""+
			"a9b02fda-b864-4988-9766-db07bb2d157d\",\"qi\":\"CoiJ9KELZ3XQNah5uLXWB_OpZCzDpO9hnoSK"+
			"rFXEnCDlztWamCqi5EgyMSeH9yHE4y8KnGTnZUpkYStUIBm1XHZ4lnPlotpvJii0fMBgVSxOXHFs_os8"+
			"bTGTLtk6skppvYLHvfDM4BNjABgQFsjJShHdbc2NMbc27rrehz1uGFU\",\"dp\":\"ja-5Va1DN1OnhtVw-"+
			"Ky2HDkc-30KSyEUt1gdFKbwB2k0ZSK6O4zkA6b6eQ0iw2r543y22WXcf2E_bkImQcyG4fqNcsO4XXK2Z"+
			"GpQ-aAG3eS7LoUaqpv8qDco11WxtjLfmwgxCUcjO8Ww56JuIOCztuaaceQe_McxJcPp6urnzP8\",\"dq\""+
			":\"kZFo7u8Exf9zqOSJbmIsNbGLxQ9Z5yZiylhfB4zsw0XgOsDYe0HFtejzHNybd7Xl_IsApjkKFYJ7E8"+
			"4rnu28wExwk-RrhK6aMHZEbsGWUrbJaGdBq8V6N_qmF5nKZLJEl8q5jBBh1r2-himjaL0_CdWc9v8DOz"+
			"ZTFDVYcPP7XYU\",\"n\":\"qENsnVVGCL_IQALmPpy2KzBYOGVYms5DodhhtHcverJoVoYjRqs_vvP64lVN"+
			"PsiXsgStwGQ3HrLiCpFUhdFHQ1-1dlRE6-0oHM55hTkDd0Y7TBld0PDnDTN-B7q91r_JLsNspdVQpTav"+
			"oo5qPZSV_bvxSlBTopMuMpxMBnmMKE20KiAcAVhJnlO7v97ILlIiYSCNwx0h5kmAHUZlhpw0DgE9yPVY"+
			"5osbmb1QjIbZRzbE6NRNFlJiXnTeraR89kHFfvyEeeIFTwFgkKPqFN0dZyWqd_zBO73yk1LruN5IDGwr"+
			"pDbK3hOSHqfExqJFi8qxISmuq4k3-bfllzKMz7HihQ\"},{\"kty\":\"EC\",\"d\":\"VwY8uyRyN5_ESw_iEH"+
			"zQU9mtx5azqTb0chPnvT6uwfo\",\"use\":\"sig\",\"crv\":\"P-256\",\"kid\":\"1e5c5964-3a8f-4133-9"+
			"c5e-345b6cdc7983\",\"x\":\"Cl_uW3h3sqo72rfUiBwRKnZxdHQrr77yM8XGm7SAVN0\",\"y\":\"4C5VHcd"+
			"1vibKU6wB5cAp4ZHBFeajJn83i3O1efKLSyM\"},{\"kty\":\"EC\",\"d\":\"LPfjJ5_eaubQLtbm8Emh_3kM"+
			"fP4vNT2D3ahZseSwTDY\",\"use\":\"sig\",\"crv\":\"secp256k1\",\"kid\":\"30892e8f-5c88-4220-b60"+
			"6-1d94fbc48f1e\",\"x\":\"jTP7IkOQFXQF3cBYZXJGTLrMxoBaOLIfxJtWUzHboXc\",\"y\":\"qB-tU8k1j"+
			"HkMNPH3KerOoIeLw68Fo0th2N6imyxV1fw\"},{\"kty\":\"OKP\",\"d\":\"dKzUQ1MhkUTPx4A82WNmAB175"+
			"aZCEt62iSAdxzSKs0M\",\"use\":\"sig\",\"crv\":\"Ed25519\",\"kid\":\"ef63a462-5106-4a0a-bca1-a"+
			"845365248f2\",\"x\":\"sfLfwTAzPJnE6GkeHS5di9XSStnZfMmG6LZJvaSlxeE\"}]},\"server_encryp"+
			"tion_keys\":{\"keys\":[{\"p\":\"1lgd4DYTwNH2OxEbJXR0UvssTS4BMZh6boXtN01zO_gR0IFUlp3vTd"+
			"1-A3as5h_ymLhkY1xMs6MUYbpeKK7MP-8R-kH0wgiiqx9HcT01aWKuyavRije-f-t5xhgqIH52PuLm6D"+
			"dIu0vEqY0tKyrzNFZABGaSYAuygrZJtUbiC-0\",\"kty\":\"RSA\",\"q\":\"uux_RAVs_az1TIXhU3IsxXmY"+
			"EVmePQx2g1PeOaSCTb_5aUkfN8v2cmfCeClevclT76RmBGr0xPpO8lOhm84OrRhHLEEiCkYF06ec1-2e"+
			"Rk75lCw7imy2oczjpygAslJN8zC_FFbVvX1T59HWxSyEcFs5ENcfsPWjoa9OuumLqJE\",\"d\":\"R932mm"+
			"YL6s4-ZCoEYoHgD5FYv3IAZ8Tk4aw-aY9aoB48MC_4HXyqizkri1ZMgDPMUP8AIaoBFXHeim0qh1GLv2"+
			"zSqi2ec9mrYi12pbl_z5nj_7-6iVvcmXu46QGRf8jyrEUOxTLgspm94op4cnir6y4fhhH2YyRwwuai-Y"+
			"0oTdMqMZs22hTSyGXQC1do2u0Wo0A3QW1ZbPthSn1Kv0d6rbCI_oo2vQaLdomz3D0gx80JkxCQGTlo-1"+
			"Ma8O9lhBSQGGmo5FFLCG-LUfQwrE1ZtQ82NVKeMJMixlalfF_ucA21j7-74-XFH6Yo3hZy6wCL61gVSs"+
			"d8AL0lgf6nGuNCgQ\",\"e\":\"AQAB\",\"use\":\"enc\",\"kid\":\"d5f9a9f3-b732-4600-9065-0164198f"+
			"e7e8\",\"qi\":\"d8-C4uvGchlYtu9EXWmqFD-FUkHMdTLZD4XcY4mT4_XPJRVtSvW436cnl5gr2vE3g6ZN"+
			"1ClgXAqVzeavAxeXZOLtHemWxdLSbbpqQOU7MnZzYj12146gNFHNO4Z52cXBdDPYphC-Cd6zdEKYo8Qb"+
			"yVHjhdRPzJ9iAP7efw4_acg\",\"dp\":\"bgGNW7wFSUm46Lmvx_pLlScJppa80Bpkg4LCq0ZZpOZ9JhiqN"+
			"B7caYcKnHZ06CpvVIgWJZSCXROOa1W9631CoHFvNZRSLJ9H9TE5dlmmpu6ZrDE9eoV_hKFyvwM547Fju"+
			"IS1Y2q0LkQFq8sj6mrBpRO7CUcnSpnWRRWPILU_YEU\",\"alg\":\"RSA-OAEP\",\"dq\":\"qLQ8JJs1BDru5"+
			"ZLLi_G3QL9-pOIadATkjaDLJ4E3bY6_PuwU9dnq4tSM6sCD1ox3agkZXhFeOs2jJQigCivOiSEpU2Kxo"+
			"04RgjZ701KNj-1FShsl3chCiQOPz9TF1Ct-WOf8_RFmNXCAovB7g1erbHCYr39JBgCdH5dGWmHgf1E\","+
			"\"n\":\"nIIJfxzBWNhaH4vWY8kaTEQOXbLtn6W7j2GShHJu9OXI9yMrT7bXwzEbaa-GgwoX5L0hTQDs0j3"+
			"_HimFTcTJGyzZFPrZrnBiO9w1_lV8-PQ00ssOjkCIBCdsXUEY76MPVuKKrZWUn89p-oZUMKrvwEBVX-q"+
			"8uLiYGASKoF55VKSfpp9s20FtgW0St8QtPs8c6qA4XaOS3lnirHnfZpZu1KuvPkFscbwJ2HHByasmML9"+
			"BD3L3dE-657P9rZ8M6LzQUYY_NxvlhMyylA05rBySVRiHh_5GXZdXDXX5vRdmsgqgrQ1FhbYnk2mobSX"+
			"N0SAVWtLvmQemeC9Acc_6rItJPQ\"},{\"kty\":\"EC\",\"d\":\"6jvIhy7c7LPcxD2u74n_CnRp9Lo8gejwq"+
			"ZVlFjl4B6M\",\"use\":\"enc\",\"crv\":\"P-256\",\"kid\":\"71da3fd0-2b61-4ad7-910c-516e513e71b"+
			"d\",\"x\":\"jJDeVt2tHGl_iAa0E0YQ0aHjzaRqpxN94mHNRd2ylPU\",\"y\":\"UJSjhP_bEhdEjZmu0mB7XD"+
			"TAjz3skgyeRufWBbLIHUU\",\"alg\":\"ECDH-ES\"}]},\"server_public_jwks\":{\"keys\":[{\"kty\":\""+
			"RSA\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\"a9b02fda-b864-4988-9766-db07bb2d157d\",\"n\":\"qE"+
			"NsnVVGCL_IQALmPpy2KzBYOGVYms5DodhhtHcverJoVoYjRqs_vvP64lVNPsiXsgStwGQ3HrLiCpFUhd"+
			"FHQ1-1dlRE6-0oHM55hTkDd0Y7TBld0PDnDTN-B7q91r_JLsNspdVQpTavoo5qPZSV_bvxSlBTopMuMp"+
			"xMBnmMKE20KiAcAVhJnlO7v97ILlIiYSCNwx0h5kmAHUZlhpw0DgE9yPVY5osbmb1QjIbZRzbE6NRNFl"+
			"JiXnTeraR89kHFfvyEeeIFTwFgkKPqFN0dZyWqd_zBO73yk1LruN5IDGwrpDbK3hOSHqfExqJFi8qxIS"+
			"muq4k3-bfllzKMz7HihQ\"},{\"kty\":\"RSA\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\"a64634e9-dca9-"+
			"4a17-8473-3a9916a5995f\",\"n\":\"iFgkzqI6TG8MxrTXCEmuDcn0dFq9vTbxIylgOMlBUmKUOhiaIUI"+
			"wfDeTWhzCn7VqZZMZoHX7AKgi3fitz8FLK_Rk-DI9ZuUMqX8jgvBmqUVn3_cEznTd1pjpwXVWdBcQeXg"+
			"--fMApLn0_Xhw_Nw89T5-ktlJ_PL1_-KqM58Zp_EURGOyspzoTrycumteAX91MIISA7Yfa4vPq3soOWs"+
			"j0aa77Y63MRbcaolsR1O5a_9JS8RFLW7wsgYkjNyMrejkMgliG6LFk1SUV0PKNJZmiPt8WAaTrexsCvt"+
			"fmeX7Xlar5_Jpd-_16z_ny4ys9Zhg3UWSyPKocngVqaw-1S3XlQ\"},{\"kty\":\"EC\",\"use\":\"sig\",\"c"+
			"rv\":\"P-256\",\"kid\":\"1e5c5964-3a8f-4133-9c5e-345b6cdc7983\",\"x\":\"Cl_uW3h3sqo72rfUiB"+
			"wRKnZxdHQrr77yM8XGm7SAVN0\",\"y\":\"4C5VHcd1vibKU6wB5cAp4ZHBFeajJn83i3O1efKLSyM\"},{\""+
			"kty\":\"EC\",\"use\":\"sig\",\"crv\":\"P-256\",\"kid\":\"b8b5f398-74f0-48a0-8c3c-b864f90b5229\""+
			",\"x\":\"1l_5-0TS9yqY2H5f6ykCvPlCgzIYVfYrqdUP8vlSBF0\",\"y\":\"ZILTNWr87F7rBcZQ8z3Lppf0"+
			"BMcQJuFdxqURS2iX3Fw\"},{\"kty\":\"EC\",\"use\":\"sig\",\"crv\":\"secp256k1\",\"kid\":\"30892e8f-"+
			"5c88-4220-b606-1d94fbc48f1e\",\"x\":\"jTP7IkOQFXQF3cBYZXJGTLrMxoBaOLIfxJtWUzHboXc\",\""+
			"y\":\"qB-tU8k1jHkMNPH3KerOoIeLw68Fo0th2N6imyxV1fw\"},{\"kty\":\"OKP\",\"use\":\"sig\",\"crv\""+
			":\"Ed25519\",\"kid\":\"ef63a462-5106-4a0a-bca1-a845365248f2\",\"x\":\"sfLfwTAzPJnE6GkeHS5"+
			"di9XSStnZfMmG6LZJvaSlxeE\"},{\"kty\":\"RSA\",\"e\":\"AQAB\",\"use\":\"enc\",\"kid\":\"d5f9a9f3-b"+
			"732-4600-9065-0164198fe7e8\",\"alg\":\"RSA-OAEP\",\"n\":\"nIIJfxzBWNhaH4vWY8kaTEQOXbLtn6"+
			"W7j2GShHJu9OXI9yMrT7bXwzEbaa-GgwoX5L0hTQDs0j3_HimFTcTJGyzZFPrZrnBiO9w1_lV8-PQ00s"+
			"sOjkCIBCdsXUEY76MPVuKKrZWUn89p-oZUMKrvwEBVX-q8uLiYGASKoF55VKSfpp9s20FtgW0St8QtPs"+
			"8c6qA4XaOS3lnirHnfZpZu1KuvPkFscbwJ2HHByasmML9BD3L3dE-657P9rZ8M6LzQUYY_NxvlhMyylA"+
			"05rBySVRiHh_5GXZdXDXX5vRdmsgqgrQ1FhbYnk2mobSXN0SAVWtLvmQemeC9Acc_6rItJPQ\"},{\"kty"+
			"\":\"EC\",\"use\":\"enc\",\"crv\":\"P-256\",\"kid\":\"71da3fd0-2b61-4ad7-910c-516e513e71bd\",\"x"+
			"\":\"jJDeVt2tHGl_iAa0E0YQ0aHjzaRqpxN94mHNRd2ylPU\",\"y\":\"UJSjhP_bEhdEjZmu0mB7XDTAjz3"+
			"skgyeRufWBbLIHUU\",\"alg\":\"ECDH-ES\"}]},\"testId\":\"BeDwk5rd8H\",\"src\":\"OIDCCGenerateS"+
			"erverJWKs\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":"+
			"1590130872646},{\"_id\":\"BeDwk5rd8H-0XHzEEfoMpILFdYaUeKXSnXaBu68Xn9W\",\"msg\":\"Valid"+
			" server JWKs\",\"result\":\"SUCCESS\",\"requirements\":[\"RFC7517-1.1\"],\"testId\":\"BeDwk5"+
			"rd8H\",\"src\":\"ValidateServerJWKs\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://d"+
			"eveloper.com\"},\"time\":1590130872655},{\"_id\":\"BeDwk5rd8H-9aniifwAJoYvSHceLV5x5irt"+
			"07HwyfKd\",\"msg\":\"Distinct \\u0027kid\\u0027 value in all keys of server_jwks\",\"res"+
			"ult\":\"SUCCESS\",\"see\":\"https://bitbucket.org/openid/connect/issues/1127\",\"require"+
			"ments\":[\"RFC7517-4.5\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"CheckDistinctKeyIdValueInSer"+
			"verJWKs\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":15"+
			"90130872656},{\"_id\":\"BeDwk5rd8H-onY0jobwkIds9vULqg1kPcnEsCc4bE82\",\"msg\":\"Added u"+
			"ser information\",\"result\":\"SUCCESS\",\"user_info\":{\"sub\":\"user-subject-1234531\",\"n"+
			"ame\":\"Demo T. User\",\"given_name\":\"Demo\",\"family_name\":\"User\",\"middle_name\":\"Ther"+
			"esa\",\"nickname\":\"Dee\",\"preferred_username\":\"d.tu\",\"gender\":\"female\",\"birthdate\":"+
			"\"2000-02-03\",\"address\":{\"street_address\":\"100 Universal City Plaza\",\"locality\":\""+
			"Hollywood\",\"region\":\"CA\",\"postal_code\":\"91608\",\"country\":\"USA\"},\"zoneinfo\":\"Amer"+
			"ica/Los_Angeles\",\"locale\":\"en-US\",\"phone_number\":\"+1 555 5550000\",\"phone_number_"+
			"verified\":false,\"email\":\"user@example.com\",\"email_verified\":false,\"website\":\"htt"+
			"ps://openid.net/\",\"updated_at\":1580000000},\"testId\":\"BeDwk5rd8H\",\"src\":\"OIDCCL"+
			"oadUserInfo\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time"+
			"\":1590130872657},{\"_id\":\"BeDwk5rd8H-w7vVtF3O66LfbU791jyk3VVR4DLS7QpI\",\"msg\":\"Def"+
			"inition for client not present in supplied configuration\",\"result\":\"INFO\",\"testI"+
			"d\":\"BeDwk5rd8H\",\"src\":\"StoreOriginalClientConfiguration\",\"testOwner\":{\"sub\":\"develo"+
			"per\",\"iss\":\"https://developer.com\"},\"time\":1590130872659},{\"_id\":\"BeDwk5rd8H-geC"+
			"snUelN0gJVsIh0lB8LBuThIUGC2hF\",\"testId\":\"BeDwk5rd8H\",\"src\":\"oidcc-client-test\",\""+
			"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":159013087266"+
			"2,\"msg\":\"Setup Done\"},{\"_id\":\"BeDwk5rd8H-3udi0DVhN2A2COxBFEnBdJuF4dbA2bB1\",\"msg\""+
			":\"Incoming HTTP request to test instance BeDwk5rd8H\",\"incoming_headers\":{\"user-a"+
			"gent\":\"openid-client/3.14.1 (https://github.com/panva/node-openid-client)\",\"acce"+
			"pt\":\"application/json\",\"accept-encoding\":\"gzip, deflate\",\"host\":\"localhost.emobi"+
			"x.co.uk:8443\",\"connection\":\"close\",\"x-ssl-cipher\":\"ECDHE-RSA-AES128-GCM-SHA256\","+
			"\"x-ssl-protocol\":\"TLSv1.2\"},\"incoming_path\":\".well-known/openid-configuration\",\""+
			"http\":\"incoming\",\"incoming_method\":\"GET\",\"incoming_query_string_params\":{},\"test"+
			"Id\":\"BeDwk5rd8H\",\"src\":\"oidcc-client-test\",\"testOwner\":{\"sub\":\"developer\",\"iss\":"+
			"\"https://developer.com\"},\"time\":1590130872709},{\"_id\":\"BeDwk5rd8H-Fl6KUnw32JWq37"+
			"ESiEXoI59TpqzgWoqB\",\"msg\":\"Response to HTTP request to test instance BeDwk5rd8H\""+
			",\"outgoing_status_code\":200,\"outgoing_headers\":{},\"http\":\"outgoing\",\"outgoing_bo"+
			"dy\":{\"issuer\":\"https://localhost.emobix.co.uk:8443/test/a/openid-client/\",\"autho"+
			"rization_endpoint\":\"https://localhost.emobix.co.uk:8443/test/a/openid-client/aut"+
			"horize\",\"token_endpoint\":\"https://localhost.emobix.co.uk:8443/test-mtls/a/openid"+
			"-client/token\",\"jwks_uri\":\"https://localhost.emobix.co.uk:8443/test/a/openid-cli"+
			"ent/jwks\",\"userinfo_endpoint\":\"https://localhost.emobix.co.uk:8443/test/a/openid"+
			"-client/userinfo\",\"registration_endpoint\":\"https://localhost.emobix.co.uk:8443/t"+
			"est/a/openid-client/register\",\"scopes_supported\":[\"openid\",\"phone\",\"profile\",\"em"+
			"ail\",\"address\",\"offline_access\"],\"response_types_supported\":[\"code\",\"id_token co"+
			"de\",\"token code id_token\",\"id_token\",\"token id_token\",\"token code\",\"token\"],\"res"+
			"ponse_modes_supported\":[\"query\",\"fragment\",\"form_post\"],\"token_endpoint_auth_met"+
			"hods_supported\":[\"tls_client_auth\"],\"token_endpoint_auth_signing_alg_values_supp"+
			"orted\":[\"RS256\",\"RS384\",\"RS512\",\"PS256\",\"PS384\",\"PS512\",\"ES256\",\"ES256K\",\"ES384\""+
			",\"ES512\",\"EdDSA\"],\"grant_types_supported\":[\"authorization_code\",\"implicit\"],\"cla"+
			"ims_parameter_supported\":true,\"acr_values_supported\":[\"PASSWORD\"],\"subject_types"+
			"_supported\":[\"public\",\"pairwise\"],\"claim_types_supported\":[\"normal\",\"aggregated\""+
			",\"distributed\"],\"claims_supported\":[\"sub\",\"name\",\"given_name\",\"family_name\",\"mid"+
			"dle_name\",\"nickname\",\"gender\",\"birthdate\",\"preferred_username\",\"profile\",\"websit"+
			"e\",\"locale\",\"updated_at\",\"address\",\"zoneinfo\",\"phone_number\",\"phone_number_verif"+
			"ied\",\"email\",\"email_verified\"],\"id_token_signing_alg_values_supported\":[\"none\",\""+
			"RS256\",\"RS384\",\"RS512\",\"PS256\",\"PS384\",\"PS512\",\"ES256\",\"ES256K\",\"ES384\",\"ES512\","+
			"\"EdDSA\"],\"id_token_encryption_alg_values_supported\":[\"RSA1_5\",\"RSA-OAEP\",\"RSA-OA"+
			"EP-256\",\"ECDH-ES\",\"ECDH-ES+A128KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A256KW\",\"A128KW\",\"A"+
			"192KW\",\"A256KW\",\"A128GCMKW\",\"A192GCMKW\",\"A256GCMKW\",\"dir\"],\"id_token_encryption_"+
			"enc_values_supported\":[\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\",\"A128GCM\""+
			",\"A192GCM\",\"A256GCM\"],\"request_object_signing_alg_values_supported\":[\"none\",\"RS2"+
			"56\",\"RS384\",\"RS512\",\"PS256\",\"PS384\",\"PS512\",\"ES256\",\"ES256K\",\"ES384\",\"ES512\",\"Ed"+
			"DSA\"],\"request_object_encryption_alg_values_supported\":[\"RSA1_5\",\"RSA-OAEP\",\"RSA"+
			"-OAEP-256\",\"ECDH-ES\",\"ECDH-ES+A128KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A256KW\",\"A128KW\""+
			",\"A192KW\",\"A256KW\",\"A128GCMKW\",\"A192GCMKW\",\"A256GCMKW\",\"dir\"],\"request_object_en"+
			"cryption_enc_values_supported\":[\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\","+
			"\"A128GCM\",\"A192GCM\",\"A256GCM\"],\"userinfo_signing_alg_values_supported\":[\"RS256\","+
			"\"RS384\",\"RS512\",\"PS256\",\"PS384\",\"PS512\",\"ES256\",\"ES256K\",\"ES384\",\"ES512\",\"EdDSA\""+
			"],\"userinfo_encryption_alg_values_supported\":[\"RSA1_5\",\"RSA-OAEP\",\"RSA-OAEP-256\""+
			",\"ECDH-ES\",\"ECDH-ES+A128KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A256KW\",\"A128KW\",\"A192KW\","+
			"\"A256KW\",\"A128GCMKW\",\"A192GCMKW\",\"A256GCMKW\",\"dir\"],\"userinfo_encryption_enc_val"+
			"ues_supported\":[\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\",\"A128GCM\",\"A192G"+
			"CM\",\"A256GCM\"]},\"outgoing_path\":\".well-known/openid-configuration\",\"testId\":\"BeD"+
			"wk5rd8H\",\"src\":\"oidcc-client-test\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https:/"+
			"/developer.com\"},\"time\":1590130872718},{\"_id\":\"BeDwk5rd8H-vFpLOHmpe6YGTfsbqwoQKF"+
			"LlAklFJPuv\",\"msg\":\"Incoming HTTP request to test instance BeDwk5rd8H\",\"incoming_"+
			"headers\":{\"user-agent\":\"openid-client/3.14.1 (https://github.com/panva/node-open"+
			"id-client)\",\"accept\":\"application/json\",\"accept-encoding\":\"gzip, deflate\",\"conte"+
			"nt-type\":\"application/json\",\"content-length\":\"223\",\"host\":\"localhost.emobix.co.u"+
			"k:8443\",\"connection\":\"close\",\"x-ssl-cipher\":\"ECDHE-RSA-AES128-GCM-SHA256\",\"x-ssl"+
			"-protocol\":\"TLSv1.2\"},\"incoming_path\":\"register\",\"http\":\"incoming\",\"incoming_met"+
			"hod\":\"POST\",\"incoming_body_json\":{\"tls_client_auth_san_dns\":\"dnsname2.conformanc"+
			"e.example.com\",\"token_endpoint_auth_method\":\"tls_client_auth\",\"response_types\":["+
			"\"code\"],\"grant_types\":[\"authorization_code\"],\"redirect_uris\":[\"https://openid-cl"+
			"ient.local/cb\"]},\"incoming_query_string_params\":{},\"incoming_body\":\"{\\\"tls_clien"+
			"t_auth_san_dns\\\":\\\"dnsname2.conformance.example.com\\\",\\\"token_endpoint_auth_meth"+
			"od\\\":\\\"tls_client_auth\\\",\\\"response_types\\\":[\\\"code\\\"],\\\"grant_types\\\":[\\\"author"+
			"ization_code\\\"],\\\"redirect_uris\\\":[\\\"https://openid-client.local/cb\\\"]}\",\"testId"+
			"\":\"BeDwk5rd8H\",\"src\":\"oidcc-client-test\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"h"+
			"ttps://developer.com\"},\"time\":1590130872736},{\"_id\":\"BeDwk5rd8H-IPmhsaSeb7exxQo7"+
			"KYILoNeGItiMBurJ\",\"msg\":\"Registration endpoint\",\"blockId\":\"2b8ee6\",\"startBlock\":"+
			"true,\"testId\":\"BeDwk5rd8H\",\"src\":\"-START-BLOCK-\",\"testOwner\":{\"sub\":\"developer\","+
			"\"iss\":\"https://developer.com\"},\"time\":1590130872741},{\"_id\":\"BeDwk5rd8H-lfhV8oKE"+
			"df28S4yXOc1iylAvtXp0sB9W\",\"msg\":\"Extracted dynamic client registration request\","+
			"\"result\":\"SUCCESS\",\"blockId\":\"2b8ee6\",\"request\":{\"tls_client_auth_san_dns\":\"dnsn"+
			"ame2.conformance.example.com\",\"token_endpoint_auth_method\":\"tls_client_auth\",\"re"+
			"sponse_types\":[\"code\"],\"grant_types\":[\"authorization_code\"],\"redirect_uris\":[\"ht"+
			"tps://openid-client.local/cb\"]},\"testId\":\"BeDwk5rd8H\",\"src\":\"OIDCCExtractDynamic"+
			"RegistrationRequest\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com"+
			"\"},\"time\":1590130872742},{\"_id\":\"BeDwk5rd8H-MwSEUA6ZWyqQs8VlrEWgbokZ0ZMrZwTp\",\"m"+
			"sg\":\"This application requires that registration requests contain at least one c"+
			"ontact.\",\"result\":\"INFO\",\"blockId\":\"2b8ee6\",\"testId\":\"BeDwk5rd8H\",\"src\":\"EnsureR"+
			"egistrationRequestContainsAtLeastOneContact\",\"testOwner\":{\"sub\":\"developer\",\"iss"+
			"\":\"https://developer.com\"},\"time\":1590130872744},{\"_id\":\"BeDwk5rd8H-KpdVfOI4fNGq"+
			"nSzOs5LWKGIsATewcMYv\",\"msg\":\"grant_types match response_types\",\"result\":\"SUCCESS"+
			"\",\"blockId\":\"2b8ee6\",\"requirements\":[\"OIDCR-2\"],\"grant_types\":[\"authorization_co"+
			"de\"],\"response_types\":[\"code\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"ValidateClientGrantT"+
			"ypes\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":15901"+
			"30872747},{\"_id\":\"BeDwk5rd8H-2ZwfVbIyRSwaFN80PXCpfEi2fLjTUs46\",\"msg\":\"Valid redi"+
			"rect_uri(s) provided in registration request\",\"result\":\"SUCCESS\",\"blockId\":\"2b8e"+
			"e6\",\"requirements\":[\"OIDCR-2\"],\"redirect_uris\":[\"https://openid-client.local/cb\""+
			"],\"testId\":\"BeDwk5rd8H\",\"src\":\"OIDCCValidateClientRedirectUris\",\"testOwner\":{\"su"+
			"b\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130872749},{\"_id\":\"BeDw"+
			"k5rd8H-xp0aO7b3V3m8Q3vMuwxwL1SRdRysTflA\",\"msg\":\"Client does not contain any logo"+
			"_uri\",\"result\":\"SUCCESS\",\"blockId\":\"2b8ee6\",\"requirements\":[\"OIDCR-2\"],\"testId\":"+
			"\"BeDwk5rd8H\",\"src\":\"ValidateClientLogoUris\",\"testOwner\":{\"sub\":\"developer\",\"iss\""+
			":\"https://developer.com\"},\"time\":1590130872752},{\"_id\":\"BeDwk5rd8H-2ZjiWkXYkBSAx"+
			"uxMG01o5cKxomvOCatl\",\"msg\":\"Client does not contain any client_uri\",\"result\":\"SU"+
			"CCESS\",\"blockId\":\"2b8ee6\",\"requirements\":[\"OIDCR-2\"],\"testId\":\"BeDwk5rd8H\",\"src\""+
			":\"ValidateClientUris\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.co"+
			"m\"},\"time\":1590130872753},{\"_id\":\"BeDwk5rd8H-RTkJxu0ba0IkfuyipMEDwGH5CjlmUj5Z\",\""+
			"msg\":\"Client does not contain any policy_uri\",\"result\":\"SUCCESS\",\"blockId\":\"2b8e"+
			"e6\",\"requirements\":[\"OIDCR-2\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"ValidateClientPolicy"+
			"Uris\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":15901"+
			"30872754},{\"_id\":\"BeDwk5rd8H-rXmd9CqkyGhPqqkTQV0tomu9PHhRLXeY\",\"msg\":\"Client doe"+
			"s not contain any tos_uri\",\"result\":\"SUCCESS\",\"blockId\":\"2b8ee6\",\"requirements\":"+
			"[\"OIDCR-2\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"ValidateClientTosUris\",\"testOwner\":{\"su"+
			"b\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130872757},{\"_id\":\"BeDw"+
			"k5rd8H-ba4K0vHuEMkBtd4Z43BGpdONAOubvvvS\",\"msg\":\"A subject_type was not provided\""+
			",\"result\":\"SUCCESS\",\"blockId\":\"2b8ee6\",\"requirements\":[\"OIDCR-2\"],\"testId\":\"BeDw"+
			"k5rd8H\",\"src\":\"ValidateClientSubjectType\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\""+
			"https://developer.com\"},\"time\":1590130872759},{\"_id\":\"BeDwk5rd8H-gkkjmtyGCkIrWdT"+
			"BasHxY2jGriENcV8E\",\"msg\":\"Skipped evaluation due to missing required element: cl"+
			"ient id_token_signed_response_alg\",\"result\":\"INFO\",\"blockId\":\"2b8ee6\",\"path\":\"id"+
			"_token_signed_response_alg\",\"requirements\":[\"OIDCR-2\"],\"object\":\"client\",\"testId"+
			"\":\"BeDwk5rd8H\",\"src\":\"ValidateIdTokenSignedResponseAlg\",\"testOwner\":{\"sub\":\"deve"+
			"loper\",\"iss\":\"https://developer.com\"},\"time\":1590130872766},{\"_id\":\"BeDwk5rd8H-b"+
			"Byr1u6xjBFgwts3lt531vajFwlygMit\",\"msg\":\"id_token_encrypted_response_enc is not s"+
			"et\",\"result\":\"SUCCESS\",\"blockId\":\"2b8ee6\",\"requirements\":[\"OIDCR-2\"],\"testId\":\"B"+
			"eDwk5rd8H\",\"src\":\"EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet\",\"testOwner\":"+
			"{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130872770},{\"_id\":\""+
			"BeDwk5rd8H-IQZBWpH6DrMMY9itjPP0EM3bWTtVUVby\",\"msg\":\"Skipped evaluation due to mi"+
			"ssing required element: client userinfo_signed_response_alg\",\"result\":\"INFO\",\"bl"+
			"ockId\":\"2b8ee6\",\"path\":\"userinfo_signed_response_alg\",\"requirements\":[\"OIDCR-2\"]"+
			",\"object\":\"client\",\"testId\":\"BeDwk5rd8H\",\"src\":\"ValidateUserinfoSignedResponseAl"+
			"g\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":15901308"+
			"72772},{\"_id\":\"BeDwk5rd8H-iMXnwNYJJFS7gGF86Se2jO1Qm5EcgUbf\",\"msg\":\"userinfo_encr"+
			"ypted_response_enc is not set\",\"result\":\"SUCCESS\",\"blockId\":\"2b8ee6\",\"requiremen"+
			"ts\":[\"OIDCR-2\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"EnsureUserinfoEncryptedResponseAlgI"+
			"sSetIfEncIsSet\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"t"+
			"ime\":1590130872773},{\"_id\":\"BeDwk5rd8H-mP30xIBKgUFExmwqr8ZXPTAPq5M3datm\",\"msg\":\""+
			"Skipped evaluation due to missing required element: client request_object_signin"+
			"g_alg\",\"result\":\"INFO\",\"blockId\":\"2b8ee6\",\"path\":\"request_object_signing_alg\",\"r"+
			"equirements\":[\"OIDCR-2\"],\"object\":\"client\",\"testId\":\"BeDwk5rd8H\",\"src\":\"Validate"+
			"RequestObjectSigningAlg\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer"+
			".com\"},\"time\":1590130872775},{\"_id\":\"BeDwk5rd8H-EgZVzwIUpCLoZk8sBjMRAGXFajnIbv20"+
			"\",\"msg\":\"request_object_encryption_enc is not set\",\"result\":\"SUCCESS\",\"blockId\":"+
			"\"2b8ee6\",\"requirements\":[\"OIDCR-2\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"EnsureRequestOb"+
			"jectEncryptionAlgIsSetIfEncIsSet\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://"+
			"developer.com\"},\"time\":1590130872776},{\"_id\":\"BeDwk5rd8H-JJY5hvWw23BrMSHmZIdbMY9"+
			"lru6TRhaH\",\"msg\":\"Skipped evaluation due to missing required element: client tok"+
			"en_endpoint_auth_signing_alg\",\"result\":\"INFO\",\"blockId\":\"2b8ee6\",\"path\":\"token_e"+
			"ndpoint_auth_signing_alg\",\"requirements\":[\"OIDCR-2\"],\"object\":\"client\",\"testId\":"+
			"\"BeDwk5rd8H\",\"src\":\"ValidateTokenEndpointAuthSigningAlg\",\"testOwner\":{\"sub\":\"dev"+
			"eloper\",\"iss\":\"https://developer.com\"},\"time\":1590130872778},{\"_id\":\"BeDwk5rd8H-"+
			"t6UcgBY2ZuCttPlhWdItl8ITW8gXwkeX\",\"msg\":\"default_max_age is not set\",\"result\":\"S"+
			"UCCESS\",\"blockId\":\"2b8ee6\",\"requirements\":[\"OIDCR-2\"],\"testId\":\"BeDwk5rd8H\",\"src"+
			"\":\"ValidateDefaultMaxAge\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://develope"+
			"r.com\"},\"time\":1590130872779},{\"_id\":\"BeDwk5rd8H-6AJChJj2GCdQIHRqPfhAQsNS9BOKaq8"+
			"d\",\"msg\":\"Skipped evaluation due to missing required element: client require_aut"+
			"h_time\",\"result\":\"INFO\",\"blockId\":\"2b8ee6\",\"path\":\"require_auth_time\",\"requireme"+
			"nts\":[\"OIDCR-2\"],\"object\":\"client\",\"testId\":\"BeDwk5rd8H\",\"src\":\"ValidateRequireA"+
			"uthTime\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":15"+
			"90130872780},{\"_id\":\"BeDwk5rd8H-RkNKHKH8c1wk6S8n9cQ6fuE5XWXakWbl\",\"msg\":\"Skipped"+
			" evaluation due to missing required element: client default_acr_values\",\"result\""+
			":\"INFO\",\"blockId\":\"2b8ee6\",\"path\":\"default_acr_values\",\"requirements\":[\"OIDCR-2\""+
			"],\"object\":\"client\",\"testId\":\"BeDwk5rd8H\",\"src\":\"ValidateDefaultAcrValues\",\"test"+
			"Owner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130872783},{"+
			"\"_id\":\"BeDwk5rd8H-zWTfWNctVMGsI7D7UkresbjU9pP2BKYJ\",\"msg\":\"Skipped evaluation du"+
			"e to missing required element: client initiate_login_uri\",\"result\":\"INFO\",\"block"+
			"Id\":\"2b8ee6\",\"path\":\"initiate_login_uri\",\"requirements\":[\"OIDCR-2\"],\"object\":\"cl"+
			"ient\",\"testId\":\"BeDwk5rd8H\",\"src\":\"ValidateInitiateLoginUri\",\"testOwner\":{\"sub\":"+
			"\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130872785},{\"_id\":\"BeDwk5r"+
			"d8H-RRrYu0Ft0ERV60sh2FrK7KswiEigzbUj\",\"msg\":\"Skipped evaluation due to missing r"+
			"equired element: client request_uris\",\"result\":\"INFO\",\"blockId\":\"2b8ee6\",\"path\":"+
			"\"request_uris\",\"requirements\":[\"OIDCR-2\"],\"object\":\"client\",\"testId\":\"BeDwk5rd8H"+
			"\",\"src\":\"ValidateRequestUris\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://deve"+
			"loper.com\"},\"time\":1590130872789},{\"_id\":\"BeDwk5rd8H-R3riFZpzS2QislXdwtkiuhy5GtZ"+
			"CH5uw\",\"msg\":\"A sector_identifier_uri was not provided\",\"result\":\"SUCCESS\",\"bloc"+
			"kId\":\"2b8ee6\",\"requirements\":[\"OIDCR-2\",\"OIDCR-5\"],\"testId\":\"BeDwk5rd8H\",\"src\":\""+
			"ValidateClientRegistrationRequestSectorIdentifierUri\",\"testOwner\":{\"sub\":\"develo"+
			"per\",\"iss\":\"https://developer.com\"},\"time\":1590130872790},{\"_id\":\"BeDwk5rd8H-Pi0"+
			"0j02GVVL58hAPeO4rbFRKDyKclXCu\",\"msg\":\"Registered client\",\"result\":\"SUCCESS\",\"blo"+
			"ckId\":\"2b8ee6\",\"client\":{\"tls_client_auth_san_dns\":\"dnsname2.conformance.example"+
			".com\",\"token_endpoint_auth_method\":\"tls_client_auth\",\"response_types\":[\"code\"],\""+
			"grant_types\":[\"authorization_code\"],\"redirect_uris\":[\"https://openid-client.loca"+
			"l/cb\"],\"client_id\":\"client_NuHAYAzlXPTuqLy36601\\u0027{^(]\"},\"testId\":\"BeDwk5rd8H"+
			"\",\"src\":\"OIDCCRegisterClient\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://deve"+
			"loper.com\"},\"time\":1590130872793},{\"_id\":\"BeDwk5rd8H-DtlxW63yvTLWwVhDWaRHdU9AU7v"+
			"9dJ44\",\"msg\":\"Client does not have both jwks and jwks_uri set\",\"result\":\"SUCCESS"+
			"\",\"blockId\":\"2b8ee6\",\"requirements\":[\"OIDCR-2\"],\"client\":{\"tls_client_auth_san_d"+
			"ns\":\"dnsname2.conformance.example.com\",\"token_endpoint_auth_method\":\"tls_client_"+
			"auth\",\"response_types\":[\"code\"],\"grant_types\":[\"authorization_code\"],\"redirect_u"+
			"ris\":[\"https://openid-client.local/cb\"],\"client_id\":\"client_NuHAYAzlXPTuqLy36601"+
			"\\u0027{^(]\"},\"testId\":\"BeDwk5rd8H\",\"src\":\"EnsureClientDoesNotHaveBothJwksAndJwks"+
			"Uri\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":159013"+
			"0872795},{\"_id\":\"BeDwk5rd8H-SBI2GxJvLDmKk0DuHZ38n1C5p4yrMlbH\",\"msg\":\"Skipped eva"+
			"luation due to missing required element: client jwks_uri\",\"result\":\"INFO\",\"block"+
			"Id\":\"2b8ee6\",\"path\":\"jwks_uri\",\"requirements\":[\"OIDCC-10.1.1\",\"OIDCC-10.2.1\"],\"o"+
			"bject\":\"client\",\"testId\":\"BeDwk5rd8H\",\"src\":\"FetchClientKeys\",\"testOwner\":{\"sub\""+
			":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130872796},{\"_id\":\"BeDwk5"+
			"rd8H-ND2KxMRoUqF3ZP248zR5qlVupQK7fYXt\",\"msg\":\"Using the default algorithm for th"+
			"e first key in server jwks\",\"result\":\"SUCCESS\",\"blockId\":\"2b8ee6\",\"signing_algor"+
			"ithm\":\"RS256\",\"testId\":\"BeDwk5rd8H\",\"src\":\"OIDCCExtractServerSigningAlg\",\"testOw"+
			"ner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130872800},{\"_"+
			"id\":\"BeDwk5rd8H-99Pl2QE3fTbHkzGv1Dv3lqZZ10vHtnYN\",\"msg\":\"Set id_token_signed_res"+
			"ponse_alg for the registered client\",\"blockId\":\"2b8ee6\",\"id_token_signed_respons"+
			"e_alg\":\"RS256\",\"testId\":\"BeDwk5rd8H\",\"src\":\"SetClientIdTokenSignedResponseAlgToS"+
			"erverSigningAlg\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\""+
			"time\":1590130872800},{\"_id\":\"BeDwk5rd8H-Xj3R7Jge61kZ93FnQf7EfSQnmKJXQU3s\",\"msg\":"+
			"\"Response to HTTP request to test instance BeDwk5rd8H\",\"outgoing_status_code\":20"+
			"1,\"outgoing_headers\":{},\"http\":\"outgoing\",\"outgoing_body\":{\"tls_client_auth_san_"+
			"dns\":\"dnsname2.conformance.example.com\",\"token_endpoint_auth_method\":\"tls_client"+
			"_auth\",\"response_types\":[\"code\"],\"grant_types\":[\"authorization_code\"],\"redirect_"+
			"uris\":[\"https://openid-client.local/cb\"],\"client_id\":\"client_NuHAYAzlXPTuqLy3660"+
			"1\\u0027{^(]\",\"id_token_signed_response_alg\":\"RS256\"},\"outgoing_path\":\"register\","+
			"\"testId\":\"BeDwk5rd8H\",\"src\":\"oidcc-client-test\",\"testOwner\":{\"sub\":\"developer\",\""+
			"iss\":\"https://developer.com\"},\"time\":1590130872803},{\"_id\":\"BeDwk5rd8H-lFiFT6mkZ"+
			"uuAj3qtlFi7cUJXcYSgVv9M\",\"msg\":\"Incoming HTTP request to test instance BeDwk5rd8"+
			"H\",\"incoming_headers\":{\"user-agent\":\"got/9.6.0 (https://github.com/sindresorhus/"+
			"got)\",\"accept-encoding\":\"gzip, deflate\",\"host\":\"localhost.emobix.co.uk:8443\",\"co"+
			"nnection\":\"close\",\"x-ssl-cipher\":\"ECDHE-RSA-AES128-GCM-SHA256\",\"x-ssl-protocol\":"+
			"\"TLSv1.2\"},\"incoming_path\":\"authorize\",\"http\":\"incoming\",\"incoming_method\":\"GET\""+
			",\"incoming_query_string_params\":{\"client_id\":\"client_NuHAYAzlXPTuqLy36601\\u0027{"+
			"^(]\",\"scope\":\"openid\",\"response_type\":\"code\",\"redirect_uri\":\"https://openid-clie"+
			"nt.local/cb\",\"state\":\"nNy6lv9HBdC5kFSMw2UjO44Ds7QoXSGvUomtAu7S_Og\"},\"testId\":\"Be"+
			"Dwk5rd8H\",\"src\":\"oidcc-client-test\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https:"+
			"//developer.com\"},\"time\":1590130872817},{\"_id\":\"BeDwk5rd8H-y3z4vdAgrbW0dQXlov8cZ"+
			"3WBGEv28hVn\",\"msg\":\"Authorization endpoint\",\"blockId\":\"d825e6\",\"startBlock\":true"+
			",\"testId\":\"BeDwk5rd8H\",\"src\":\"-START-BLOCK-\",\"testOwner\":{\"sub\":\"developer\",\"iss"+
			"\":\"https://developer.com\"},\"time\":1590130872820},{\"_id\":\"BeDwk5rd8H-6IbLVAOBdjnd"+
			"TifbVTQOkJdk109YuWU0\",\"msg\":\"Request does not contain a request parameter\",\"resu"+
			"lt\":\"SUCCESS\",\"blockId\":\"d825e6\",\"requirements\":[\"OIDCC-6.1\"],\"testId\":\"BeDwk5rd"+
			"8H\",\"src\":\"EnsureRequestDoesNotContainRequestObject\",\"testOwner\":{\"sub\":\"develop"+
			"er\",\"iss\":\"https://developer.com\"},\"time\":1590130872821},{\"_id\":\"BeDwk5rd8H-E2OH"+
			"deDGWlJ3IT1hVgG2fg3M8yBuQNXN\",\"msg\":\"Found \\u0027openid\\u0027 in scope http requ"+
			"est parameter\",\"result\":\"SUCCESS\",\"blockId\":\"d825e6\",\"actual\":[\"openid\"],\"requir"+
			"ements\":[\"OIDCC-6.2\",\"OIDCC-6.1\"],\"expected\":\"openid\",\"testId\":\"BeDwk5rd8H\",\"src"+
			"\":\"EnsureAuthorizationHttpRequestContainsOpenIDScope\",\"testOwner\":{\"sub\":\"d"+
			"eveloper\",\"iss\":\"https://developer.com\"},\"time\":1590130872822},{\"_id\":\"BeDwk5rd8"+
			"H-l1BSGOPLy7GTeUH3cStUlaCq2hPuJ6z2\",\"msg\":\"Merged http request parameters with r"+
			"equest object claims\",\"result\":\"SUCCESS\",\"blockId\":\"d825e6\",\"requirements\":[\"OID"+
			"CC-6.2\",\"OIDCC-6.1\"],\"effective_authorization_endpoint_request\":{\"client_id\":\"cl"+
			"ient_NuHAYAzlXPTuqLy36601\\u0027{^(]\",\"scope\":\"openid\",\"response_type\":\"code\",\"re"+
			"direct_uri\":\"https://openid-client.local/cb\",\"state\":\"nNy6lv9HBdC5kFSMw2UjO44Ds7"+
			"QoXSGvUomtAu7S_Og\"},\"testId\":\"BeDwk5rd8H\",\"src\":\"CreateEffectiveAuthorizationReq"+
			"uestParameters\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"t"+
			"ime\":1590130872823},{\"_id\":\"BeDwk5rd8H-9EqzPvRZ23T5nVvIjHePd7TimZaky1KR\",\"msg\":\""+
			"Requested scopes\",\"result\":\"SUCCESS\",\"blockId\":\"d825e6\",\"scope\":\"openid\",\"testId"+
			"\":\"BeDwk5rd8H\",\"src\":\"ExtractRequestedScopes\",\"testOwner\":{\"sub\":\"developer\",\"is"+
			"s\":\"https://developer.com\"},\"time\":1590130872825},{\"_id\":\"BeDwk5rd8H-srTdYnxgh5u"+
			"MHPSznW8iu74wyDnXNRfb\",\"msg\":\"Couldn\\u0027t find \\u0027nonce\\u0027 in authorizat"+
			"ion endpoint parameters\",\"result\":\"INFO\",\"blockId\":\"d825e6\",\"requirements\":[\"OID"+
			"CC-3.1.2.1\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"ExtractNonceFromAuthorizationRequest\","+
			"\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":15901308728"+
			"26},{\"_id\":\"BeDwk5rd8H-NClGuyhwJV364Ov96xDCDDucLYIBh4V5\",\"msg\":\"Response type is"+
			" expected value\",\"result\":\"SUCCESS\",\"blockId\":\"d825e6\",\"expected\":\"code\",\"testId"+
			"\":\"BeDwk5rd8H\",\"src\":\"EnsureResponseTypeIsCode\",\"testOwner\":{\"sub\":\"developer\",\""+
			"iss\":\"https://developer.com\"},\"time\":1590130872828},{\"_id\":\"BeDwk5rd8H-x7FtK07Bc"+
			"3mF1Xv01surCmIFNTL18eW2\",\"msg\":\"Client ID matched\",\"result\":\"SUCCESS\",\"blockId\":"+
			"\"d825e6\",\"requirements\":[\"OIDCC-3.1.2.1\"],\"client_id\":\"client_NuHAYAzlXPTuqLy366"+
			"01\\u0027{^(]\",\"testId\":\"BeDwk5rd8H\",\"src\":\"EnsureMatchingClientId\",\"testOwner\":{"+
			"\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130872830},{\"_id\":\"B"+
			"eDwk5rd8H-7JGy56hXaWA2mmzJziZ5pOF94rje0Hpw\",\"msg\":\"redirect_uri is one of the al"+
			"lowed redirect uris\",\"result\":\"SUCCESS\",\"blockId\":\"d825e6\",\"actual\":\"https://ope"+
			"nid-client.local/cb\",\"requirements\":[\"OIDCC-3.1.2.1\"],\"expected\":[\"https://openi"+
			"d-client.local/cb\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"EnsureValidRedirectUriForAuthor"+
			"izationEndpointRequest\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer."+
			"com\"},\"time\":1590130872833},{\"_id\":\"BeDwk5rd8H-qqh6CvMEWEUt3Ioq6ymwdMbYPRkPGYOq\""+
			",\"msg\":\"Found \\u0027openid\\u0027 scope in request\",\"result\":\"SUCCESS\",\"blockId\":"+
			"\"d825e6\",\"actual\":[\"openid\"],\"requirements\":[\"OIDCC-3.1.2.1\"],\"expected\":\"openid"+
			"\",\"testId\":\"BeDwk5rd8H\",\"src\":\"EnsureOpenIDInScopeRequest\",\"testOwner\":{\"sub\":\"d"+
			"eveloper\",\"iss\":\"https://developer.com\"},\"time\":1590130872835},{\"_id\":\"BeDwk5rd8"+
			"H-MBuowM9tJlnQvT1mC8qPv6ss5acN3XaM\",\"msg\":\"The client did not send max_age\\u003d"+
			"0 and prompt\\u003dnone parameters as expected\",\"result\":\"SUCCESS\",\"blockId\":\"d82"+
			"5e6\",\"requirements\":[\"OIDCC-3.1.2.3\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"DisallowMaxAg"+
			"eEqualsZeroAndPromptNone\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://develope"+
			"r.com\"},\"time\":1590130872837},{\"_id\":\"BeDwk5rd8H-9t34rb6mgFeSzh0Bt3EYZltRS3wvh30"+
			"D\",\"msg\":\"Created authorization code\",\"result\":\"SUCCESS\",\"blockId\":\"d825e6\",\"aut"+
			"horization_code\":\"lt1UTp51m3\",\"testId\":\"BeDwk5rd8H\",\"src\":\"CreateAuthorizationCo"+
			"de\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130"+
			"872838},{\"_id\":\"BeDwk5rd8H-U35RixvOXlGRAFkqdGQDdsYCT6tylXGO\",\"msg\":\"Successful c"+
			"_hash encoding\",\"result\":\"SUCCESS\",\"blockId\":\"d825e6\",\"c_hash\":\"WerU1VhwR83dR6S2"+
			"B42xZQ\",\"requirements\":[\"OIDCC-3.3.2.11\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"Calculate"+
			"CHash\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590"+
			"130872840},{\"_id\":\"BeDwk5rd8H-QcEakXQtDJKRKap4yzEgkH1gmXfRhhek\",\"msg\":\"Added aut"+
			"horization_endpoint_response_params to environment\",\"result\":\"SUCCESS\",\"blockId\""+
			":\"d825e6\",\"params\":{\"redirect_uri\":\"https://openid-client.local/cb\",\"state\":\"nNy"+
			"6lv9HBdC5kFSMw2UjO44Ds7QoXSGvUomtAu7S_Og\"},\"testId\":\"BeDwk5rd8H\",\"src\":\"CreateAu"+
			"thorizationEndpointResponseParams\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https:/"+
			"/developer.com\"},\"time\":1590130872841},{\"_id\":\"BeDwk5rd8H-4oOwSfjY4YMe0R6Lk0cLsi"+
			"ZXGal1dPtm\",\"msg\":\"Added code to authorization endpoint response params\",\"result"+
			"\":\"SUCCESS\",\"blockId\":\"d825e6\",\"requirements\":[\"OIDCC-3.3.2.5\"],\"authorization_e"+
			"ndpoint_response_params\":{\"redirect_uri\":\"https://openid-client.local/cb\",\"state"+
			"\":\"nNy6lv9HBdC5kFSMw2UjO44Ds7QoXSGvUomtAu7S_Og\",\"code\":\"lt1UTp51m3\"},\"testId\":\"B"+
			"eDwk5rd8H\",\"src\":\"AddCodeToAuthorizationEndpointResponseParams\",\"testOwner\":{\"su"+
			"b\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130872843},{\"_id\":\"BeDw"+
			"k5rd8H-7WGSRXgEgSaZWdtl2Mim6fWcMq0N0FuR\",\"msg\":\"Redirecting back to client\",\"blo"+
			"ckId\":\"d825e6\",\"uri\":\"https://openid-client.local/cb?state\\u003dnNy6lv9HBdC5kFSM"+
			"w2UjO44Ds7QoXSGvUomtAu7S_Og\\u0026code\\u003dlt1UTp51m3\",\"testId\":\"BeDwk5rd8H\",\"sr"+
			"c\":\"SendAuthorizationResponseWithResponseModeQuery\",\"testOwner\":{\"sub\":\"develope"+
			"r\",\"iss\":\"https://developer.com\"},\"time\":1590130872845},{\"_id\":\"BeDwk5rd8H-u2dO6"+
			"RKKpt0qYVK8H3D2aj1NX0rRumjs\",\"msg\":\"Response to HTTP request to test instance Be"+
			"Dwk5rd8H\",\"outgoing\":\"org.springframework.web.servlet.view.RedirectView: [Redire"+
			"ctView]; URL [https://openid-client.local/cb?state\\u003dnNy6lv9HBdC5kFSMw2UjO44D"+
			"s7QoXSGvUomtAu7S_Og\\u0026code\\u003dlt1UTp51m3]\",\"http\":\"outgoing\",\"outgoing_path"+
			"\":\"authorize\",\"testId\":\"BeDwk5rd8H\",\"src\":\"oidcc-client-test\",\"testOwner\":{\"sub\""+
			":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130872848},{\"_id\":\"BeDwk5"+
			"rd8H-AtTQZrBwQ4ipaqaM7rBxm74GB2sBK00M\",\"msg\":\"Incoming HTTP request to test inst"+
			"ance BeDwk5rd8H\",\"incoming_headers\":{\"user-agent\":\"openid-client/3.14.1 (https:/"+
			"/github.com/panva/node-openid-client)\",\"accept\":\"application/json\",\"accept-encod"+
			"ing\":\"gzip, deflate\",\"content-type\":\"application/x-www-form-urlencoded\",\"content"+
			"-length\":\"150\",\"host\":\"localhost.emobix.co.uk:8443\",\"connection\":\"close\",\"x-ssl-"+
			"cipher\":\"ECDHE-RSA-AES128-GCM-SHA256\",\"x-ssl-protocol\":\"TLSv1.2\",\"x-ssl-cert\":\"-"+
			"----BEGIN CERTIFICATE----- MIIGCDCCA/CgAwIBAgIJAIgwloUBq+0LMA0GCSqGSIb3DQEBCwUAM"+
			"GYxCzAJBgNV BAYTAlVTMQswCQYDVQQIDAJDQTESMBAGA1UEBwwJU2FuIFJhbW9uMQ0wCwYDVQQK DAR"+
			"PSURGMScwJQYDVQQDDB50ZXN0LmNlcnRpZmljYXRpb24uZXhhbXBsZS5jb20w HhcNMjAwNTIxMDYzNT"+
			"AyWhcNMzAwNTE5MDYzNTAyWjBmMQswCQYDVQQGEwJVUzEL MAkGA1UECAwCQ0ExEjAQBgNVBAcMCVNhb"+
			"iBSYW1vbjENMAsGA1UECgwET0lERjEn MCUGA1UEAwwedGVzdC5jZXJ0aWZpY2F0aW9uLmV4YW1wbGUu"+
			"Y29tMIICIjANBgkq hkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAwFCUwv8D5ReG/bXSiHSuLQ0laRG+paS"+
			"T PlXLhflpGh4pWsHoPaLHmIwScXKDYZVUGtvLzQkkFT6QODn3FXWogxkzNCLpbpvk iiHK/1NdibYeK"+
			"aWj0CnaFj6v5ObpsMo5QpQkCPlOPaARGjnsNoEVBhqFdbp4xqiw 6ZmBBNYt8iZ8D9ta50UJmwYPyOiw"+
			"yacKni5XBjfbjq7M4pubHSg1fgzDxlJdSEKa n9nNMTLRXor3qvJLUpbDR4x+NjKfezlpeKI3Y0GyEdB"+
			"0DxOYJSOgF8wq6gSMTr7F SAVacXRKIxyzAICmFZj4wYGQ2wYhQXB+wgbMw9zmD25NlwqTHQLcN9fi2E"+
			"w/Abvu J79OoMDLZrbInXKtPObv0Szz7A2tBv4O6juQRlkTfs27z1K1hFr+MKzXNd4vgGVv PUIdluKZ"+
			"zxx5qGcg/ya07LRl4qn06zm+ectdCeKUuFQi3q0YVE3NSRXgv2VTpzWb 5zy4iJlxifiInp3miiYP9kf"+
			"RKR7DBLWV6/RsrObmgJUeUjiwFEfimaeM5NFKYwdE TLhAB/1L28EevM5k6jdRfDHsqGbEL5T630G6iw"+
			"u1Ih+N7GNVobkJuzOYPtwyxRwN 8vcVVCvQ2k41sNbpnacg8SyRBepeiIQR+xC1avvfF148tUwnhz1vQ"+
			"qR4Cx3Pbrsh +V21WCWsH9MCAwEAAaOBuDCBtTCBsgYDVR0RBIGqMIGngg93d3cuZXhhbXBsZS5j b22"+
			"CIGRuc25hbWUyLmNvbmZvcm1hbmNlLmV4YW1wbGUuY29thwR/AAABhwQKAAAB hiRodHRwczovL3d3dy"+
			"5jZXJ0aWZpY2F0aW9uLm9wZW5pZC5uZXSGEWh0dHBzOi8v bG9jYWxob3N0gRhjZXJ0aWZpY2F0aW9uQ"+
			"G9wZW5pZC5uZXSBE2pvaG5kb2VAZXhh bXBsZS5jb20wDQYJKoZIhvcNAQELBQADggIBAENIqUWJgwFq"+
			"9eXWkM3yWZ1p2HqV e0dAwCBPMT1wUQ8OdzPgR9AzZxAhv3uqHmCqEY8eeFXQyMgz9lNPjTvnzVQxAFH"+
			"4 SqH20S3mh/ymMSMaZsHb8/acziXtY6qtTpwwjJmp9szx+fXlMrssr51HAivbI1ea PI2PzpwgHJIlt"+
			"hg5DSbvoYhNuvUtv33N9zzOcFTBLcGcdLXeVisnCXMmltweyUM3 AKqT9eMWZfxCMg69eFPNs9nvQ1u9"+
			"BQqPYns2illfFdtL4hN6S6v4WjUiUS2IEmmJ h8k9xLHwb8ZQucdOb3V4ybHGqx7/aigHOcvpbUL+4aA"+
			"uzyVtU20QR3wQXJ9dlRyk kpz+RJLH2h45JaWtS4T0dv+NmATXjIcpEqRMDRpZT4y35wkMfX375CBaV0"+
			"OAm+0T 1DvD9NFa2HQUQTV/vedJIXavF0yswuaPyY8sKpH4v66FpZJYpO8K1O+JGM3/sGgE X3LhcekI"+
			"OnSZzwBraRian7u1fJhfSmAUlBcnaxtMpZ2XVGvtedz+NA9lkXvD9RI9 u/9XJsTKaQn35FbGG9W18bm"+
			"9JkB9IU08hekygqSJK9v/6ajD6S1U5SBgrQ7GfQya ygd6HbUDO51D2El4vqxBcbMlzPEItj11b0xaBM"+
			"1ZuF64/s3BzzwRyuyTOU5gu4SL PjBLfDw2pK+NfvAb -----END CERTIFICATE----- \",\"x-ssl-v"+
			"erify\":\"FAILED:self signed certificate\"},\"incoming_path\":\"token\",\"incoming_body_"+
			"form_params\":{\"grant_type\":\"authorization_code\",\"code\":\"lt1UTp51m3\",\"redirect_ur"+
			"i\":\"https://openid-client.local/cb\",\"client_id\":\"client_NuHAYAzlXPTuqLy36601\\u00"+
			"27{^(]\"},\"http\":\"incoming\",\"incoming_method\":\"POST\",\"incoming_query_string_param"+
			"s\":{},\"incoming_body\":\"grant_type\\u003dauthorization_code\\u0026code\\u003dlt1UTp5"+
			"1m3\\u0026redirect_uri\\u003dhttps%3A%2F%2Fopenid-client.local%2Fcb\\u0026client_id"+
			"\\u003dclient_NuHAYAzlXPTuqLy36601%27%7B%5E%28%5D\",\"testId\":\"BeDwk5rd8H\",\"src\":\"o"+
			"idcc-client-test\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},"+
			"\"time\":1590130872896},{\"_id\":\"BeDwk5rd8H-vRKvomYCfEbl17fCLvdq53bteGyyYOhn\",\"msg\""+
			":\"Token endpoint\",\"blockId\":\"445471\",\"startBlock\":true,\"testId\":\"BeDwk5rd8H\",\"sr"+
			"c\":\"-START-BLOCK-\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"}"+
			",\"time\":1590130872899},{\"_id\":\"BeDwk5rd8H-N2FSDHbzZo7viHmQBPhDqzhUYbKYHcyM\",\"msg"+
			"\":\"Request parameters contain a valid client_id parameter\",\"result\":\"SUCCESS\",\"b"+
			"lockId\":\"445471\",\"requirements\":[\"RFC8705-2\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"Ensur"+
			"eMTLSRequestContainsValidClientId\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https:/"+
			"/developer.com\"},\"time\":1590130872901},{\"_id\":\"BeDwk5rd8H-0uhJKRSKKyoBkFCOvgQY5Z"+
			"mnghqhi6Sj\",\"msg\":\"Extracted client certificate\",\"result\":\"SUCCESS\",\"blockId\":\"4"+
			"45471\",\"client_certificate\":{\"cert\":\"-----BEGIN CERTIFICATE----- MIIGCDCCA/CgAwI"+
			"BAgIJAIgwloUBq+0LMA0GCSqGSIb3DQEBCwUAMGYxCzAJBgNV BAYTAlVTMQswCQYDVQQIDAJDQTESMB"+
			"AGA1UEBwwJU2FuIFJhbW9uMQ0wCwYDVQQK DARPSURGMScwJQYDVQQDDB50ZXN0LmNlcnRpZmljYXRpb"+
			"24uZXhhbXBsZS5jb20w HhcNMjAwNTIxMDYzNTAyWhcNMzAwNTE5MDYzNTAyWjBmMQswCQYDVQQGEwJV"+
			"UzEL MAkGA1UECAwCQ0ExEjAQBgNVBAcMCVNhbiBSYW1vbjENMAsGA1UECgwET0lERjEn MCUGA1UEAw"+
			"wedGVzdC5jZXJ0aWZpY2F0aW9uLmV4YW1wbGUuY29tMIICIjANBgkq hkiG9w0BAQEFAAOCAg8AMIICC"+
			"gKCAgEAwFCUwv8D5ReG/bXSiHSuLQ0laRG+paST PlXLhflpGh4pWsHoPaLHmIwScXKDYZVUGtvLzQkk"+
			"FT6QODn3FXWogxkzNCLpbpvk iiHK/1NdibYeKaWj0CnaFj6v5ObpsMo5QpQkCPlOPaARGjnsNoEVBhq"+
			"Fdbp4xqiw 6ZmBBNYt8iZ8D9ta50UJmwYPyOiwyacKni5XBjfbjq7M4pubHSg1fgzDxlJdSEKa n9nNM"+
			"TLRXor3qvJLUpbDR4x+NjKfezlpeKI3Y0GyEdB0DxOYJSOgF8wq6gSMTr7F SAVacXRKIxyzAICmFZj4"+
			"wYGQ2wYhQXB+wgbMw9zmD25NlwqTHQLcN9fi2Ew/Abvu J79OoMDLZrbInXKtPObv0Szz7A2tBv4O6ju"+
			"QRlkTfs27z1K1hFr+MKzXNd4vgGVv PUIdluKZzxx5qGcg/ya07LRl4qn06zm+ectdCeKUuFQi3q0YVE"+
			"3NSRXgv2VTpzWb 5zy4iJlxifiInp3miiYP9kfRKR7DBLWV6/RsrObmgJUeUjiwFEfimaeM5NFKYwdE "+
			"TLhAB/1L28EevM5k6jdRfDHsqGbEL5T630G6iwu1Ih+N7GNVobkJuzOYPtwyxRwN 8vcVVCvQ2k41sNb"+
			"pnacg8SyRBepeiIQR+xC1avvfF148tUwnhz1vQqR4Cx3Pbrsh +V21WCWsH9MCAwEAAaOBuDCBtTCBsg"+
			"YDVR0RBIGqMIGngg93d3cuZXhhbXBsZS5j b22CIGRuc25hbWUyLmNvbmZvcm1hbmNlLmV4YW1wbGUuY"+
			"29thwR/AAABhwQKAAAB hiRodHRwczovL3d3dy5jZXJ0aWZpY2F0aW9uLm9wZW5pZC5uZXSGEWh0dHBz"+
			"Oi8v bG9jYWxob3N0gRhjZXJ0aWZpY2F0aW9uQG9wZW5pZC5uZXSBE2pvaG5kb2VAZXhh bXBsZS5jb2"+
			"0wDQYJKoZIhvcNAQELBQADggIBAENIqUWJgwFq9eXWkM3yWZ1p2HqV e0dAwCBPMT1wUQ8OdzPgR9AzZ"+
			"xAhv3uqHmCqEY8eeFXQyMgz9lNPjTvnzVQxAFH4 SqH20S3mh/ymMSMaZsHb8/acziXtY6qtTpwwjJmp"+
			"9szx+fXlMrssr51HAivbI1ea PI2PzpwgHJIlthg5DSbvoYhNuvUtv33N9zzOcFTBLcGcdLXeVisnCXM"+
			"mltweyUM3 AKqT9eMWZfxCMg69eFPNs9nvQ1u9BQqPYns2illfFdtL4hN6S6v4WjUiUS2IEmmJ h8k9x"+
			"LHwb8ZQucdOb3V4ybHGqx7/aigHOcvpbUL+4aAuzyVtU20QR3wQXJ9dlRyk kpz+RJLH2h45JaWtS4T0"+
			"dv+NmATXjIcpEqRMDRpZT4y35wkMfX375CBaV0OAm+0T 1DvD9NFa2HQUQTV/vedJIXavF0yswuaPyY8"+
			"sKpH4v66FpZJYpO8K1O+JGM3/sGgE X3LhcekIOnSZzwBraRian7u1fJhfSmAUlBcnaxtMpZ2XVGvted"+
			"z+NA9lkXvD9RI9 u/9XJsTKaQn35FbGG9W18bm9JkB9IU08hekygqSJK9v/6ajD6S1U5SBgrQ7GfQya "+
			"ygd6HbUDO51D2El4vqxBcbMlzPEItj11b0xaBM1ZuF64/s3BzzwRyuyTOU5gu4SL PjBLfDw2pK+NfvA"+
			"b -----END CERTIFICATE----- \",\"pem\":\"-----BEGIN CERTIFICATE-----\\nMIIGCDCCA/CgAw"+
			"IBAgIJAIgwloUBq+0LMA0GCSqGSIb3DQEBCwUAMGYxCzAJBgNV\\nBAYTAlVTMQswCQYDVQQIDAJDQTES"+
			"MBAGA1UEBwwJU2FuIFJhbW9uMQ0wCwYDVQQK\\nDARPSURGMScwJQYDVQQDDB50ZXN0LmNlcnRpZmljYX"+
			"Rpb24uZXhhbXBsZS5jb20w\\nHhcNMjAwNTIxMDYzNTAyWhcNMzAwNTE5MDYzNTAyWjBmMQswCQYDVQQG"+
			"EwJVUzEL\\nMAkGA1UECAwCQ0ExEjAQBgNVBAcMCVNhbiBSYW1vbjENMAsGA1UECgwET0lERjEn\\nMCUG"+
			"A1UEAwwedGVzdC5jZXJ0aWZpY2F0aW9uLmV4YW1wbGUuY29tMIICIjANBgkq\\nhkiG9w0BAQEFAAOCAg"+
			"8AMIICCgKCAgEAwFCUwv8D5ReG/bXSiHSuLQ0laRG+paST\\nPlXLhflpGh4pWsHoPaLHmIwScXKDYZVU"+
			"GtvLzQkkFT6QODn3FXWogxkzNCLpbpvk\\niiHK/1NdibYeKaWj0CnaFj6v5ObpsMo5QpQkCPlOPaARGj"+
			"nsNoEVBhqFdbp4xqiw\\n6ZmBBNYt8iZ8D9ta50UJmwYPyOiwyacKni5XBjfbjq7M4pubHSg1fgzDxlJd"+
			"SEKa\\nn9nNMTLRXor3qvJLUpbDR4x+NjKfezlpeKI3Y0GyEdB0DxOYJSOgF8wq6gSMTr7F\\nSAVacXRK"+
			"IxyzAICmFZj4wYGQ2wYhQXB+wgbMw9zmD25NlwqTHQLcN9fi2Ew/Abvu\\nJ79OoMDLZrbInXKtPObv0S"+
			"zz7A2tBv4O6juQRlkTfs27z1K1hFr+MKzXNd4vgGVv\\nPUIdluKZzxx5qGcg/ya07LRl4qn06zm+ectd"+
			"CeKUuFQi3q0YVE3NSRXgv2VTpzWb\\n5zy4iJlxifiInp3miiYP9kfRKR7DBLWV6/RsrObmgJUeUjiwFE"+
			"fimaeM5NFKYwdE\\nTLhAB/1L28EevM5k6jdRfDHsqGbEL5T630G6iwu1Ih+N7GNVobkJuzOYPtwyxRwN"+
			"\\n8vcVVCvQ2k41sNbpnacg8SyRBepeiIQR+xC1avvfF148tUwnhz1vQqR4Cx3Pbrsh\\n+V21WCWsH9MC"+
			"AwEAAaOBuDCBtTCBsgYDVR0RBIGqMIGngg93d3cuZXhhbXBsZS5j\\nb22CIGRuc25hbWUyLmNvbmZvcm"+
			"1hbmNlLmV4YW1wbGUuY29thwR/AAABhwQKAAAB\\nhiRodHRwczovL3d3dy5jZXJ0aWZpY2F0aW9uLm9w"+
			"ZW5pZC5uZXSGEWh0dHBzOi8v\\nbG9jYWxob3N0gRhjZXJ0aWZpY2F0aW9uQG9wZW5pZC5uZXSBE2pvaG"+
			"5kb2VAZXhh\\nbXBsZS5jb20wDQYJKoZIhvcNAQELBQADggIBAENIqUWJgwFq9eXWkM3yWZ1p2HqV\\ne0"+
			"dAwCBPMT1wUQ8OdzPgR9AzZxAhv3uqHmCqEY8eeFXQyMgz9lNPjTvnzVQxAFH4\\nSqH20S3mh/ymMSMa"+
			"ZsHb8/acziXtY6qtTpwwjJmp9szx+fXlMrssr51HAivbI1ea\\nPI2PzpwgHJIlthg5DSbvoYhNuvUtv3"+
			"3N9zzOcFTBLcGcdLXeVisnCXMmltweyUM3\\nAKqT9eMWZfxCMg69eFPNs9nvQ1u9BQqPYns2illfFdtL"+
			"4hN6S6v4WjUiUS2IEmmJ\\nh8k9xLHwb8ZQucdOb3V4ybHGqx7/aigHOcvpbUL+4aAuzyVtU20QR3wQXJ"+
			"9dlRyk\\nkpz+RJLH2h45JaWtS4T0dv+NmATXjIcpEqRMDRpZT4y35wkMfX375CBaV0OAm+0T\\n1DvD9N"+
			"Fa2HQUQTV/vedJIXavF0yswuaPyY8sKpH4v66FpZJYpO8K1O+JGM3/sGgE\\nX3LhcekIOnSZzwBraRia"+
			"n7u1fJhfSmAUlBcnaxtMpZ2XVGvtedz+NA9lkXvD9RI9\\nu/9XJsTKaQn35FbGG9W18bm9JkB9IU08he"+
			"kygqSJK9v/6ajD6S1U5SBgrQ7GfQya\\nygd6HbUDO51D2El4vqxBcbMlzPEItj11b0xaBM1ZuF64/s3B"+
			"zzwRyuyTOU5gu4SL\\nPjBLfDw2pK+NfvAb\\n-----END CERTIFICATE-----\\n\",\"subject\":{\"dn\""+
			":\"cn\\u003dtest.certification.example.com,o\\u003doidf,l\\u003dsan ramon,st\\u003dca"+
			",c\\u003dus\"},\"sanDnsNames\":[\"www.example.com\",\"dnsname2.conformance.example.com\""+
			"],\"sanUris\":[\"https://www.certification.openid.net\",\"https://localhost\"],\"sanIPs"+
			"\":[\"127.0.0.1\",\"10.0.0.1\"],\"sanEmails\":[\"certification@openid.net\",\"johndoe@exam"+
			"ple.com\"]},\"testId\":\"BeDwk5rd8H\",\"src\":\"OIDCCExtractClientCertificateFromTokenEn"+
			"dpointRequestHeaders\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.co"+
			"m\"},\"time\":1590130872903},{\"_id\":\"BeDwk5rd8H-f8C3t0gksxonWxQLqlLp7tOP9JEbWZyX\",\""+
			"msg\":\"Found client certificate\",\"result\":\"SUCCESS\",\"blockId\":\"445471\",\"testId\":\""+
			"BeDwk5rd8H\",\"src\":\"CheckForClientCertificate\",\"testOwner\":{\"sub\":\"developer\",\"is"+
			"s\":\"https://developer.com\"},\"time\":1590130872904},{\"_id\":\"BeDwk5rd8H-dB98doqKhjH"+
			"wgzSBuETJZjUbI9mGSPNA\",\"msg\":\"Certificate contains the expected tls_client_auth_"+
			"san_dns\",\"result\":\"SUCCESS\",\"blockId\":\"445471\",\"actual\":[\"www.example.com\",\"dnsn"+
			"ame2.conformance.example.com\"],\"requirements\":[\"RFC8705-2.1.2\"],\"expected\":\"dnsn"+
			"ame2.conformance.example.com\",\"testId\":\"BeDwk5rd8H\",\"src\":\"ValidateClientCertifi"+
			"cateForTlsClientAuth\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.co"+
			"m\"},\"time\":1590130872907},{\"_id\":\"BeDwk5rd8H-EwM4hPeqnDKaew5actL5oMDJO6eA7n20\",\""+
			"msg\":\"Found authorization code\",\"result\":\"SUCCESS\",\"blockId\":\"445471\",\"requireme"+
			"nts\":[\"OIDCC-3.1.3.2\"],\"authorization_code\":\"lt1UTp51m3\",\"testId\":\"BeDwk5rd8H\",\""+
			"src\":\"ValidateAuthorizationCode\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://d"+
			"eveloper.com\"},\"time\":1590130872908},{\"_id\":\"BeDwk5rd8H-dPXuYsxTfGo4VMOB4XI6ivL9"+
			"d03pt98s\",\"msg\":\"redirect_uri is the same as the one used in the authorization r"+
			"equest\",\"result\":\"SUCCESS\",\"blockId\":\"445471\",\"actual\":\"https://openid-client.lo"+
			"cal/cb\",\"requirements\":[\"OIDCC-3.1.3.2\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"ValidateRe"+
			"directUriForTokenEndpointRequest\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://"+
			"developer.com\"},\"time\":1590130872910},{\"_id\":\"BeDwk5rd8H-kBPC0v6JnwNz22XF4i3FXGf"+
			"R4LmSclRi\",\"access_token\":\"I20dmjAIhDd5IqFQfld0GFPtDeGhB97rV3JvANyxzYKlH5JFn1\",\""+
			"msg\":\"Generated access token\",\"result\":\"SUCCESS\",\"blockId\":\"445471\",\"testId\":\"Be"+
			"Dwk5rd8H\",\"src\":\"GenerateBearerAccessToken\",\"testOwner\":{\"sub\":\"developer\",\"iss\""+
			":\"https://developer.com\"},\"time\":1590130872911},{\"_id\":\"BeDwk5rd8H-cfSXfnO2Dg8Ld"+
			"onz8EOrvycwGiFhgyGT\",\"at_hash\":\"h0_lnbi4w6yP_4NZ3w_hVw\",\"msg\":\"Successful at_has"+
			"h encoding\",\"result\":\"SUCCESS\",\"blockId\":\"445471\",\"requirements\":[\"OIDCC-3.3.2.1"+
			"1\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"CalculateAtHash\",\"testOwner\":{\"sub\":\"developer\""+
			",\"iss\":\"https://developer.com\"},\"time\":1590130872913},{\"_id\":\"BeDwk5rd8H-qm5IaVl"+
			"TS3VxoQB7YdDBHE0woJ5Qjfam\",\"iss\":\"https://localhost.emobix.co.uk:8443/test/a/ope"+
			"nid-client/\",\"sub\":\"user-subject-1234531\",\"aud\":\"client_NuHAYAzlXPTuqLy36601\\u00"+
			"27{^(]\",\"iat\":1590130872,\"exp\":1590131172,\"msg\":\"Created ID Token Claims\",\"resul"+
			"t\":\"SUCCESS\",\"blockId\":\"445471\",\"testId\":\"BeDwk5rd8H\",\"src\":\"GenerateIdTokenClai"+
			"ms\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130"+
			"872915},{\"_id\":\"BeDwk5rd8H-z74RGKw7QXrUq3gm7rm8GpdSHTqFoni5\",\"at_hash\":\"h0_lnbi4"+
			"w6yP_4NZ3w_hVw\",\"msg\":\"Added at_hash to ID token claims\",\"result\":\"SUCCESS\",\"blo"+
			"ckId\":\"445471\",\"requirements\":[\"OIDCC-3.3.2.11\"],\"id_token_claims\":{\"iss\":\"https"+
			"://localhost.emobix.co.uk:8443/test/a/openid-client/\",\"sub\":\"user-subject-123453"+
			"1\",\"aud\":\"client_NuHAYAzlXPTuqLy36601\\u0027{^(]\",\"iat\":1590130872,\"exp\":15901311"+
			"72,\"at_hash\":\"h0_lnbi4w6yP_4NZ3w_hVw\"},\"testId\":\"BeDwk5rd8H\",\"src\":\"AddAtHashToI"+
			"dTokenClaims\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"tim"+
			"e\":1590130872916},{\"_id\":\"BeDwk5rd8H-LFQwMdgcctAH0lFiRAY9R5sSx1hsT3wo\",\"msg\":\"Si"+
			"gned the ID token\",\"result\":\"SUCCESS\",\"blockId\":\"445471\",\"requirements\":[\"OIDCC-"+
			"2\"],\"id_token\":{\"verifiable_jws\":\"eyJraWQiOiJhOWIwMmZkYS1iODY0LTQ5ODgtOTc2Ni1kYj"+
			"A3YmIyZDE1N2QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiaDBfbG5iaTR3NnlQXzROWjN3X2hWd"+
			"yIsInN1YiI6InVzZXItc3ViamVjdC0xMjM0NTMxIiwiYXVkIjoiY2xpZW50X051SEFZQXpsWFBUdXFMe"+
			"TM2NjAxJ3teKF0iLCJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0LmVtb2JpeC5jby51azo4NDQzXC90Z"+
			"XN0XC9hXC9vcGVuaWQtY2xpZW50XC8iLCJleHAiOjE1OTAxMzExNzIsImlhdCI6MTU5MDEzMDg3Mn0.K"+
			"pK4RsS1nl_QWwJp9ESATmrxN4HCaQgy2Tye1gu0ZAHBlmxI6cPUnu-6CUSzvB-jbPVsHMbHoY2c5l4a9"+
			"qVpdrU855QzhfFBPjzSH_RNmzODCF6q3BMRT6eNr7lDyXUzGdkfHkA8MkgKaMXz-Y4XC-Fu77bLz_H8f"+
			"w4gthdOU3tYmW0k_sKJhqTNUh77AVNqFJpeK8hgXYNXkSrrNtT0RVrRIW40fxgPhsQuX1oqsuYUdtasA"+
			"OrXnOsxZC3TpdA1OS1qQ20gCEsVaiZefuXjaw86hGfQcCoJz98iNqA1BaKf5I90bcYIF6Q-O_ZoBMCgA"+
			"v0BA6No3uXF9-2vCjRjyw\",\"public_jwk\":\"{\\\"kty\\\":\\\"RSA\\\",\\\"e\\\":\\\"AQAB\\\",\\\"use\\\":\\\"s"+
			"ig\\\",\\\"kid\\\":\\\"a9b02fda-b864-4988-9766-db07bb2d157d\\\",\\\"n\\\":\\\"qENsnVVGCL_IQALmPp"+
			"y2KzBYOGVYms5DodhhtHcverJoVoYjRqs_vvP64lVNPsiXsgStwGQ3HrLiCpFUhdFHQ1-1dlRE6-0oHM"+
			"55hTkDd0Y7TBld0PDnDTN-B7q91r_JLsNspdVQpTavoo5qPZSV_bvxSlBTopMuMpxMBnmMKE20KiAcAV"+
			"hJnlO7v97ILlIiYSCNwx0h5kmAHUZlhpw0DgE9yPVY5osbmb1QjIbZRzbE6NRNFlJiXnTeraR89kHFfv"+
			"yEeeIFTwFgkKPqFN0dZyWqd_zBO73yk1LruN5IDGwrpDbK3hOSHqfExqJFi8qxISmuq4k3-bfllzKMz7"+
			"HihQ\\\"}\"},\"key\":\"{\\\"p\\\":\\\"92NKQx2naIbszd7dmyOkNILSVBus4BVjLUkfk46ts6l2c1NHRGiQ3e"+
			"z8YO1tU43BVpZ8REMt431JoqsLA7Hz_-2N0tEQRj4ovRBGoXF__vzj9OYEE5si6hzGdeK_f-syXgX2Ht"+
			"6vsnkemRx5H7MZK8gcKFbQlab7HcqfRfmfAEM\\\",\\\"kty\\\":\\\"RSA\\\",\\\"q\\\":\\\"rh768jKSkXFs291c"+
			"RkodvAQBo17amsT6Z-jxixP4oyJ4dQBl87GyAc5_Bch5k7NTIhXCnE77wpHzCkaWdLj4CdYVXRtLFyNi"+
			"JM1Fe6_-v8wZhrB1KQKd0kCGpmR3MeBKNQTf-EWD6oFM13QsFL-aqTfZHCMSbc7ulXZV-43KKZc\\\",\\\""+
			"d\\\":\\\"FZehZoc8136U2fIFTd-aWOx5RbNrrWHut-QmlZv90cb3MLNB752mLoDsnwLHSNlA7i0_hsLVd_"+
			"EIpMaFjC6Q8IplpOundIcYcKnmWjw4v6OZlGhk650eaUlEUQWbuuUdkgyt8pHvKWRdIYUZCRH6NXdavW"+
			"iVcvVkJkZ8UxBh9gHcl5m1pOD445m1mhYdLnTSkOrnFiVuLE938_sAP9ybKhFkOgqpes2CMUFQGqSn0D"+
			"Wyhsu-Ay-fCqNxbmkMujykPKR3_zlab1SXabASMuDXT4Bkk7fm2sdVn5UbS8BvvaGBeZ05Mw8UTDQZsl"+
			"vpR8jxMqmhPwHPF2Lsfria4onCeQ\\\",\\\"e\\\":\\\"AQAB\\\",\\\"use\\\":\\\"sig\\\",\\\"kid\\\":\\\"a9b02fda"+
			"-b864-4988-9766-db07bb2d157d\\\",\\\"qi\\\":\\\"CoiJ9KELZ3XQNah5uLXWB_OpZCzDpO9hnoSKrFXE"+
			"nCDlztWamCqi5EgyMSeH9yHE4y8KnGTnZUpkYStUIBm1XHZ4lnPlotpvJii0fMBgVSxOXHFs_os8bTGT"+
			"Ltk6skppvYLHvfDM4BNjABgQFsjJShHdbc2NMbc27rrehz1uGFU\\\",\\\"dp\\\":\\\"ja-5Va1DN1OnhtVw-"+
			"Ky2HDkc-30KSyEUt1gdFKbwB2k0ZSK6O4zkA6b6eQ0iw2r543y22WXcf2E_bkImQcyG4fqNcsO4XXK2Z"+
			"GpQ-aAG3eS7LoUaqpv8qDco11WxtjLfmwgxCUcjO8Ww56JuIOCztuaaceQe_McxJcPp6urnzP8\\\",\\\"d"+
			"q\\\":\\\"kZFo7u8Exf9zqOSJbmIsNbGLxQ9Z5yZiylhfB4zsw0XgOsDYe0HFtejzHNybd7Xl_IsApjkKFY"+
			"J7E84rnu28wExwk-RrhK6aMHZEbsGWUrbJaGdBq8V6N_qmF5nKZLJEl8q5jBBh1r2-himjaL0_CdWc9v"+
			"8DOzZTFDVYcPP7XYU\\\",\\\"n\\\":\\\"qENsnVVGCL_IQALmPpy2KzBYOGVYms5DodhhtHcverJoVoYjRqs_"+
			"vvP64lVNPsiXsgStwGQ3HrLiCpFUhdFHQ1-1dlRE6-0oHM55hTkDd0Y7TBld0PDnDTN-B7q91r_JLsNs"+
			"pdVQpTavoo5qPZSV_bvxSlBTopMuMpxMBnmMKE20KiAcAVhJnlO7v97ILlIiYSCNwx0h5kmAHUZlhpw0"+
			"DgE9yPVY5osbmb1QjIbZRzbE6NRNFlJiXnTeraR89kHFfvyEeeIFTwFgkKPqFN0dZyWqd_zBO73yk1Lr"+
			"uN5IDGwrpDbK3hOSHqfExqJFi8qxISmuq4k3-bfllzKMz7HihQ\\\"}\",\"algorithm\":\"RS256\",\"test"+
			"Id\":\"BeDwk5rd8H\",\"src\":\"OIDCCSignIdToken\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\""+
			"https://developer.com\"},\"time\":1590130872922},{\"_id\":\"BeDwk5rd8H-Zjweo8SK2E3sXYV"+
			"MdFANRnUaM7RDedM0\",\"msg\":\"Skipped evaluation due to missing required element: cl"+
			"ient id_token_encrypted_response_alg\",\"result\":\"INFO\",\"blockId\":\"445471\",\"path\":"+
			"\"id_token_encrypted_response_alg\",\"requirements\":[\"OIDCC-10.2\"],\"object\":\"client"+
			"\",\"testId\":\"BeDwk5rd8H\",\"src\":\"EncryptIdToken\",\"testOwner\":{\"sub\":\"developer\",\"i"+
			"ss\":\"https://developer.com\"},\"time\":1590130872925},{\"_id\":\"BeDwk5rd8H-a3FWFYV91E"+
			"WTM6uZDmZ9P7JPk1JoOsh0\",\"access_token\":\"I20dmjAIhDd5IqFQfld0GFPtDeGhB97rV3JvANyx"+
			"zYKlH5JFn1\",\"token_type\":\"Bearer\",\"id_token\":\"eyJraWQiOiJhOWIwMmZkYS1iODY0LTQ5OD"+
			"gtOTc2Ni1kYjA3YmIyZDE1N2QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiaDBfbG5iaTR3NnlQX"+
			"zROWjN3X2hWdyIsInN1YiI6InVzZXItc3ViamVjdC0xMjM0NTMxIiwiYXVkIjoiY2xpZW50X051SEFZQ"+
			"XpsWFBUdXFMeTM2NjAxJ3teKF0iLCJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0LmVtb2JpeC5jby51a"+
			"zo4NDQzXC90ZXN0XC9hXC9vcGVuaWQtY2xpZW50XC8iLCJleHAiOjE1OTAxMzExNzIsImlhdCI6MTU5M"+
			"DEzMDg3Mn0.KpK4RsS1nl_QWwJp9ESATmrxN4HCaQgy2Tye1gu0ZAHBlmxI6cPUnu-6CUSzvB-jbPVsH"+
			"MbHoY2c5l4a9qVpdrU855QzhfFBPjzSH_RNmzODCF6q3BMRT6eNr7lDyXUzGdkfHkA8MkgKaMXz-Y4XC"+
			"-Fu77bLz_H8fw4gthdOU3tYmW0k_sKJhqTNUh77AVNqFJpeK8hgXYNXkSrrNtT0RVrRIW40fxgPhsQuX"+
			"1oqsuYUdtasAOrXnOsxZC3TpdA1OS1qQ20gCEsVaiZefuXjaw86hGfQcCoJz98iNqA1BaKf5I90bcYIF"+
			"6Q-O_ZoBMCgAv0BA6No3uXF9-2vCjRjyw\",\"scope\":\"openid\",\"msg\":\"Created token endpoin"+
			"t response\",\"result\":\"SUCCESS\",\"requirements\":[\"OIDCC-3.1.3.3\"],\"blockId\":\"44547"+
			"1\",\"testId\":\"BeDwk5rd8H\",\"src\":\"CreateTokenEndpointResponse\",\"testOwner\":{\"sub\":"+
			"\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130872927},{\"_id\":\"BeDwk5r"+
			"d8H-YzhyxWFuHg8EU8YrdCEsFOqWmQiIr6MI\",\"msg\":\"Response to HTTP request to test in"+
			"stance BeDwk5rd8H\",\"outgoing_status_code\":200,\"outgoing_headers\":{},\"http\":\"outg"+
			"oing\",\"outgoing_body\":{\"access_token\":\"I20dmjAIhDd5IqFQfld0GFPtDeGhB97rV3JvANyxz"+
			"YKlH5JFn1\",\"token_type\":\"Bearer\",\"id_token\":\"eyJraWQiOiJhOWIwMmZkYS1iODY0LTQ5ODg"+
			"tOTc2Ni1kYjA3YmIyZDE1N2QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiaDBfbG5iaTR3NnlQXz"+
			"ROWjN3X2hWdyIsInN1YiI6InVzZXItc3ViamVjdC0xMjM0NTMxIiwiYXVkIjoiY2xpZW50X051SEFZQX"+
			"psWFBUdXFMeTM2NjAxJ3teKF0iLCJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0LmVtb2JpeC5jby51az"+
			"o4NDQzXC90ZXN0XC9hXC9vcGVuaWQtY2xpZW50XC8iLCJleHAiOjE1OTAxMzExNzIsImlhdCI6MTU5MD"+
			"EzMDg3Mn0.KpK4RsS1nl_QWwJp9ESATmrxN4HCaQgy2Tye1gu0ZAHBlmxI6cPUnu-6CUSzvB-jbPVsHM"+
			"bHoY2c5l4a9qVpdrU855QzhfFBPjzSH_RNmzODCF6q3BMRT6eNr7lDyXUzGdkfHkA8MkgKaMXz-Y4XC-"+
			"Fu77bLz_H8fw4gthdOU3tYmW0k_sKJhqTNUh77AVNqFJpeK8hgXYNXkSrrNtT0RVrRIW40fxgPhsQuX1"+
			"oqsuYUdtasAOrXnOsxZC3TpdA1OS1qQ20gCEsVaiZefuXjaw86hGfQcCoJz98iNqA1BaKf5I90bcYIF6"+
			"Q-O_ZoBMCgAv0BA6No3uXF9-2vCjRjyw\",\"scope\":\"openid\"},\"outgoing_path\":\"token\",\"tes"+
			"tId\":\"BeDwk5rd8H\",\"src\":\"oidcc-client-test\",\"testOwner\":{\"sub\":\"developer\",\"iss\""+
			":\"https://developer.com\"},\"time\":1590130872931},{\"_id\":\"BeDwk5rd8H-TtKwn4Mz6mKED"+
			"IJeCiJXvtThH5RPGKVk\",\"msg\":\"Incoming HTTP request to test instance BeDwk5rd8H\",\""+
			"incoming_headers\":{\"user-agent\":\"openid-client/3.14.1 (https://github.com/panva/"+
			"node-openid-client)\",\"accept\":\"application/json\",\"accept-encoding\":\"gzip, deflat"+
			"e\",\"host\":\"localhost.emobix.co.uk:8443\",\"connection\":\"close\",\"x-ssl-cipher\":\"ECD"+
			"HE-RSA-AES128-GCM-SHA256\",\"x-ssl-protocol\":\"TLSv1.2\"},\"incoming_path\":\"jwks\",\"ht"+
			"tp\":\"incoming\",\"incoming_method\":\"GET\",\"incoming_query_string_params\":{},\"testId"+
			"\":\"BeDwk5rd8H\",\"src\":\"oidcc-client-test\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"h"+
			"ttps://developer.com\"},\"time\":1590130872944},{\"_id\":\"BeDwk5rd8H-fbt9CBJN97DN429c"+
			"1dEoMtDmHjIHKBKh\",\"msg\":\"Response to HTTP request to test instance BeDwk5rd8H\",\""+
			"outgoing_status_code\":200,\"outgoing_headers\":{},\"http\":\"outgoing\",\"outgoing_body"+
			"\":{\"keys\":[{\"kty\":\"RSA\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\"a9b02fda-b864-4988-9766-db"+
			"07bb2d157d\",\"n\":\"qENsnVVGCL_IQALmPpy2KzBYOGVYms5DodhhtHcverJoVoYjRqs_vvP64lVNPsi"+
			"XsgStwGQ3HrLiCpFUhdFHQ1-1dlRE6-0oHM55hTkDd0Y7TBld0PDnDTN-B7q91r_JLsNspdVQpTavoo5"+
			"qPZSV_bvxSlBTopMuMpxMBnmMKE20KiAcAVhJnlO7v97ILlIiYSCNwx0h5kmAHUZlhpw0DgE9yPVY5os"+
			"bmb1QjIbZRzbE6NRNFlJiXnTeraR89kHFfvyEeeIFTwFgkKPqFN0dZyWqd_zBO73yk1LruN5IDGwrpDb"+
			"K3hOSHqfExqJFi8qxISmuq4k3-bfllzKMz7HihQ\"},{\"kty\":\"RSA\",\"e\":\"AQAB\",\"use\":\"sig\",\"k"+
			"id\":\"a64634e9-dca9-4a17-8473-3a9916a5995f\",\"n\":\"iFgkzqI6TG8MxrTXCEmuDcn0dFq9vTbx"+
			"IylgOMlBUmKUOhiaIUIwfDeTWhzCn7VqZZMZoHX7AKgi3fitz8FLK_Rk-DI9ZuUMqX8jgvBmqUVn3_cE"+
			"znTd1pjpwXVWdBcQeXg--fMApLn0_Xhw_Nw89T5-ktlJ_PL1_-KqM58Zp_EURGOyspzoTrycumteAX91"+
			"MIISA7Yfa4vPq3soOWsj0aa77Y63MRbcaolsR1O5a_9JS8RFLW7wsgYkjNyMrejkMgliG6LFk1SUV0PK"+
			"NJZmiPt8WAaTrexsCvtfmeX7Xlar5_Jpd-_16z_ny4ys9Zhg3UWSyPKocngVqaw-1S3XlQ\"},{\"kty\":"+
			"\"EC\",\"use\":\"sig\",\"crv\":\"P-256\",\"kid\":\"1e5c5964-3a8f-4133-9c5e-345b6cdc7983\",\"x\":"+
			"\"Cl_uW3h3sqo72rfUiBwRKnZxdHQrr77yM8XGm7SAVN0\",\"y\":\"4C5VHcd1vibKU6wB5cAp4ZHBFeajJ"+
			"n83i3O1efKLSyM\"},{\"kty\":\"EC\",\"use\":\"sig\",\"crv\":\"P-256\",\"kid\":\"b8b5f398-74f0-48a0"+
			"-8c3c-b864f90b5229\",\"x\":\"1l_5-0TS9yqY2H5f6ykCvPlCgzIYVfYrqdUP8vlSBF0\",\"y\":\"ZILTN"+
			"Wr87F7rBcZQ8z3Lppf0BMcQJuFdxqURS2iX3Fw\"},{\"kty\":\"EC\",\"use\":\"sig\",\"crv\":\"secp256k"+
			"1\",\"kid\":\"30892e8f-5c88-4220-b606-1d94fbc48f1e\",\"x\":\"jTP7IkOQFXQF3cBYZXJGTLrMxoB"+
			"aOLIfxJtWUzHboXc\",\"y\":\"qB-tU8k1jHkMNPH3KerOoIeLw68Fo0th2N6imyxV1fw\"},{\"kty\":\"OKP"+
			"\",\"use\":\"sig\",\"crv\":\"Ed25519\",\"kid\":\"ef63a462-5106-4a0a-bca1-a845365248f2\",\"x\":\""+
			"sfLfwTAzPJnE6GkeHS5di9XSStnZfMmG6LZJvaSlxeE\"},{\"kty\":\"RSA\",\"e\":\"AQAB\",\"use\":\"enc"+
			"\",\"kid\":\"d5f9a9f3-b732-4600-9065-0164198fe7e8\",\"alg\":\"RSA-OAEP\",\"n\":\"nIIJfxzBWNh"+
			"aH4vWY8kaTEQOXbLtn6W7j2GShHJu9OXI9yMrT7bXwzEbaa-GgwoX5L0hTQDs0j3_HimFTcTJGyzZFPr"+
			"ZrnBiO9w1_lV8-PQ00ssOjkCIBCdsXUEY76MPVuKKrZWUn89p-oZUMKrvwEBVX-q8uLiYGASKoF55VKS"+
			"fpp9s20FtgW0St8QtPs8c6qA4XaOS3lnirHnfZpZu1KuvPkFscbwJ2HHByasmML9BD3L3dE-657P9rZ8"+
			"M6LzQUYY_NxvlhMyylA05rBySVRiHh_5GXZdXDXX5vRdmsgqgrQ1FhbYnk2mobSXN0SAVWtLvmQemeC9"+
			"Acc_6rItJPQ\"},{\"kty\":\"EC\",\"use\":\"enc\",\"crv\":\"P-256\",\"kid\":\"71da3fd0-2b61-4ad7-91"+
			"0c-516e513e71bd\",\"x\":\"jJDeVt2tHGl_iAa0E0YQ0aHjzaRqpxN94mHNRd2ylPU\",\"y\":\"UJSjhP_b"+
			"EhdEjZmu0mB7XDTAjz3skgyeRufWBbLIHUU\",\"alg\":\"ECDH-ES\"}]},\"outgoing_path\":\"jwks\",\""+
			"testId\":\"BeDwk5rd8H\",\"src\":\"oidcc-client-test\",\"testOwner\":{\"sub\":\"developer\",\"i"+
			"ss\":\"https://developer.com\"},\"time\":1590130872949},{\"_id\":\"BeDwk5rd8H-62X0mpsPTO"+
			"ZuXKzCpztWGOvB9adu43eZ\",\"msg\":\"Incoming HTTP request to test instance BeDwk5rd8H"+
			"\",\"incoming_headers\":{\"user-agent\":\"openid-client/3.14.1 (https://github.com/pan"+
			"va/node-openid-client)\",\"authorization\":\"Bearer I20dmjAIhDd5IqFQfld0GFPtDeGhB97r"+
			"V3JvANyxzYKlH5JFn1\",\"accept\":\"application/json\",\"accept-encoding\":\"gzip, deflate"+
			"\",\"host\":\"localhost.emobix.co.uk:8443\",\"connection\":\"close\",\"x-ssl-cipher\":\"ECDH"+
			"E-RSA-AES128-GCM-SHA256\",\"x-ssl-protocol\":\"TLSv1.2\"},\"incoming_path\":\"userinfo\","+
			"\"http\":\"incoming\",\"incoming_method\":\"GET\",\"incoming_query_string_params\":{},\"tes"+
			"tId\":\"BeDwk5rd8H\",\"src\":\"oidcc-client-test\",\"testOwner\":{\"sub\":\"developer\",\"iss\""+
			":\"https://developer.com\"},\"time\":1590130872976},{\"_id\":\"BeDwk5rd8H-mrFkadj0YRZr8"+
			"plXm9FpvA67CGpv65bn\",\"msg\":\"Userinfo endpoint\",\"blockId\":\"ac3e6b\",\"startBlock\":t"+
			"rue,\"testId\":\"BeDwk5rd8H\",\"src\":\"-START-BLOCK-\",\"testOwner\":{\"sub\":\"developer\",\""+
			"iss\":\"https://developer.com\"},\"time\":1590130872979},{\"_id\":\"BeDwk5rd8H-YqMOa8Ree"+
			"4kR9MmTTKB8Zc8PifKb95ba\",\"access_token\":\"I20dmjAIhDd5IqFQfld0GFPtDeGhB97rV3JvANy"+
			"xzYKlH5JFn1\",\"msg\":\"Found access token on incoming request\",\"result\":\"SUCCESS\",\""+
			"blockId\":\"ac3e6b\",\"requirements\":[\"RFC6750-2\",\"OIDCC-5.3.1\"],\"testId\":\"BeDwk5rd8"+
			"H\",\"src\":\"OIDCCExtractBearerAccessTokenFromRequest\",\"testOwner\":{\"sub\":\"develope"+
			"r\",\"iss\":\"https://developer.com\"},\"time\":1590130872980},{\"_id\":\"BeDwk5rd8H-doUhN"+
			"qJnLs9qzZW4WkNg45SwZm8yfdDx\",\"msg\":\"Found access token in request\",\"result\":\"SUC"+
			"CESS\",\"blockId\":\"ac3e6b\",\"actual\":\"I20dmjAIhDd5IqFQfld0GFPtDeGhB97rV3JvANyxzYKlH"+
			"5JFn1\",\"requirements\":[\"OIDCC-5.3.1\"],\"testId\":\"BeDwk5rd8H\",\"src\":\"RequireBearer"+
			"AccessToken\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time"+
			"\":1590130872981},{\"_id\":\"BeDwk5rd8H-JfKPri2gYPOcUXVi3ghgHwOgYWekhEP4\",\"sub\":\"use"+
			"r-subject-1234531\",\"msg\":\"User info endpoint output\",\"result\":\"SUCCESS\",\"require"+
			"ments\":[\"OIDCC-5.4\"],\"blockId\":\"ac3e6b\",\"testId\":\"BeDwk5rd8H\",\"src\":\"FilterUserI"+
			"nfoForScopes\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"tim"+
			"e\":1590130872983},{\"_id\":\"BeDwk5rd8H-hxlhsim4mVLeH2rVeqF1KOnAvPeRjuB4\",\"msg\":\"Co"+
			"ndition ran but did not log anything\",\"blockId\":\"ac3e6b\",\"testId\":\"BeDwk5rd8H\",\""+
			"src\":\"ClearAccessTokenFromRequest\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https:/"+
			"/developer.com\"},\"time\":1590130872984},{\"_id\":\"BeDwk5rd8H-6SKvxSrkzXxFfTg3ZkPqKg"+
			"YH9xmzyOlj\",\"msg\":\"Skipped evaluation due to missing required element: client us"+
			"erinfo_signed_response_alg\",\"result\":\"INFO\",\"blockId\":\"ac3e6b\",\"path\":\"userinfo_"+
			"signed_response_alg\",\"requirements\":[\"OIDCC-5.3.2\"],\"object\":\"client\",\"testId\":\""+
			"BeDwk5rd8H\",\"src\":\"AddIssAndAudToUserInfoResponse\",\"testOwner\":{\"sub\":\"developer"+
			"\",\"iss\":\"https://developer.com\"},\"time\":1590130872985},{\"_id\":\"BeDwk5rd8H-PBzbi1"+
			"jtv6UhDOhGg1mCWgGxwhZoY7we\",\"msg\":\"Skipped evaluation due to missing required el"+
			"ement: client userinfo_signed_response_alg\",\"result\":\"INFO\",\"blockId\":\"ac3e6b\",\""+
			"path\":\"userinfo_signed_response_alg\",\"requirements\":[\"OIDCC-5.3.2\"],\"object\":\"cl"+
			"ient\",\"testId\":\"BeDwk5rd8H\",\"src\":\"SignUserInfoResponse\",\"testOwner\":{\"sub\":\"dev"+
			"eloper\",\"iss\":\"https://developer.com\"},\"time\":1590130872987},{\"_id\":\"BeDwk5rd8H-"+
			"gcoiPlphzeFBj5GFETeyHlzeRWoCZQGZ\",\"msg\":\"Skipped evaluation due to missing requi"+
			"red element: client userinfo_encrypted_response_alg\",\"result\":\"INFO\",\"blockId\":\""+
			"ac3e6b\",\"path\":\"userinfo_encrypted_response_alg\",\"requirements\":[\"OIDCC-5.3.2\"],"+
			"\"object\":\"client\",\"testId\":\"BeDwk5rd8H\",\"src\":\"EncryptUserInfoResponse\",\"testOwn"+
			"er\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":1590130872989},{\"_i"+
			"d\":\"BeDwk5rd8H-9lJ8b8XC4ripypyKaWPmd1WuExs2t4ZG\",\"msg\":\"Response to HTTP request"+
			" to test instance BeDwk5rd8H\",\"outgoing_status_code\":200,\"outgoing_headers\":{},\""+
			"http\":\"outgoing\",\"outgoing_body\":{\"sub\":\"user-subject-1234531\"},\"outgoing_path\":"+
			"\"userinfo\",\"testId\":\"BeDwk5rd8H\",\"src\":\"oidcc-client-test\",\"testOwner\":{\"sub\":\"d"+
			"eveloper\",\"iss\":\"https://developer.com\"},\"time\":1590130872992},{\"_id\":\"BeDwk5rd8"+
			"H-WEtbEFduNvdqPeh74RmCxNNYeoXvbN5Z\",\"msg\":\"Test has run to completion\",\"result\":"+
			"\"FINISHED\",\"testmodule_result\":\"PASSED\",\"testId\":\"BeDwk5rd8H\",\"src\":\"oidcc-clien"+
			"t-test\",\"testOwner\":{\"sub\":\"developer\",\"iss\":\"https://developer.com\"},\"time\":159"+
			"0130872996},{\"_id\":\"BeDwk5rd8H-uDPNsozQLhk0ZcGFAQ8mYieBkmGfAemi\",\"msg\":\"Alias ha"+
			"s now been claimed by another test\",\"alias\":\"openid-client\",\"new_test_id\":\"1Hyt3"+
			"xZsTf\",\"testId\":\"BeDwk5rd8H\",\"src\":\"TEST-RUNNER\",\"testOwner\":{\"sub\":\"developer\","+
			"\"iss\":\"https://developer.com\"},\"time\":1590130874746}]}";

		JsonObject parsed = JsonParser.parseString(jsonExport).getAsJsonObject();
		JsonArray results = parsed.get("results").getAsJsonArray();
		int i=0;
		for(JsonElement result : results) {
			JsonObject singleResult = result.getAsJsonObject();
			System.out.println(singleResult.toString());
			i++;
			if(i>5)
			{
				break;
			}
		}
	}
}
