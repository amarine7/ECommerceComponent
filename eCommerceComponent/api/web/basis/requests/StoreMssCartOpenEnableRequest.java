package com.retapps.smartbip.api.web.basis.requests;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

public class StoreMssCartOpenEnableRequest implements Serializable {

    private static final long serialVersionUID = -9166746475368692343L;

    private Boolean mssCartOpenEnabled;

    public Boolean getMssCartOpenEnabled() {
        return mssCartOpenEnabled;
    }

    public void setMssCartOpenEnabled(Boolean mssCartOpenEnabled) {
        this.mssCartOpenEnabled = mssCartOpenEnabled;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
