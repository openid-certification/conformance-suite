package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

public class QrCodeKeys {
    
	// Valid QrCode with proxy cliente-a00001@pix.bcb.gov.br without the ammount value
	public static final String QRES_EMAIL = "00020126510014BR.GOV.BCB.PIX0129cliente-a00001@pix.bcb.gov.br5204000053039865802BR5910JOAO SILVA6014BELO HORIZONTE62070503***63042A73";
	// Valid QrCode with proxy cliente-a00001@pix.bcb.gov.br with 123.45 as an amount value
	public static final String QRES_WRONG_AMOUNT = "00020126510014BR.GOV.BCB.PIX0129cliente-a00001@pix.bcb.gov.br5204000053039865406123.455802BR5910JOAO SILVA6014BELO HORIZONTE62070503***6304B7E7";
	// Valid QrCode with proxy cliente-a00001@pix.bcb.gov.br with city equal to SALVADOR instead of BELO HORIZONTE
	public static final String QRES_EMAIL_WRONG_CITY="00020126510014BR.GOV.BCB.PIX0129cliente-a00001@pix.bcb.gov.br5204000053039865802BR5910JOAO SILVA6008SALVADOR62070503***6304AE08";
	// Invalid QrCode with proxy cliente-a00001@pix.bcb.gov.br. Currency code is equal to 666 instead of default 986 value
	public static final String QRES_EMAIL_WRONG_CURRENCY="00020126510014BR.GOV.BCB.PIX0129cliente-a00001@pix.bcb.gov.br5204000053036665802BR5910JOAO SILVA6014BELO HORIZONTE62070503***6304364A";
	// Valid QrCode with proxy +5561990010001 with ammount equal to 100.00 -> To be used only on the happy phone test
	public static final String QRES_PHONE_NUMBER = "00020126360014BR.GOV.BCB.PIX0114+55619900100015204000053039865406100.005802BR5910JOAO SILVA6014BELO HORIZONTE62070503***6304DA56";
}
