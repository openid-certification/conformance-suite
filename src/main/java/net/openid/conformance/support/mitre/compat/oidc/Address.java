package net.openid.conformance.support.mitre.compat.oidc;

import java.io.Serializable;

public interface Address extends Serializable {

	/**
	 * Get the system-specific ID of the Address object
	 * @return
	 */
	Long getId();

	/**
	 * @return the formatted address
	 */
	String getFormatted();

	/**
	 * @param formatted the formatted address to set
	 */
	void setFormatted(String formatted);

	/**
	 * @return the streetAddress
	 */
	String getStreetAddress();

	/**
	 * @param streetAddress the streetAddress to set
	 */
	void setStreetAddress(String streetAddress);

	/**
	 * @return the locality
	 */
	String getLocality();

	/**
	 * @param locality the locality to set
	 */
	void setLocality(String locality);

	/**
	 * @return the region
	 */
	String getRegion();

	/**
	 * @param region the region to set
	 */
	void setRegion(String region);

	/**
	 * @return the postalCode
	 */
	String getPostalCode();

	/**
	 * @param postalCode the postalCode to set
	 */
	void setPostalCode(String postalCode);

	/**
	 * @return the country
	 */
	String getCountry();

	/**
	 * @param country the country to set
	 */
	void setCountry(String country);

}
