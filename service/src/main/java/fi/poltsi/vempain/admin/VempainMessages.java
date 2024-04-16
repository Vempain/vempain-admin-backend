package fi.poltsi.vempain.admin;

import lombok.experimental.UtilityClass;

@UtilityClass
public class VempainMessages {
    public static final String INVALID_USER_SESSION             = "User session is not valid";
    public static final String INVALID_USER_SESSION_MSG         = "User attempted an action without proper session";
    public static final String UNAUTHORIZED_ACCESS              = "Insufficient user permissions";
    public static final String NO_LAYOUT_FOUND_BY_ID            = "Failed to find layout by ID";
    public static final String MALFORMED_ID_IN_REQUEST          = "Malformed ID in request";
    public static final String MALFORMED_OBJECT_NAME_IN_REQUEST = "Malformed object name in request";
    public static final String MALFORMED_ID_IN_REQUEST_MSG      = "Request contains a malformed ID: {}";
    public static final String INTERNAL_ERROR                   = "Internal server error";
    public static final String OBJECT_NOT_FOUND                 = "Object not found";

    public static final String MESSAGES_INSTANTIATION_ERROR = "Static message class";
    public static final String OBJECT_NAME_ALREADY_EXISTS   = "Object name already exists";
    public static final String MALFORMED_OBJECT_IN_REQUEST  = "Request object is malformed";
}
