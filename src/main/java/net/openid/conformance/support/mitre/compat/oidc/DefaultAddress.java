package net.openid.conformance.support.mitre.compat.oidc;


public class DefaultAddress implements Address {

	private static final long serialVersionUID = -1304880008685206811L;

	private Long id;
	private String formatted;
	private String streetAddress;
	private String locality;
	private String region;
	private String postalCode;
	private String country;

	/**
	 * Empty constructor
	 */
	public DefaultAddress() {

	}

	/**
	 * Copy constructor from an existing address.
	 * @param address
	 */
	@SuppressWarnings("this-escape")
	public DefaultAddress(Address address) {
		setFormatted(address.getFormatted());
		setStreetAddress(address.getStreetAddress());
		setLocality(address.getLocality());
		setRegion(address.getRegion());
		setPostalCode(address.getPostalCode());
		setCountry(address.getCountry());
	}

	/**
	 * @return the formatted address string
	 */
	@Override
	public String getFormatted() {
		return formatted;
	}
	/**
	 * @param formatted the formatted address to set
	 */
	@Override
	public void setFormatted(String formatted) {
		this.formatted = formatted;
	}
	/**
	 * @return the streetAddress
	 */
	@Override
	public String getStreetAddress() {
		return streetAddress;
	}
	/**
	 * @param streetAddress the streetAddress to set
	 */
	@Override
	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}
	/**
	 * @return the locality
	 */
	@Override
	public String getLocality() {
		return locality;
	}
	/**
	 * @param locality the locality to set
	 */
	@Override
	public void setLocality(String locality) {
		this.locality = locality;
	}
	/**
	 * @return the region
	 */
	@Override
	public String getRegion() {
		return region;
	}
	/**
	 * @param region the region to set
	 */
	@Override
	public void setRegion(String region) {
		this.region = region;
	}
	/**
	 * @return the postalCode
	 */
	@Override
	public String getPostalCode() {
		return postalCode;
	}
	/**
	 * @param postalCode the postalCode to set
	 */
	@Override
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}
	/**
	 * @return the country
	 */
	@Override
	public String getCountry() {
		return country;
	}
	/**
	 * @param country the country to set
	 */
	@Override
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @return the id
	 */
	@Override
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((country == null) ? 0 : country.hashCode());
		result = prime * result + ((formatted == null) ? 0 : formatted.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((locality == null) ? 0 : locality.hashCode());
		result = prime * result + ((postalCode == null) ? 0 : postalCode.hashCode());
		result = prime * result + ((region == null) ? 0 : region.hashCode());
		result = prime * result + ((streetAddress == null) ? 0 : streetAddress.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof DefaultAddress)) {
			return false;
		}
		DefaultAddress other = (DefaultAddress) obj;
		if (country == null) {
			if (other.country != null) {
				return false;
			}
		} else if (!country.equals(other.country)) {
			return false;
		}
		if (formatted == null) {
			if (other.formatted != null) {
				return false;
			}
		} else if (!formatted.equals(other.formatted)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (locality == null) {
			if (other.locality != null) {
				return false;
			}
		} else if (!locality.equals(other.locality)) {
			return false;
		}
		if (postalCode == null) {
			if (other.postalCode != null) {
				return false;
			}
		} else if (!postalCode.equals(other.postalCode)) {
			return false;
		}
		if (region == null) {
			if (other.region != null) {
				return false;
			}
		} else if (!region.equals(other.region)) {
			return false;
		}
		if (streetAddress == null) {
			if (other.streetAddress != null) {
				return false;
			}
		} else if (!streetAddress.equals(other.streetAddress)) {
			return false;
		}
		return true;
	}

}
