package api.web.basis.requests;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

public class NotificationSubscriptionRequest implements Serializable {

    private String profileId;

    private String registrationId;

    private Boolean isOperator;

    public NotificationSubscriptionRequest() {

    }

    public NotificationSubscriptionRequest(String retailerId, String profileId, String registrationId, Boolean isOperator) {

        this.profileId = profileId;
        this.registrationId = registrationId;
        this.isOperator = isOperator;
    }

    public String getProfileId() {

        return profileId;
    }

    public void setProfileId(String profileId) {

        this.profileId = profileId;
    }

    public String getRegistrationId() {

        return registrationId;
    }

    public void setRegistrationId(String registrationId) {

        this.registrationId = registrationId;
    }

    public Boolean getOperator() {

        return isOperator;
    }

    public void setOperator(Boolean operator) {

        isOperator = operator;
    }

    @Override
    public String toString() {

        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
