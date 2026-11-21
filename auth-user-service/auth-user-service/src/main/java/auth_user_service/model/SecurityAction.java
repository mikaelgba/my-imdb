package auth_user_service.model;

public enum SecurityAction {
    USER_REGISTERED,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    ACCOUNT_LOCKED,
    TOKEN_REFRESHED,
    TOKEN_REUSE_DETECTED,
    LOGOUT,
    ROLE_CHANGED,
    STATUS_CHANGED,
    PASSWORD_CHANGED
}
