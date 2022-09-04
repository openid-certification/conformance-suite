package net.openid.conformance.condition.client;

public class FAPIBrazilOpenInsuranceCheckDirectoryDiscoveryUrl  extends AbstractFAPIBrazilCheckDirectoryDiscoveryUrl {

	@Override
	String getErrorMessage() {
		return "Testing for Brazil certification must be done using the Brazil directory.";
	}

	@Override
	String getExpectedUrl() {
		return "https://auth.sandbox.directory.opinbrasil.com.br/.well-known/openid-configuration";
	}

}
