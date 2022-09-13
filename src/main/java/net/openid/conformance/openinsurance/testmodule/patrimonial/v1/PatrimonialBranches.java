package net.openid.conformance.openinsurance.testmodule.Patrimonial.v1;

public enum PatrimonialBranches {
	ASSISTENCIA ("0112"),
	COMPREENSIVO_CONDOMINIO ("0116"),
	COMPREENSIVO_EMPRESARIAL("0118"),
	LUCROS_CESSANTES ("0141"),
	RISCOS_ENGENHARIA ("0167"),
	RISCOS_DIVERSOS ("0171"),
	GLOBAL_BANCOS ("0173"),
	GARANTIA_ESTENDIDA ("0195"),
	RISCOS_NOMEADOS ("0196"),
	COMPREENSIVO_RESIDENCIAL ("0114")
	;



	private final String branchCode;
	PatrimonialBranches(String branchCode) {
		this.branchCode = branchCode;
	}

	public String getBranchCode() {
		return branchCode;
	}
}
