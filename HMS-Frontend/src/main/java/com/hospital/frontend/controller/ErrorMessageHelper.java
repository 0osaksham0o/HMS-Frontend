package com.hospital.frontend.controller;

/**
 * Translates raw database / API exception messages into simple,
 * user-friendly sentences that can be shown safely in the UI.
 */
public final class ErrorMessageHelper {

    private ErrorMessageHelper() {}

    /**
     * Returns a clean, user-readable error message for any exception
     * that may originate from the backend API (database constraints, etc.).
     */
    public static String friendly(Exception e) {
        String raw = e.getMessage();
        if (raw == null) return "An unexpected error occurred. Please try again.";

        String lower = raw.toLowerCase();

        // ── Foreign-key / referential-integrity violations ─────────────────
        if (lower.contains("foreign key constraint") || lower.contains("a foreign key constraint fails")) {
            // Try to extract the table name after "REFERENCES `…`"
            String referenced = extractReferences(raw);
            if (referenced != null) {
                return "Cannot complete this action: this record is still referenced by related data in \""
                        + referenced + "\". Please remove those linked records first.";
            }
            return "Cannot complete this action: this record is still linked to other data in the system. "
                    + "Please remove the related records first.";
        }

        // ── Duplicate / unique-key violations ─────────────────────────────
        if (lower.contains("duplicate entry") || lower.contains("unique constraint")) {
            return "A record with that ID or value already exists. Please use a different value.";
        }

        // ── Data-too-long / truncation ─────────────────────────────────────
        if (lower.contains("data too long") || lower.contains("value too long")) {
            return "One of the entered values is too long. Please shorten it and try again.";
        }

        // ── Null / not-null constraint ─────────────────────────────────────
        if (lower.contains("cannot be null") || lower.contains("not null constraint")) {
            return "A required field is missing. Please fill in all required fields.";
        }

        // ── Generic database error (hide internal details) ─────────────────
        if (lower.contains("sql") || lower.contains("jdbc") || lower.contains("hibernate")
                || lower.contains("constraint")) {
            return "A database error occurred. Please check your input and try again.";
        }

        // ── HTTP / connection errors ───────────────────────────────────────
        if (lower.contains("connection refused") || lower.contains("connect timed out")) {
            return "Could not reach the server. Please check that the backend service is running.";
        }

        if (lower.contains("404") || lower.contains("not found")) {
            return "The requested record was not found.";
        }

        if (lower.contains("400") || lower.contains("bad request")) {
            return "Invalid input. Please review the form and try again.";
        }

        // ── Pass through short, already-friendly messages ──────────────────
        if (raw.length() <= 120 && !lower.contains("stack") && !lower.contains("exception")) {
            return raw;
        }

        // ── Fallback ───────────────────────────────────────────────────────
        return "Cannot delete this record: it is linked to other data in the system. Please remove the related records first.";
    }

    /**
     * Attempts to extract the referenced table name from a MySQL FK error message.
     * Example: "… REFERENCES `Department` (`DepartmentID`)" → "Department"
     */
    private static String extractReferences(String raw) {
        // Pattern: REFERENCES `TableName`
        int idx = raw.toUpperCase().indexOf("REFERENCES `");
        if (idx != -1) {
            int start = idx + "REFERENCES `".length();
            int end   = raw.indexOf('`', start);
            if (end > start) {
                return raw.substring(start, end);
            }
        }
        // Also handle: REFERENCES 'TableName'
        idx = raw.toUpperCase().indexOf("REFERENCES '");
        if (idx != -1) {
            int start = idx + "REFERENCES '".length();
            int end   = raw.indexOf('\'', start);
            if (end > start) return raw.substring(start, end);
        }
        return null;
    }
}
