package api.web.basis.requests;

import com.retapps.smartbip.api.web.basis.dtos.ProfileSignUpDto;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import javax.validation.Valid;
import java.io.Serializable;

/**
 * Dto for profile registration
 */
public class ProfileSignUpRequest implements Serializable {

    private static final long serialVersionUID = 1564600065502385393L;

    @Valid
    private ProfileSignUpDto profile;

    private String username;

    private String password;

    public ProfileSignUpDto getProfile() {
        return profile;
    }

    public void setProfile(ProfileSignUpDto profile) {
        this.profile = profile;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toStringExclude(this, "password");
    }
}
