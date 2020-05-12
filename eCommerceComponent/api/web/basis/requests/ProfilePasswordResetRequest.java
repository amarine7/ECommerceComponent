package com.retapps.smartbip.api.web.basis.requests;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.validator.constraints.NotBlank;

import java.io.Serializable;

public class ProfilePasswordResetRequest implements Serializable {

    private static final long serialVersionUID = -419836882283355411L;

    @NotBlank
    private String tid;

    @NotBlank
    private String email;

    public ProfilePasswordResetRequest() {
    }

    public ProfilePasswordResetRequest(String email) {
        this.email = email;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
