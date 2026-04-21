package com.medibook.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    /*
     * HTTP status code of the error.
     * Example: 400, 401, 403, 404, 409, 500
     * Client uses this to understand what went wrong.
     */
    private int status;

    /*
     * Short name of the error.
     * Example: Bad Request, Not Found, Conflict
     * Matches the HTTP status code name.
     */
    private String error;

    /*
     * Detailed human readable error message.
     * Example: Slot not found with id: 5
     * This is what the frontend shows to the user.
     */
    private String message;

    /*
     * Which API endpoint caused this error?
     * Example: /slots/5 or /appointments/book
     * Helps with debugging and logging.
     */
    private String path;

    /*
     * When did this error happen?
     * Auto set when error response is created.
     * Used for error logging and monitoring.
     */
    private LocalDateTime timestamp;
}