package com.campusflow.exchange.dto;

import com.campusflow.user.entity.User;

/** Contact details of the other party, only revealed once a request is accepted. */
public record ContactInfo(String fullName, String email, String phoneNumber) {

    public static ContactInfo from(User user) {
        return new ContactInfo(user.getFullName(), user.getEmail(), user.getPhoneNumber());
    }
}
