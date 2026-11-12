package com.campusflow.wanted.dto;

/** @param fulfilled true if the student found the item, false if they just stop looking */
public record CloseWantedPostRequest(boolean fulfilled) {
}
