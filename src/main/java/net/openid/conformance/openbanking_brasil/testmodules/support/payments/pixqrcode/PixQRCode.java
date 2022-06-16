package net.openid.conformance.openbanking_brasil.testmodules.support.payments.pixqrcode;

import org.apache.commons.lang3.StringUtils;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class PixQRCode {

	// Payload Format Indicator
	private Tag payloadFormatIndicator;

	// Merchant Category Code
	private Tag merchantCategoryCode;

	// Transaction Currency
	private Tag transactionCurrency;

	// Transaction Amount
	private Tag transactionAmount;

	// Country Code
	private Tag countryCode;

	// Merchant Name
	private Tag merchantName;

	// Merchant City
	private Tag merchantCity;

	// Postal Code
	private Tag postalCode;

	private Tag proxy;

	//
	private Tag additionalField;

	public void setPayloadFormatIndicator(String payloadFormatIndicator) {
		this.payloadFormatIndicator = new Tag(PixQrCodeConstants.ID_PAYLOAD_FORMAT_INDICATOR, payloadFormatIndicator);
	}

	public void setMerchantCategoryCode(String merchantCategoryCode) {
		this.merchantCategoryCode = new Tag(PixQrCodeConstants.ID_MERCHANT_CATEGORY_CODE,merchantCategoryCode);
	}

	public void setTransactionCurrency(String transactionCurrency) {
		this.transactionCurrency = new Tag(PixQrCodeConstants.ID_TRANSACTION_CURRENCY,transactionCurrency);
	}

	public void setTransactionAmount(String transactionAmount) {
		this.transactionAmount = new Tag(PixQrCodeConstants.ID_TRANSACTION_AMOUNT,transactionAmount);
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = new Tag(PixQrCodeConstants.ID_COUNTRY_CODE,countryCode);
	}

	public void setMerchantName(String merchantName) {
		this.merchantName = new Tag(PixQrCodeConstants.ID_MERCHANT_NAME, merchantName);
	}

	public void setMerchantCity(String merchantCity) {
		this.merchantCity = new Tag(PixQrCodeConstants.ID_MERCHANT_CITY, merchantCity);
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = new Tag(PixQrCodeConstants.ID_POSTAL_CODE,postalCode);
	}

	public void setProxy(String proxy) {

		this.proxy = new Tag(PixQrCodeConstants.ID_MERCHANT_ACCOUNT_INFORMATION_RESERVED_ADDITIONAL_RANGE_START,
				String.format("0014BR.GOV.BCB.PIX0129%s",proxy));
	}

	public void setAdditionalField(String transactionID){
		this.additionalField = new Tag(PixQrCodeConstants.ID_ADDITIONAL_DATA_FIELD_TEMPLATE,
			String.format("05%s", transactionID));
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder(toStringWithoutCrc16());

		final String string = sb.toString();

		if (StringUtils.isBlank(string)) {
			return StringUtils.EMPTY;
		}

		final int crc16 = CRC.crc16(sb.toString().getBytes(StandardCharsets.UTF_8));

		sb.append(String.format("%04X", crc16));

		return sb.toString();
	}

	public String toStringWithoutCrc16() {
		final StringBuilder sb = new StringBuilder();

		Optional.ofNullable(payloadFormatIndicator).ifPresent(tag -> sb.append(tag.toString()));
		Optional.ofNullable(proxy).ifPresent(tag -> sb.append(tag.toString()));
		Optional.ofNullable(merchantCategoryCode).ifPresent(tag -> sb.append(tag.toString()));
		Optional.ofNullable(transactionCurrency).ifPresent(tag -> sb.append(tag.toString()));
		Optional.ofNullable(transactionAmount).ifPresent(tag -> sb.append(tag.toString()));
		Optional.ofNullable(countryCode).ifPresent(tag -> sb.append(tag.toString()));
		Optional.ofNullable(merchantName).ifPresent(tag -> sb.append(tag.toString()));
		Optional.ofNullable(merchantCity).ifPresent(tag -> sb.append(tag.toString()));
		Optional.ofNullable(postalCode).ifPresent(tag -> sb.append(tag.toString()));
		Optional.ofNullable(additionalField).ifPresent(tag -> sb.append(tag.toString()));

		final String string = sb.toString();

		if (StringUtils.isBlank(string)) {
			return StringUtils.EMPTY;
		}

		sb.append(String.format("%s%s", PixQrCodeConstants.ID_CRC, "04"));

		return sb.toString();
	}



}
