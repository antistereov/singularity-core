package io.stereov.singularity.auth.oauth2.model

enum class OAuth2ErrorCode(val value: String) {
    AUTHENTICATION_FAILED("authentication_failed"),
    STATE_PARAMETER_MISSING("state_parameter_missing"),
    STATE_EXPIRED("state_expired"),
    INVALID_STATE("invalid_state"),
    USER_ALREADY_AUTHENTICATED("user_already_authenticated"),
    EMAIL_ALREADY_REGISTERED("email_already_registered"),
    SESSION_TOKEN_MISSING("session_token_missing"),
    SESSION_TOKEN_EXPIRED("session_token_expired"),
    INVALID_SESSION_TOKEN("invalid_session_token"),
    SUB_CLAIM_MISSING("sub_claim_missing"),
    EMAIL_CLAIM_MISSING("email_claim_missing"),
    PROVIDER_ALREADY_CONNECTED("provider_already_connected"),
    CONNECTED_TO_ANOTHER_PRINCIPAL("connected_to_another_principal"),
    CONNECTION_TOKEN_EXPIRED("connection_token_expired"),
    INVALID_CONNECTION_TOKEN("invalid_connection_token"),
    CONNECTION_TOKEN_PROVIDER_MISMATCH("connection_token_provider_mismatch"),
    STEP_UP_MISSING("step_up_missing"),
    STEP_UP_TOKEN_EXPIRED("step_up_token_expired"),
    INVALID_STEP_UP_TOKEN("invalid_step_up_token"),
    ACCESS_TOKEN_MISSING("access_token_missing"),
    INVALID_ACCESS_TOKEN("invalid_access_token"),
    ACCESS_TOKEN_EXPIRED("access_token_expired"),
    WRONG_ACCOUNT_AUTHENTICATED("wrong_account_authenticated"),
    SERVER_ERROR("server_error");

    override fun toString(): String {
        return value
    }
}