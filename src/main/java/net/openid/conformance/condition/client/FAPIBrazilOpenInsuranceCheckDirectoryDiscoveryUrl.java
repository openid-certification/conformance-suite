package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
