package com.retapps.smartbip.api.web.basis.requests;

import com.retapps.smartbip.basis.models.StoreDevice;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class StoreDeviceStatusRequest implements Serializable {

    private static final long serialVersionUID = 1714035136395685784L;

    @NotBlank
    private String tid;

    @NotBlank
    private String storeId;

    @NotNull
    private StoreDevice storeDevice;

    private Boolean boot;

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public StoreDevice getStoreDevice() {
        return storeDevice;
    }

    public void setStoreDevice(StoreDevice storeDevice) {
        this.storeDevice = storeDevice;
    }

    public Boolean getBoot() {
        return boot;
    }

    public void setBoot(Boolean boot) {
        this.boot = boot;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
