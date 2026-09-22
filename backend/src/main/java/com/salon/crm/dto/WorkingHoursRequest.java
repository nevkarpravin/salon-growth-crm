package com.salon.crm.dto;

import java.util.List;

public record WorkingHoursRequest(List<Entry> hours) {
    public record Entry(String dayOfWeek, String startTime, String endTime) {
    }
}
