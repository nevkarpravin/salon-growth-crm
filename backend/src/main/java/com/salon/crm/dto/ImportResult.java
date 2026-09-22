package com.salon.crm.dto;

import java.util.List;

public record ImportResult(
        int imported,
        int skipped,
        List<RowError> errors
) {
    public record RowError(int row, String message) {
    }
}
