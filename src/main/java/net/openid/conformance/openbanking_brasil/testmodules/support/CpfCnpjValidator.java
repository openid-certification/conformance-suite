package net.openid.conformance.openbanking_brasil.testmodules.support;

public class CpfCnpjValidator {
	private static final int[] weightCPF = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
	private static final int[] weightCNPJ = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

	private static int calculateDigit(String str, int[] weight) {
		int sum = 0;
		for (int index=str.length()-1, digit; index >= 0; index-- ) {
			digit = Integer.parseInt(str.substring(index,index+1));
			sum += digit*weight[weight.length-str.length()+index];
		}
		sum = 11 - sum % 11;
		return sum > 9 ? 0 : sum;
	}

	public static boolean isValidCPF(String cpf) {
		if ((cpf==null) || (cpf.length()!=11)) return false;

		Integer firstDigit = calculateDigit(cpf.substring(0,9), weightCPF);
		Integer secondDigit = calculateDigit(cpf.substring(0,9) + firstDigit, weightCPF);
		return cpf.equals(cpf.substring(0,9) + firstDigit.toString() + secondDigit.toString());
	}

	public static boolean isValidCNPJ(String cnpj) {
		if ((cnpj==null)||(cnpj.length()!=14)) return false;

		Integer firstDigit = calculateDigit(cnpj.substring(0,12), weightCNPJ);
		Integer secondDigit = calculateDigit(cnpj.substring(0,12) + firstDigit, weightCNPJ);
		return cnpj.equals(cnpj.substring(0,12) + firstDigit.toString() + secondDigit.toString());
	}

	public static void main(String[] args) {
		System.out.printf("CPF Valido:%s \n", CpfCnpjValidator.isValidCPF("0711821306"));
		System.out.printf("CNPJ Valido:%s \n", CpfCnpjValidator.isValidCNPJ("13642634756318"));
	}
}
