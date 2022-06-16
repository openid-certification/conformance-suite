package net.openid.conformance.openbanking_brasil.testmodules.support.payments.PixQrCode;

import net.openid.conformance.openbanking_brasil.testmodules.support.payments.QrCodeKeys;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.pixqrcode.PixQRCode;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PixQrCodeTest {
	@Test
	public void happyPathPixQrCode(){
		PixQRCode qrCode = new PixQRCode();
		qrCode.setPayloadFormatIndicator("01");
		qrCode.setProxy("cliente-a00001@pix.bcb.gov.br");
		qrCode.setMerchantCategoryCode("0000");
		qrCode.setTransactionCurrency("986");
		qrCode.setTransactionAmount("123.45");
		qrCode.setCountryCode("BR");
		qrCode.setMerchantName("JOAO SILVA");
		qrCode.setMerchantCity("BELO HORIZONTE");
		qrCode.setAdditionalField("03***");

		assertEquals(QrCodeKeys.QRES_WRONG_AMOUNT, qrCode.toString());
	}
}
