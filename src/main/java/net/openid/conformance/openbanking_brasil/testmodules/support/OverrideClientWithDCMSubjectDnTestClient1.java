package net.openid.conformance.openbanking_brasil.testmodules.support;

public class OverrideClientWithDCMSubjectDnTestClient1 extends AbstractOverrideClient {

	@Override
	String clientCert() {
		return "-----BEGIN CERTIFICATE-----\n" +
			"MIIHBzCCBe+gAwIBAgIUQDjg9s2Fe9VmMgQErOQOV7v8mxMwDQYJKoZIhvcNAQEL\n" +
			"BQAwcTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\n" +
			"FTATBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNB\n" +
			"TkRCT1ggSXNzdWluZyBDQSAtIEcxMB4XDTIyMDUyNDIyNDMwMFoXDTIzMDYyMzIy\n" +
			"NDMwMFowggEdMQswCQYDVQQGEwJCUjELMAkGA1UECBMCU1AxDzANBgNVBAcTBkxv\n" +
			"bmRvbjEcMBoGA1UEChMTT3BlbiBCYW5raW5nIEJyYXNpbDEtMCsGA1UECxMkNzRl\n" +
			"OTI5ZDktMzNiNi00ZDg1LThiYTctYzE0NmM4NjdhODE3MSUwIwYDVQQDExxodHRw\n" +
			"czovL3d3dy5kb21haW5vbmUuY29tLmJyMRcwFQYDVQQFEw40MzE0MjY2NjAwMDE5\n" +
			"NzEYMBYGA1UEDxMPQnVzaW5lc3MgRW50aXR5MRMwEQYLKwYBBAGCNzwCAQMTAlVL\n" +
			"MTQwMgYKCZImiZPyLGQBARMkOTdlOTAxNjktOTU0My00ZWM1LWI0NTAtNTI0YTJk\n" +
			"NDc1NmI0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqI/PkbVmbVSd\n" +
			"39avt7Rd9khA9CS3Tys/ciYYO05PgpOvn78g5Obmrb98DNCejqqYhFes2jjQnh8z\n" +
			"IIE+FNaMRGrndXE7qYNw2wWkq8zwv3a+rBGSYMHSfY5+oEqJ0T+K/plLgvAdm/o8\n" +
			"raGItqp7C+tRONl2UqGe+/U1hpP3otOrXssgSBSfK/c/IuU+oOE5NYy1Y2m7k3V1\n" +
			"zsWB2vIxTXdox0k/96jhueaKo6GptejHvO2nu4a1F/aaif3fYmXgMsg+1QlnD1HV\n" +
			"rQvRNBonPGfcQIlEulOx/mdJV3jdIahSbv8WXjUHPZHNw2Bu69bUJIGcvFQVbpDi\n" +
			"nagTcmBiQQIDAQABo4IC5zCCAuMwDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQUuuoo\n" +
			"6Jp0aAz8XITwgVt9Xjuo708wHwYDVR0jBBgwFoAUhn9YrRf1grZOtAWz+7DOEUPf\n" +
			"TL4wTAYIKwYBBQUHAQEEQDA+MDwGCCsGAQUFBzABhjBodHRwOi8vb2NzcC5zYW5k\n" +
			"Ym94LnBraS5vcGVuYmFua2luZ2JyYXNpbC5vcmcuYnIwSwYDVR0fBEQwQjBAoD6g\n" +
			"PIY6aHR0cDovL2NybC5zYW5kYm94LnBraS5vcGVuYmFua2luZ2JyYXNpbC5vcmcu\n" +
			"YnIvaXNzdWVyLmNybDAnBgNVHREEIDAeghxodHRwczovL3d3dy5kb21haW5vbmUu\n" +
			"Y29tLmJyMA4GA1UdDwEB/wQEAwIFoDATBgNVHSUEDDAKBggrBgEFBQcDAjCCAagG\n" +
			"A1UdIASCAZ8wggGbMIIBlwYKKwYBBAGDui9kATCCAYcwggE2BggrBgEFBQcCAjCC\n" +
			"ASgMggEkVGhpcyBDZXJ0aWZpY2F0ZSBpcyBzb2xlbHkgZm9yIHVzZSB3aXRoIFJh\n" +
			"aWRpYW0gU2VydmljZXMgTGltaXRlZCBhbmQgb3RoZXIgcGFydGljaXBhdGluZyBv\n" +
			"cmdhbmlzYXRpb25zIHVzaW5nIFJhaWRpYW0gU2VydmljZXMgTGltaXRlZHMgVHJ1\n" +
			"c3QgRnJhbWV3b3JrIFNlcnZpY2VzLiBJdHMgcmVjZWlwdCwgcG9zc2Vzc2lvbiBv\n" +
			"ciB1c2UgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgUmFpZGlhbSBTZXJ2\n" +
			"aWNlcyBMdGQgQ2VydGljaWNhdGUgUG9saWN5IGFuZCByZWxhdGVkIGRvY3VtZW50\n" +
			"cyB0aGVyZWluLjBLBggrBgEFBQcCARY/aHR0cDovL3JlcG9zaXRvcnkuc2FuZGJv\n" +
			"eC5wa2kub3BlbmJhbmtpbmdicmFzaWwub3JnLmJyL3BvbGljaWVzMA0GCSqGSIb3\n" +
			"DQEBCwUAA4IBAQAFddZb4KiB0YYi9/4nnadBemJJrnmGovypgjyxB2FODmzixBwg\n" +
			"yNbAwz5gI9d+IdhrmIoJG3Kr54Z+hxO3m183mLyW6xrXrRcbbdgHtmHdXjHqIgKG\n" +
			"2RCOS82EeHECkw7hsIDxKcysLc+s8wXP3UYwRI/mbIqWjlB7ezqbBivhIEimz0Gm\n" +
			"t0EJpFgFsAHv81tmGjFhNR6ziildRBk4SgWw8x8DSMpAMagRNbrx1W5hnxchJo0o\n" +
			"Hc45rEQ1m5CWQcmGrvlcfOdVPihJ0rscXT/efn4arsLsiCcADlyOpXX/rODhLd09\n" +
			"/Cg1fIM188ao8+GQRt+pgbMR9usgSZJo0K2c\n" +
			"-----END CERTIFICATE-----";
	}
	@Override
	String clientKey() {
		return "-----BEGIN PRIVATE KEY-----\n" +
			"MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCoj8+RtWZtVJ3f\n" +
			"1q+3tF32SED0JLdPKz9yJhg7Tk+Ck6+fvyDk5uatv3wM0J6OqpiEV6zaONCeHzMg\n" +
			"gT4U1oxEaud1cTupg3DbBaSrzPC/dr6sEZJgwdJ9jn6gSonRP4r+mUuC8B2b+jyt\n" +
			"oYi2qnsL61E42XZSoZ779TWGk/ei06teyyBIFJ8r9z8i5T6g4Tk1jLVjabuTdXXO\n" +
			"xYHa8jFNd2jHST/3qOG55oqjoam16Me87ae7hrUX9pqJ/d9iZeAyyD7VCWcPUdWt\n" +
			"C9E0Gic8Z9xAiUS6U7H+Z0lXeN0hqFJu/xZeNQc9kc3DYG7r1tQkgZy8VBVukOKd\n" +
			"qBNyYGJBAgMBAAECggEAGw32BA4zB0ebt/0hIqGjATAIqNxUcF23Jw72jbJHMGSc\n" +
			"PXjJAqRi5a6Zw67KUYi6cRISGXZ2rs0VgNlC9HJD0jQRvA0KxULmXr5m90zYs/UP\n" +
			"ASwoK+UqxTc/8LdLVoKrQFZXbd00oL/cdFvnHGEm3btibnb3jFIKOVOFSKPq4mnG\n" +
			"u0yei+iCveYnRTCVNeNVzn1h8E+lU5m8qTNevLyF90TEeqwt2qWPp/0+yfc5n2yL\n" +
			"wyyq4O0GatzDKsJuC8NeI1qPhmqCBQP/bQgcWx2Ie7wwwc65ZN05C+xj+z75B68p\n" +
			"wQ24DSs1sCyOZpUzWKvGm0m0uBu7ScsafrvX9+pJRwKBgQDqseeCf9Hfmzf3XG9M\n" +
			"dlZ9a1d8au+HgdVOsakA+lTiPonFLDze+lzxM0JtRqeRJzp5Pnj9i6JugrzTzYLv\n" +
			"Rwdy7+6Uiui0Nu5Y2rJ1GCa6JFMIWdmfPRwpSMV3vHUtade63LJ6DZP12mIhrx62\n" +
			"vcna9VXoo5vtWKaLdtpjGDjN7wKBgQC33QgsugrUZ4bCz+1tpW+iH1GmdqXhpg2d\n" +
			"l+BEHNjWRO1ULq259WvpURr/7A46lNFhV4ae0LLd39oFPv6n92mf1jY6GWdVy4rL\n" +
			"MwD2GyaEDtIit67iDe1/gaYvgfbduEXD+DhwxzwVck1Fy0+x4TDFVg3ah5jkzv/H\n" +
			"VIh7GIoCzwKBgF6WdtS8iSxSlXrHMUAizXbcxTSqsIDvjbWamp9/RKiuRb5Gtv2y\n" +
			"77RyUUpTWIOCyOlGiWK+XSem302JnUSsXs8u7fvGBEVlgigjdBsHMcyBiUlrVO79\n" +
			"pRTqdFNui9dSuhRgkDnqsQA8FKK4vmsuEGWDzSzmi5hbyzuYGion//TXAoGAPE3T\n" +
			"m4/dc3MNISI/dmH3bk4lYpqxp4PmHVUt6kkn2Yc77AtvUXmsThca6uuKcy5SSkIB\n" +
			"1l8O+3SrNmNF9ONSEmmY2Y9xdBNRT3pIC3A2PsP5qgdi5aO5zMNCNXzD8k65GyBz\n" +
			"qpG+JC6cJ5MabXY1n9OssYsd25YFEGSqau1OfsMCgYBZFomjpwoUC3rEnVFsyaEb\n" +
			"09NZ2oDSMxnUH9fnaZg9yotU6fVkKQNjYC8D2ZI1uA4IDUVBcNVFYNu5gUAX98CO\n" +
			"R4OrHhYU5aXYgJUySDiW0vXO6N0OYBSu7QIyxY0Rp55D0YE0XfwFudoNmYPz6b21\n" +
			"Nj85e29wptN9UN6+8QjecQ==\n" +
			"-----END PRIVATE KEY-----\n";
	}

	@Override
	String clientJwks() {
		// there's no separate key for this client, it uses the org jwks for auth
		return orgJwks;
	}

	@Override
	String role() {
		return "DCM client 1";
	}

	@Override
	String directoryClientId() {
		// can be extracted from cert
		return "97e90169-9543-4ec5-b450-524a2d4756b4";
	}
}
