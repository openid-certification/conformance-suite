package net.openid.conformance.fapiciba.rp;

public class ConnectIdAuCibaRPProfileBehavior extends FAPICIBARPProfileBehavior {

	@Override
	public void exposeProfileSpecificEndpoints() {
		module.exposeEnvString("userinfo_endpoint", "server", "userinfo_endpoint");
	}

}