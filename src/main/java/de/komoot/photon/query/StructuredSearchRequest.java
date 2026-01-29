package de.komoot.photon.query;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class StructuredSearchRequest extends SearchRequestBase {
    @Nullable private String countryCode;
    @Nullable private String state;
    @Nullable private String county;
    @Nullable private String city;
    @Nullable private String postCode;
    @Nullable private String district;
    @Nullable private String street;
    @Nullable private String houseNumber;

    @Nullable
    public String getCounty() {
        return county;
    }

    public void setCounty(@Nullable String county) {
        this.county = county;
    }

    @Nullable
    public String getCity() {
        return city;
    }

    public void setCity(@Nullable String city) {
        this.city = city;
    }

    @Nullable
    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(@Nullable String postCode) {
        this.postCode = postCode;
    }

    @Nullable
    public String getDistrict() {
        return district;
    }

    public void setDistrict(@Nullable String district) {
        this.district = district;
    }

    @Nullable
    public String getStreet() {
        return street;
    }

    public void setStreet(@Nullable String street) {
        this.street = street;
    }

    @Nullable
    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(@Nullable String houseNumber) {
        this.houseNumber = houseNumber;
    }

    @Nullable
    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(@Nullable String countryCode) {
        this.countryCode = countryCode;
    }

    @Nullable
    public String getState() {
        return state;
    }

    public void setState(@Nullable String state) {
        this.state = state;
    }

    public boolean hasState() { return state != null && !state.isBlank(); }

    public boolean hasDistrict() { return district != null && !district.isBlank(); }

    public boolean hasPostCode() { return postCode != null && !postCode.isBlank(); }

    public boolean hasCityOrPostCode() { return (city != null && !city.isBlank()) || hasPostCode(); }

    public boolean hasCounty() { return county != null && !county.isBlank(); }

    public boolean hasStreet() { return (street != null && !street.isBlank()) || hasHouseNumber(); }

    public boolean hasHouseNumber() { return houseNumber != null && !houseNumber.isBlank(); }
}
