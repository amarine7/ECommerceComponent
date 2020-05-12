package api.web.basis.requests;

import basis.models.MailBox;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

public class CreateMailBoxRequest implements Serializable {

    private static final long serialVersionUID = -4135313557444218415L;

    private MailBox notification;

    public CreateMailBoxRequest() {

    }

    public MailBox getNotification() {
        return notification;
    }

    public void setNotification(MailBox notification) {
        this.notification = notification;
    }

    @Override
    public String toString() {

        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
