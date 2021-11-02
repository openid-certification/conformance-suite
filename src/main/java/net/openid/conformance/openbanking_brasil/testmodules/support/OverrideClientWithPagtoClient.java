package net.openid.conformance.openbanking_brasil.testmodules.support;

public class OverrideClientWithPagtoClient extends AbstractOverrideClient {
	// software id is 368bceef-6096-4e1a-a702-64c07d12c98a

	@Override
	String clientCert() {
		return "-----BEGIN CERTIFICATE-----\n" +
			"MIIHDDCCBfSgAwIBAgIUYvUFpecbRLx1Dt2CwJQpQW7x7AowDQYJKoZIhvcNAQEL\n" +
			"BQAwcTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\n" +
			"FTATBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNB\n" +
			"TkRCT1ggSXNzdWluZyBDQSAtIEcxMB4XDTIxMTEwMjE4NTMwMFoXDTIyMTIwMjE4\n" +
			"NTMwMFowggEnMQswCQYDVQQGEwJCUjELMAkGA1UECBMCU1AxEjAQBgNVBAcTCVNh\n" +
			"byBQYXVsbzEcMBoGA1UEChMTT3BlbiBCYW5raW5nIEJyYXNpbDEtMCsGA1UECxMk\n" +
			"NzRlOTI5ZDktMzNiNi00ZDg1LThiYTctYzE0NmM4NjdhODE3MScwJQYDVQQDEx50\n" +
			"cHAuc2FuZGJveC5vcGVuYmFua2luZy5vcmcuYnIxFzAVBgNVBAUTDjQzMTQyNjY2\n" +
			"MDAwMTk3MTQwMgYKCZImiZPyLGQBARMkMzY4YmNlZWYtNjA5Ni00ZTFhLWE3MDIt\n" +
			"NjRjMDdkMTJjOThhMR0wGwYDVQQPExRQcml2YXRlIE9yZ2FuaXphdGlvbjETMBEG\n" +
			"CysGAQQBgjc8AgEDEwJCUjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n" +
			"ALeG7Q28qLiQMnIhaOMVIQzkhPxy9ARpHs6Ro8wrZiagAu3KN7rYaOBiRxRKCa9B\n" +
			"fCnjmM7e3bW956/+nfp0wque2FHeiw+xhYyRzkP0kqlwiS+ctj6XrO/fv6zBMdMZ\n" +
			"Gg5qYo2WaKnovouVlmSsyXGKkf5PSBl0po2fc0/G4lKF5znKRItDlnEFJenUSqmo\n" +
			"/fFOzP18WwefUHlF21eZWO9Zj7vUtbSoVV9wBB2yw3V8V2iU53oy/N5LKgxdauB2\n" +
			"Q+71oZIterf83Cu4yLZ0qVZFRwAt7ITJjnkXh7kUXXztHSUxb3lzhp9GRXWMn0eB\n" +
			"jZbyyn8SbkqUwfSGYYl9W0sCAwEAAaOCAuIwggLeMAwGA1UdEwEB/wQCMAAwHwYD\n" +
			"VR0jBBgwFoAUhn9YrRf1grZOtAWz+7DOEUPfTL4wTAYIKwYBBQUHAQEEQDA+MDwG\n" +
			"CCsGAQUFBzABhjBodHRwOi8vb2NzcC5zYW5kYm94LnBraS5vcGVuYmFua2luZ2Jy\n" +
			"YXNpbC5vcmcuYnIwSwYDVR0fBEQwQjBAoD6gPIY6aHR0cDovL2NybC5zYW5kYm94\n" +
			"LnBraS5vcGVuYmFua2luZ2JyYXNpbC5vcmcuYnIvaXNzdWVyLmNybDAOBgNVHQ8B\n" +
			"Af8EBAMCBaAwEwYDVR0lBAwwCgYIKwYBBQUHAwIwHQYDVR0OBBYEFJtxiQ1FP67l\n" +
			"hmQARlLzBHqW9yTzMCkGA1UdEQQiMCCCHnRwcC5zYW5kYm94Lm9wZW5iYW5raW5n\n" +
			"Lm9yZy5icjCCAaEGA1UdIASCAZgwggGUMIIBkAYKKwYBBAGDui9kATCCAYAwggE2\n" +
			"BggrBgEFBQcCAjCCASgMggEkVGhpcyBDZXJ0aWZpY2F0ZSBpcyBzb2xlbHkgZm9y\n" +
			"IHVzZSB3aXRoIFJhaWRpYW0gU2VydmljZXMgTGltaXRlZCBhbmQgb3RoZXIgcGFy\n" +
			"dGljaXBhdGluZyBvcmdhbmlzYXRpb25zIHVzaW5nIFJhaWRpYW0gU2VydmljZXMg\n" +
			"TGltaXRlZHMgVHJ1c3QgRnJhbWV3b3JrIFNlcnZpY2VzLiBJdHMgcmVjZWlwdCwg\n" +
			"cG9zc2Vzc2lvbiBvciB1c2UgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUg\n" +
			"UmFpZGlhbSBTZXJ2aWNlcyBMdGQgQ2VydGljaWNhdGUgUG9saWN5IGFuZCByZWxh\n" +
			"dGVkIGRvY3VtZW50cyB0aGVyZWluLjBEBggrBgEFBQcCARY4aHR0cDovL2Nwcy5z\n" +
			"YW5kYm94LnBraS5vcGVuYmFua2luZ2JyYXNpbC5vcmcuYnIvcG9saWNpZXMwDQYJ\n" +
			"KoZIhvcNAQELBQADggEBANFOZt51FHjK64n5MbZkXqXef/9UCJx6QR+Hnu35tDmX\n" +
			"7qhq5FE8c1JC4LUNbT5i2rmQSJctlab2nyTb89UsrLj+MIF7Do0jV3dSmfE9ACbk\n" +
			"7QluLscNywUNDElBCMyR5J6OB4MpvfPMXuakDGjSuOUJzD/SLBj7dndaAEEdcVnU\n" +
			"9koP/ogddAjT5zPmAEUh7/dD2Z8q7J6emW1wqlM6qkXiPQaPpvwhid07LK2/kM/d\n" +
			"M4jV1Zm6m/5lacLiQmUJSf2rP94Ub29OXzJHjf+vvbhjdtReplWqNx4jzlZRbO1q\n" +
			"qcJMtpaq+5UaVsptwcG5nwrp4goqNeDx12vO32+hbmo=\n" +
			"-----END CERTIFICATE-----";
	}
	@Override
	String clientKey() {
		return "-----BEGIN PRIVATE KEY-----\n" +
			"MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC3hu0NvKi4kDJy\n" +
			"IWjjFSEM5IT8cvQEaR7OkaPMK2YmoALtyje62GjgYkcUSgmvQXwp45jO3t21veev\n" +
			"/p36dMKrnthR3osPsYWMkc5D9JKpcIkvnLY+l6zv37+swTHTGRoOamKNlmip6L6L\n" +
			"lZZkrMlxipH+T0gZdKaNn3NPxuJShec5ykSLQ5ZxBSXp1EqpqP3xTsz9fFsHn1B5\n" +
			"RdtXmVjvWY+71LW0qFVfcAQdssN1fFdolOd6MvzeSyoMXWrgdkPu9aGSLXq3/Nwr\n" +
			"uMi2dKlWRUcALeyEyY55F4e5FF187R0lMW95c4afRkV1jJ9HgY2W8sp/Em5KlMH0\n" +
			"hmGJfVtLAgMBAAECggEBAKeNxrUI7dz6wSOTvwe7TZ5Qr90vMfso4dhj/PA1GyDe\n" +
			"a24fdbvfQfjBoPU0Fx1rIum3gdY0gKQhifoS8rYrjWNUNVihXcJpJ21L3M4L5y5M\n" +
			"sOLIJ1aD8X5af8s/4rZAr5pDpFBx5oJhHD2hx1mIqq0/fsR4K3IaHskjrwpBoHUH\n" +
			"gEoOFZQbfTo3BBI+IZaRnfXg3mgitZ/28QH6uwtQvxTyGr2vTjElqzTNHuOZKkrh\n" +
			"jjoFWR0dBNJky/FtXddeDzJCkTID3Rkr7wzWlntEw4dsR2jFct59VQ+i0x/KKyI8\n" +
			"QqqGWrqZL4oxvv9C/TaJ1VQO4HA/bIe8IRJVGxWzU7kCgYEA6aDtpYw4nMp68Jyo\n" +
			"/ymE3D+aB7JZOMwJL+x2ocHgeESGeZxq7MoNSGei7+zWR9aemNwqjL0ZSbi2A8ty\n" +
			"pcFtcZ7EdlpB5VUG9eZI1pl3MtOAs6xcoHqEVm9dqGyAWw77ilj0rJqZyxgq+4y2\n" +
			"7l5KlMnxuo9GEqXbGP8KsBr5pqUCgYEAyRnUTQJmuTmbNt3FcbSKO7f3Z2vqN4QK\n" +
			"q9+l6THK7EWoJvoXTHfE8NAck3z9XlVh3tyzUQDArhZ30X+SOl9YnVN1J0eXqKcg\n" +
			"bXZdRc6cHQYLP+3geGwLJLhph7sur0Gq0a055Ls6Ox4Wdi8mHiDAHRZ9+R9w2DAV\n" +
			"jCmBb/uwRy8CgYBRJXdFl+WV9/IpnpBISDiAc4rhf33j72KFhcFSv89ilpKKrNyq\n" +
			"sCda7bw3T3DB9PaTK22QAqJE6+Y0fTauAfgsjbO5U/ItYJIRWNIVz6oKOlIqlgR6\n" +
			"LPPIPdgvBriyaArQa8NYLD6cvyE2zBJByvhmo5nVbawuD/OAxB0HO+dYjQKBgFvM\n" +
			"87I332a8mlNAh6pxTQ0X3NOUWrX/C9QL4zQBq+2RmsI4NjyNDBUWG4VkcFEIzwWq\n" +
			"YB0hJ9QQo5+6Caml7tDa0UceEmhF6rKtbsS4HWHOaYJEd8zhHXEk0d/JHfWZF35i\n" +
			"AmSc52cnQ/+tZEwqjSh6JQV9SckWoi96nwDct0q3AoGASkv35OF4cVFvIM570m8v\n" +
			"fMfALQWfafoJn/gNV9JIru1d4QsS40eaKMtbQkkgbIZoRkRd9bYrrqcLlOzg34CG\n" +
			"Twh9NPEmbJUe7wxwcQuh9wrJ5cGCkNfOuwwK/dTkk2cPGdveCKzaqA9Wgo1lV3pK\n" +
			"oWvBNoS85NEcmu9OX90rq3Q=\n" +
			"-----END PRIVATE KEY-----\n";
	}

	@Override
	String role() {
		return "PAGTO";
	}

	@Override
	String directoryClientId() {
		return "019NDXYT8Nn0T0ger0Rey";
	}
}
