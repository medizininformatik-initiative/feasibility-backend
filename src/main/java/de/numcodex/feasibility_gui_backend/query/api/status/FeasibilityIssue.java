package de.numcodex.feasibility_gui_backend.query.api.status;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@JsonSerialize(using = FeasibilityIssueSerializer.class)
public enum FeasibilityIssue {

    USER_NOT_LOGGED_IN(10001, "Unauthorized", IssueType.FORBIDDEN, Severity.ERROR, false),
    USER_INCORRECT_ACCESS_RIGHTS(10002, "Missing access right for api call", IssueType.FORBIDDEN, Severity.ERROR),
    USER_BLACKLISTED_NOT_POWER_USER(10003, "Too many requests - user is blacklisted", IssueType.FORBIDDEN, Severity.ERROR),
    QUOTA_EXCEEDED(10004, "Too many requests - quota reached", IssueType.BUSY, Severity.ERROR),
    POLLING_LIMIT_EXCEEDED(10005, "Too many Requests - polling limit exceeded", IssueType.BUSY, Severity.ERROR),
    PRIVACY_RESTRICTION_RESULT_SIZE(10006, "Response insufficiently large", IssueType.BUSY, Severity.ERROR),
    PRIVACY_RESTRICTION_RESULT_SITES(10007, "Number of responded sites too small", IssueType.BUSY, Severity.ERROR);

    private static final FeasibilityIssue[] VALUES;

    static {
        VALUES = values();
    }


    private final int code;

    private final String message;

    private final IssueType type;

    private final Severity severity;

    private final Boolean retry;

    FeasibilityIssue(int code, String message, IssueType type, Severity severity, Boolean retry) {
        this.code = code;
        this.message = message;
        this.type = type;
        this.severity = severity;
        this.retry = retry;
    }

    FeasibilityIssue(int code, String message, IssueType type, Severity severity) {
        this.code = code;
        this.message = message;
        this.type = type;
        this.severity = severity;
        this.retry = null;
    }

    public String message() {
        return this.message;
    }

    public IssueType type() {
        return this.type;
    }

    public int code() {
        return this.code;
    }

    public Severity severity() {
        return this.severity;
    }

    public Boolean retry() {
        return this.retry;
    }

    public static FeasibilityIssue valueOf(int feasibilityIssueCode) {
        FeasibilityIssue feasibilityIssue = resolve(feasibilityIssueCode);
        if (feasibilityIssue == null) {
            throw new IllegalArgumentException("No matching Feasibility issue for code " + feasibilityIssueCode);
        }
        return feasibilityIssue;
    }

    @Nullable
    public static FeasibilityIssue resolve(int feasibilityIssueCode) {
        for (FeasibilityIssue feasibilityIssue : VALUES) {
            if (feasibilityIssue.code == feasibilityIssueCode) {
                return feasibilityIssue;
            }
        }
        return null;
    }

    public enum IssueType {

        FORBIDDEN("forbidden"),
        BUSY("busy");
        private final String value;

        IssueType(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }

    public enum Severity {

        ERROR("error");
        private final String value;

        Severity(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }
}
