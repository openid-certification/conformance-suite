package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import org.springframework.beans.factory.annotation.Value;

public class OverrideClientWithDadosClient extends AbstractOverrideClient {

	@Override
	String clientCert() {
		return "-----BEGIN CERTIFICATE-----\n" +
			"MIIHDDCCBfSgAwIBAgIUEr/5GcuxjfrjkVdBtUwuldkGq7IwDQYJKoZIhvcNAQEL\n" +
			"BQAwcTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\n" +
			"FTATBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNB\n" +
			"TkRCT1ggSXNzdWluZyBDQSAtIEcxMB4XDTIxMTEwMjE4NDkwMFoXDTIyMTIwMjE4\n" +
			"NDkwMFowggEnMQswCQYDVQQGEwJCUjELMAkGA1UECBMCU1AxEjAQBgNVBAcTCVNh\n" +
			"byBQYXVsbzEcMBoGA1UEChMTT3BlbiBCYW5raW5nIEJyYXNpbDEtMCsGA1UECxMk\n" +
			"NzRlOTI5ZDktMzNiNi00ZDg1LThiYTctYzE0NmM4NjdhODE3MScwJQYDVQQDEx50\n" +
			"cHAuc2FuZGJveC5vcGVuYmFua2luZy5vcmcuYnIxFzAVBgNVBAUTDjQzMTQyNjY2\n" +
			"MDAwMTk3MTQwMgYKCZImiZPyLGQBARMkYmY0ZmU1ODUtYTU2Zi00YzY4LTgyZmUt\n" +
			"ZWIxMjY1MjYxNzI3MR0wGwYDVQQPExRQcml2YXRlIE9yZ2FuaXphdGlvbjETMBEG\n" +
			"CysGAQQBgjc8AgEDEwJCUjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n" +
			"ALnljhq109eofpJGo4MOf2jROvKHclEojPJbVAIyRBz3BK0WW4ZL4WJnIebOJJFs\n" +
			"YLvq011b4D5sL/WsNhdEevLaXQe6nH61yoriTEvctC1lJrWozr22A8drxuqtCyYr\n" +
			"jS5FaD0Zp1rUjVwDSXQmpr5mxWpd19Vji6d07nDt6ENFowtBoZcCO8BLL9VwGDVT\n" +
			"AQpqgEy/WsnsHogrhgyl8+6OtaB9DjiVl440x8+S/fAFKe/P1TY+3ckpvwzvMyrK\n" +
			"K/HjCf0MLT8OJZ6fn+ma4ITX/qmDS2fwjJOgLqGnQ390xeI9DPZzjcwn1ynm2EPP\n" +
			"Tjo1IHvAKUqLsQ99f3eXWrcCAwEAAaOCAuIwggLeMAwGA1UdEwEB/wQCMAAwHwYD\n" +
			"VR0jBBgwFoAUhn9YrRf1grZOtAWz+7DOEUPfTL4wTAYIKwYBBQUHAQEEQDA+MDwG\n" +
			"CCsGAQUFBzABhjBodHRwOi8vb2NzcC5zYW5kYm94LnBraS5vcGVuYmFua2luZ2Jy\n" +
			"YXNpbC5vcmcuYnIwSwYDVR0fBEQwQjBAoD6gPIY6aHR0cDovL2NybC5zYW5kYm94\n" +
			"LnBraS5vcGVuYmFua2luZ2JyYXNpbC5vcmcuYnIvaXNzdWVyLmNybDAOBgNVHQ8B\n" +
			"Af8EBAMCBaAwEwYDVR0lBAwwCgYIKwYBBQUHAwIwHQYDVR0OBBYEFK5JEcJO4uCH\n" +
			"03RV5JF3t1QOi1DuMCkGA1UdEQQiMCCCHnRwcC5zYW5kYm94Lm9wZW5iYW5raW5n\n" +
			"Lm9yZy5icjCCAaEGA1UdIASCAZgwggGUMIIBkAYKKwYBBAGDui9kATCCAYAwggE2\n" +
			"BggrBgEFBQcCAjCCASgMggEkVGhpcyBDZXJ0aWZpY2F0ZSBpcyBzb2xlbHkgZm9y\n" +
			"IHVzZSB3aXRoIFJhaWRpYW0gU2VydmljZXMgTGltaXRlZCBhbmQgb3RoZXIgcGFy\n" +
			"dGljaXBhdGluZyBvcmdhbmlzYXRpb25zIHVzaW5nIFJhaWRpYW0gU2VydmljZXMg\n" +
			"TGltaXRlZHMgVHJ1c3QgRnJhbWV3b3JrIFNlcnZpY2VzLiBJdHMgcmVjZWlwdCwg\n" +
			"cG9zc2Vzc2lvbiBvciB1c2UgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUg\n" +
			"UmFpZGlhbSBTZXJ2aWNlcyBMdGQgQ2VydGljaWNhdGUgUG9saWN5IGFuZCByZWxh\n" +
			"dGVkIGRvY3VtZW50cyB0aGVyZWluLjBEBggrBgEFBQcCARY4aHR0cDovL2Nwcy5z\n" +
			"YW5kYm94LnBraS5vcGVuYmFua2luZ2JyYXNpbC5vcmcuYnIvcG9saWNpZXMwDQYJ\n" +
			"KoZIhvcNAQELBQADggEBAFLy47nhnbOp+xdSme4NFkZdwUifl2SFFK3u88zOeDbD\n" +
			"KbBAS6ML3vhcSX0MqKp/JGEJbLZg6K9VlRHuwkm1FYsdEV6KVUMQrxwpLqI4jby3\n" +
			"YY7FkXfG7Pm5CSzizzakBuHzoynW7w/Y65aIpFBDZXDTU0FfWvn+/ED5pF/nFTXt\n" +
			"RrktVMWjUZ6ReY3Cwywd7KMpESlDYwWJJGee3jVFnkdIvqyTonRryEDR9fnGulDi\n" +
			"RoP35gGr5wv3jSnuDsxYb60P/y8UD+QhzjZuJSXmahjkEi1fhhqmar0AKNWbygmJ\n" +
			"a9MaQFvtRwhzJozAvknkzbplS5RObu00y4vp4odHUjA=\n" +
			"-----END CERTIFICATE-----\n";
	}
	@Override
	String clientKey() {
		return "-----BEGIN PRIVATE KEY-----\n" +
			"MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC55Y4atdPXqH6S\n" +
			"RqODDn9o0Tryh3JRKIzyW1QCMkQc9wStFluGS+FiZyHmziSRbGC76tNdW+A+bC/1\n" +
			"rDYXRHry2l0Hupx+tcqK4kxL3LQtZSa1qM69tgPHa8bqrQsmK40uRWg9Gada1I1c\n" +
			"A0l0Jqa+ZsVqXdfVY4undO5w7ehDRaMLQaGXAjvASy/VcBg1UwEKaoBMv1rJ7B6I\n" +
			"K4YMpfPujrWgfQ44lZeONMfPkv3wBSnvz9U2Pt3JKb8M7zMqyivx4wn9DC0/DiWe\n" +
			"n5/pmuCE1/6pg0tn8IyToC6hp0N/dMXiPQz2c43MJ9cp5thDz046NSB7wClKi7EP\n" +
			"fX93l1q3AgMBAAECggEAfzkvi/Asz0cteIaHmUXuxJmNMjaocgrCi4sdb3NfUkNs\n" +
			"G9lVQg/kbCgQ95jVRgFaIsGPbjFWwuHf4M6/JiVjz5jKWmPIfp+DwpEljjW5yeeU\n" +
			"GncxJnzPEUgdlrZfDvx3Xtd5g3dvJaaaKALH3oolS+Gh8i3PbOb3yN8p4HexoXd4\n" +
			"WpRIY6JfFYvfuSSVCWRnxbHV0Kpv3CKovV/QEvzi82a6PGGf4AhvChnM2nJbqE1Y\n" +
			"H+LewRzpMX6zN5I+3rSVdPpTW9PWSxs6HwfgUhKwTHKLtq9bfcMfS710KT3Qt/NX\n" +
			"N2ad01YFRkhMSn05LFXk+wWh2TrGJsAp4Ud8uew1IQKBgQDh5WjFUwoUi3dVX/HR\n" +
			"txHhm8PoYn1exCmsHGZNov9ZsfdN/x7d18+4l/bEGiREc00HTmO5cjD29VM1/4MI\n" +
			"xwQXVEj52OdnN//vcyA4LdtMVdenauHDfW4n2NPEY+6ykfkKXPZ/RiT/07sMyfMH\n" +
			"x4V28O3WXwvTLSBiq2LIsEBXawKBgQDSq4orSGfTPFhDiugbFLM5Q7hQE1n5+dPc\n" +
			"mynlLdQ62T7nZU0iDcJKtULt1eb2J+XRJBZ6fYJwXk517iLdg3c+0hFfwx41g/+6\n" +
			"3x4mfsAi1Zpmq8/6loX4yvssN7phoqTmoc2uWSPtIHX/2eHZPlqutgGDmy7CAPtX\n" +
			"VRjI5e145QKBgDA2CPVMMrONzGsxLegHPU68MsSO+JjYHti3uvHI3tyiydggodLQ\n" +
			"82k/LZBOz/y36vGrPkde4qpiU18L6EhgSTQ9bdG9BC4YWowdMvwqdUpGa980RXi2\n" +
			"dWMVuKSKe3ArDU6z5nvlWZIr+xjQpQi9AXQQGYKOCHUKhYXpdAzG6QR/AoGAUB5k\n" +
			"4akbsz4T4zwEoafxsSmMAwgZA/R7Gj74Y+xV9juArMfd2cGZzoe4+HKM77iksjEj\n" +
			"S1pILSvwcvEp2UySIRYK/XxbFMcqjoskEEfMEnNh0QIuioKMWN35QAvBmjgctol6\n" +
			"i3/jJd9egPr/5XrNSfx1/vMZiTaOX9xBHMY01fECgYAZOCdigkoIJ01zjeUw79Nr\n" +
			"7R1PFU0r4P6qUX4w7/9qmf4sfZDZoZVW6P3uG44FMS710s+XATCswBbZcbPD1Eq3\n" +
			"1HVIw4c2ICpi3pdIWJeZD5Zo5BOYRVSge6QV+o8A3/cbBGvlIW8HrZXPbbCKSRiD\n" +
			"v5I9apDUcrcpKiesTwoRfA==\n" +
			"-----END PRIVATE KEY-----\n";
	}

	@Override
	String clientJwks() {
		// there's no separate key for this client, it uses the org jwks for auth
		return orgJwks;
	}

	@Override
	String role() {
		return "DADOS";
	}

	@Override
	String directoryClientId() {
		return "rK3VIPBPDKToTv-jR1tvz";
	}
}
