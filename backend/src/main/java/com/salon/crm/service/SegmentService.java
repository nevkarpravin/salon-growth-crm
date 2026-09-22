package com.salon.crm.service;

import com.salon.crm.config.SegmentProperties;
import com.salon.crm.entity.Client;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class SegmentService {

    public static final String NEW = "NEW";
    public static final String LAPSED = "LAPSED";
    public static final String AT_RISK = "AT_RISK";
    public static final String VIP = "VIP";
    public static final String BIRTHDAY_THIS_MONTH = "BIRTHDAY_THIS_MONTH";

    private final SegmentProperties props;

    public SegmentService(SegmentProperties props) {
        this.props = props;
    }

    public List<String> segmentsFor(Client client, LocalDate lastVisitDate, long visitCount, BigDecimal totalSpend) {
        return segmentsFor(client, lastVisitDate, visitCount, totalSpend, LocalDate.now());
    }

    public List<String> segmentsFor(Client client, LocalDate lastVisitDate, long visitCount,
                                  BigDecimal totalSpend, LocalDate today) {
        List<String> segments = new ArrayList<>();

        boolean isNew;
        if (visitCount > 0 && lastVisitDate != null) {
            isNew = ChronoUnit.DAYS.between(lastVisitDate, today) <= props.getNewDays()
                    && visitCount == 1;
        } else {
            isNew = client.getCreatedAt() != null
                    && ChronoUnit.DAYS.between(toLocalDate(client.getCreatedAt()), today) <= props.getNewDays();
        }
        if (isNew) {
            segments.add(NEW);
        }

        if (visitCount > 0 && lastVisitDate != null) {
            long daysSinceVisit = ChronoUnit.DAYS.between(lastVisitDate, today);
            if (daysSinceVisit > props.getLapsedDays()) {
                segments.add(LAPSED);
            } else if (daysSinceVisit >= props.getAtRiskMinDays()
                    && daysSinceVisit <= props.getAtRiskMaxDays()) {
                segments.add(AT_RISK);
            }
        }

        boolean vip = (totalSpend != null
                && totalSpend.compareTo(BigDecimal.valueOf(props.getVipSpend())) >= 0)
                || visitCount >= props.getVipVisits();
        if (vip) {
            segments.add(VIP);
        }

        if (client.getDateOfBirth() != null
                && client.getDateOfBirth().getMonth() == today.getMonth()) {
            segments.add(BIRTHDAY_THIS_MONTH);
        }

        return segments;
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
