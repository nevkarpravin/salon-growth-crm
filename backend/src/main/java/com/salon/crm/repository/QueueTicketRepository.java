package com.salon.crm.repository;

import com.salon.crm.entity.PaymentStatus;
import com.salon.crm.entity.QueueTicket;
import com.salon.crm.entity.QueueTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface QueueTicketRepository extends JpaRepository<QueueTicket, UUID> {

    List<QueueTicket> findByQueueDateOrderByQueueOrderAsc(LocalDate date);

    List<QueueTicket> findByQueueDateAndStatusInOrderByQueueOrderAsc(LocalDate date,
                                                                     Set<QueueTicketStatus> statuses);

    List<QueueTicket> findByStatusIn(Set<QueueTicketStatus> statuses);

    Optional<QueueTicket> findFirstByClient_PhoneAndStatusInOrderByJoinedAtDesc(
            String phone, Set<QueueTicketStatus> statuses);

    @Query("select coalesce(max(t.tokenNumber), 0) from QueueTicket t where t.queueDate = :date")
    int maxTokenNumberForDate(@Param("date") LocalDate date);

    @Query("select t from QueueTicket t where t.status = com.salon.crm.entity.QueueTicketStatus.COMPLETED "
            + "and t.finishedAt < :before and t.reviewRequestedAt is null "
            + "and (:paymentStatus is null or t.paymentStatus = :paymentStatus)")
    List<QueueTicket> findCompletedAwaitingReview(@Param("before") Instant before,
                                                  @Param("paymentStatus") PaymentStatus paymentStatus);
}
