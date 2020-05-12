package api.web.basis.requests;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

public class ProfileFlagsRequest implements Serializable {

    private static final long serialVersionUID = -2760782988936337063L;

    private Boolean accept1;

    private Boolean accept2;

    private Boolean accept3;

    private Boolean accept4;

    public Boolean getAccept1() {
        return accept1;
    }

    public void setAccept1(Boolean accept1) {
        this.accept1 = accept1;
    }

    public Boolean getAccept2() {
        return accept2;
    }

    public void setAccept2(Boolean accept2) {
        this.accept2 = accept2;
    }

    public Boolean getAccept3() {
        return accept3;
    }

    public void setAccept3(Boolean accept3) {
        this.accept3 = accept3;
    }

    public Boolean getAccept4() {
        return accept4;
    }

    public void setAccept4(Boolean accept4) {
        this.accept4 = accept4;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
