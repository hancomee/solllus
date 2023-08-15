package com.solllus.web.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

public class UserDetail implements UserDetails {

    Set<GrantedAuthority> authorities = new HashSet<>();
    public Set<String> roles = new HashSet<>();

    private String username;
    private String password;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public UserDetail setPassword(String password) {
        this.password = password;
        return this;
    }

    public UserDetail setUsername(String username) {
        this.username = username;
        return this;
    }

    // ADMIN MASTER :: 공백으로 구분해 여러개를 적어넣을 수 있다.
    public UserDetail setRoles(String roles) {
        authorities = new HashSet<>();
        if (roles != null && !roles.isEmpty()) {
            String[] values = roles.split("\\s+|,\\s*");
            for (String value : values) {
                authorities.add(new SimpleGrantedAuthority(value));
                this.roles.add(value);
            }

        }
        return this;
    }

    public boolean hasRole(String role) {
        for (GrantedAuthority grantedAuthority : authorities)
            if (grantedAuthority.getAuthority().equals(role)) return true;
        return false;
    }

    public Map<String, Object> toJSON() {
        Map<String, Object> map = new HashMap<>();
        map.put("password", password);
        if(!authorities.isEmpty()) map.put("roles", String.join(" ", authorities.stream().map((v) -> v.getAuthority()).collect(Collectors.toSet())));
        return map;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String toString() {
        return username + " / " + password;
    }
}
