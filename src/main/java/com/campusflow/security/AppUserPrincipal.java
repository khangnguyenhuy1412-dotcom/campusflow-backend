package com.campusflow.security;

import com.campusflow.user.entity.Role;
import com.campusflow.user.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security view of a {@link User}, used only during email/password login.
 * Requests authenticated by JWT carry a {@code Jwt} principal instead.
 */
public record AppUserPrincipal(Long id, String email, String password, boolean active, List<String> roles)
        implements UserDetails {

    public static AppUserPrincipal from(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getPassword(), user.isActive(), roles);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
