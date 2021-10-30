package net.openid.conformance.openbanking_brasil.testmodules.support;

public class OverrideClientWithPagtoClient extends AbstractOverrideClient {

	@Override
	String clientCert() {
		return "-----BEGIN CERTIFICATE-----\n" +
			"MIIG+zCCBeOgAwIBAgIUL2wvQ+efTa6gQJC5DI4g6cLOYfAwDQYJKoZIhvcNAQEL\n" +
			"BQAwcTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\n" +
			"FTATBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNB\n" +
			"TkRCT1ggSXNzdWluZyBDQSAtIEcxMB4XDTIxMTAyOTAxMTkwMFoXDTIyMTEyODAx\n" +
			"MTkwMFowggEeMQswCQYDVQQGEwJCUjELMAkGA1UECBMCUkoxETAPBgNVBAcTCEJP\n" +
			"VEFGT0dPMRwwGgYDVQQKExNPcGVuIEJhbmtpbmcgQnJhc2lsMS0wKwYDVQQLEyQ3\n" +
			"NGU5MjlkOS0zM2I2LTRkODUtOGJhNy1jMTQ2Yzg2N2E4MTcxHzAdBgNVBAMTFm1v\n" +
			"Y2stcGFndG8ucmFpZGlhbS5jb20xFjAUBgNVBAUTDTEzMzUzMjM2MDAxODkxHjAc\n" +
			"BgNVBA8TFU5vbi1Db21tZXJjaWFsIEVudGl0eTETMBEGCysGAQQBgjc8AgEDEwJC\n" +
			"UjE0MDIGCgmSJomT8ixkAQETJDM2OGJjZWVmLTYwOTYtNGUxYS1hNzAyLTY0YzA3\n" +
			"ZDEyYzk4YTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANjjR7vOEU7E\n" +
			"BkMU15SzsoABYqMQdkWwZfGWt2DFvYBvYC3I9jR9Fl4W5sVk9WX7YGr5uIviATYK\n" +
			"finLLlsmIuhi2wSdGhi3a3aPACtYtrriOrRqKPfBmbes1b/GxxEcdXz+4gdMPXHy\n" +
			"46Zs2NeqfjEI9mhOWV3O91lLlF/tGGjyCPqP2r45G+2nLOqIQfAI16TqI02z+Hmv\n" +
			"/tQpBPfOd6LX9hEzCSNtDxYtLZ6MnxZsKDvCS7x2jSOmiBBzhR+U787MDhupnIZq\n" +
			"qFM8w9dHFdM4NO9lUEYQpvcTdhxbmsbiXJHXl/gwRt+psfbJor1ESl0PoM5dWI7c\n" +
			"JyTu5gvcf2sCAwEAAaOCAtowggLWMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFCkB\n" +
			"Jgw37O5X3tWIdd3z6l6GKDA4MB8GA1UdIwQYMBaAFIZ/WK0X9YK2TrQFs/uwzhFD\n" +
			"30y+MEwGCCsGAQUFBwEBBEAwPjA8BggrBgEFBQcwAYYwaHR0cDovL29jc3Auc2Fu\n" +
			"ZGJveC5wa2kub3BlbmJhbmtpbmdicmFzaWwub3JnLmJyMEsGA1UdHwREMEIwQKA+\n" +
			"oDyGOmh0dHA6Ly9jcmwuc2FuZGJveC5wa2kub3BlbmJhbmtpbmdicmFzaWwub3Jn\n" +
			"LmJyL2lzc3Vlci5jcmwwIQYDVR0RBBowGIIWbW9jay1wYWd0by5yYWlkaWFtLmNv\n" +
			"bTAOBgNVHQ8BAf8EBAMCBaAwEwYDVR0lBAwwCgYIKwYBBQUHAwIwggGhBgNVHSAE\n" +
			"ggGYMIIBlDCCAZAGCisGAQQBg7ovZAEwggGAMIIBNgYIKwYBBQUHAgIwggEoDIIB\n" +
			"JFRoaXMgQ2VydGlmaWNhdGUgaXMgc29sZWx5IGZvciB1c2Ugd2l0aCBSYWlkaWFt\n" +
			"IFNlcnZpY2VzIExpbWl0ZWQgYW5kIG90aGVyIHBhcnRpY2lwYXRpbmcgb3JnYW5p\n" +
			"c2F0aW9ucyB1c2luZyBSYWlkaWFtIFNlcnZpY2VzIExpbWl0ZWRzIFRydXN0IEZy\n" +
			"YW1ld29yayBTZXJ2aWNlcy4gSXRzIHJlY2VpcHQsIHBvc3Nlc3Npb24gb3IgdXNl\n" +
			"IGNvbnN0aXR1dGVzIGFjY2VwdGFuY2Ugb2YgdGhlIFJhaWRpYW0gU2VydmljZXMg\n" +
			"THRkIENlcnRpY2ljYXRlIFBvbGljeSBhbmQgcmVsYXRlZCBkb2N1bWVudHMgdGhl\n" +
			"cmVpbi4wRAYIKwYBBQUHAgEWOGh0dHA6Ly9jcHMuc2FuZGJveC5wa2kub3BlbmJh\n" +
			"bmtpbmdicmFzaWwub3JnLmJyL3BvbGljaWVzMA0GCSqGSIb3DQEBCwUAA4IBAQAB\n" +
			"LM2dOfVev4khfkpThUwHD6pRzUneEX8OZj62fJ2nqTt++LoPJZy+trsddYfuDp5Q\n" +
			"Gys8RWB1f3+Dc26265KRERVMegLrBvyWZzP1ZSS3CHZ4Bx5LPqMEgMhd1jxqI/IE\n" +
			"0sUoDiw77VJJv/I5IkYLD1iIIr0jnzE559QxrGe0fl8ca56P71+NFSlBUQJRYTA5\n" +
			"UGQ09iZc+Cx3fzO7yuaY4uNtm5lP/06W5GNPCI5ZPNNRtSdXqJlBO50q9RUw/aZG\n" +
			"DSnVogooknPIre67CLlS0mn9TFPhQ7l7ktPurSWSNSRU5HcOv4Zugwpaix0MNWKT\n" +
			"eFXxTwSgH+mElLyWqSaZ\n" +
			"-----END CERTIFICATE-----";
	}
	@Override
	String clientKey() {
		return "-----BEGIN PRIVATE KEY-----\n" +
			"MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDY40e7zhFOxAZD\n" +
			"FNeUs7KAAWKjEHZFsGXxlrdgxb2Ab2AtyPY0fRZeFubFZPVl+2Bq+biL4gE2Cn4p\n" +
			"yy5bJiLoYtsEnRoYt2t2jwArWLa64jq0aij3wZm3rNW/xscRHHV8/uIHTD1x8uOm\n" +
			"bNjXqn4xCPZoTlldzvdZS5Rf7Rho8gj6j9q+ORvtpyzqiEHwCNek6iNNs/h5r/7U\n" +
			"KQT3znei1/YRMwkjbQ8WLS2ejJ8WbCg7wku8do0jpogQc4UflO/OzA4bqZyGaqhT\n" +
			"PMPXRxXTODTvZVBGEKb3E3YcW5rG4lyR15f4MEbfqbH2yaK9REpdD6DOXViO3Cck\n" +
			"7uYL3H9rAgMBAAECggEAKzc6S2MXksS/ZK1p+VDG//eGsmwszN0Fqxo3Ztpv1IAc\n" +
			"v3K8ECBejRfuGqXvucbZYudoTMMXL6/ujKBWF8ZlDT3pWyV8ljEyAfkjdP+jxWAf\n" +
			"h2RhzmAuo2kWXDSc5L1xcZsN6ZtY4PQfRUGKAYsQwu+Ava7i4qzDH3FOWsDQ7NPz\n" +
			"aVEEHd+BxWxE1Hf6HCaqiwHRLcFupCJYXjGE86rSLSdtqzF42QZdm2EKjDB9mx2A\n" +
			"J3RNbXpTqBTb0Pf7oWMlKzBbbcWCwlOn6pdrtPQ0wgW37qjRCD9KP6WPA+7Nx2Ot\n" +
			"+huOXyhTFCmpYd8dYMGTbqImgNkrpA1jGy4RR0MEAQKBgQDvDIj7z9jDbpfxvF5O\n" +
			"2GEh2tgnhnYOs7QfrGurnYtElSvG8kUxJvqNyROSlEIlOEyCC2f/50yUNUjGYlve\n" +
			"FEW+WZZW5ltgk4nOf8NlQISurnQkL3FgkJpxPhQq5M3Rg20bxBOCx35b2J9KPxjF\n" +
			"jU+UsOrFN2f6EDY2AGKDujszawKBgQDoRHPwgiZJDLfFukIrPGrL4TcyvymsiehM\n" +
			"BpnkZpaPH994ymS7XT0AwJq6ucazjPXqpbGf/M7opFTCol0AApSDHLTnmTCLDe8C\n" +
			"k//O+MgS+pGL7LlG9KEung4hhO9K81jC3wvaj1v6mmtivMm6Frdu8lMy/BeyMyb+\n" +
			"GYmK0wLkAQKBgGFsFWOyqF+1cb02DKVLcAKRIuiw1upU+IK0DUlqsnhn5uh0khhf\n" +
			"D1u7Z/uj6nUqxZt2NJNJW59HO/qF2XTfhDD4Z9nQlcr9NftvfF2GdsyOEo0wvJ8j\n" +
			"L54ZM7TrfKn3Sv824NC6ptedqAVlQ543YghyNavcUcbK1p0ZeQzAWCIzAoGBAL8M\n" +
			"x10WgQ1nU6ZlrdXIjYQETZYFXe8ZRKV8bItNwxwZOIUv3amsaOg8hUmAVCwOj50i\n" +
			"C7gwh7UersWmLd87QBOUwk0aWa6XmHjDahBB3LiXuK6i2ke+IvKoebEimn5JKkKo\n" +
			"myR89UhuSlReO4RA+UNeOgdfCtK+dp1ePh+dr+wBAoGASP8/tivXPA5m2nSKSScf\n" +
			"JuWW/3Vm4/CgFYqeIkD9fgY3VqrHwjTTjLdvvlAPD97cxODNbsPkJqdq8mjeVQ5g\n" +
			"FXZQjGS8GmQ2AD0JSez4jPF+KBFcCANAe04jWgLr6CcNT+1aM6pa+VaPKo2coJM6\n" +
			"dc7ydJkJRNosqFFYyqJTjTw=\n" +
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
