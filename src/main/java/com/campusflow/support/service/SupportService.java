package com.campusflow.support.service;

import com.campusflow.common.dto.PageQuery;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.exception.ApiException;
import com.campusflow.moderation.entity.Report;
import com.campusflow.moderation.repository.ReportRepository;
import com.campusflow.support.dto.SupportMessageResponse;
import com.campusflow.support.dto.TicketResponse;
import com.campusflow.support.entity.SupportMessage;
import com.campusflow.support.entity.SupportTicket;
import com.campusflow.support.entity.TicketStatus;
import com.campusflow.support.event.SupportMessageSentEvent;
import com.campusflow.support.repository.SupportMessageRepository;
import com.campusflow.support.repository.SupportTicketRepository;
import com.campusflow.support.repository.TicketUnreadCount;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tickets between a user and the moderation team. Users only ever see their own tickets (others
 * are 404); staff endpoints are protected by role in SecurityConfig.
 */
@Service
@RequiredArgsConstructor
public class SupportService {

    static final int MAX_OPEN_TICKETS_PER_USER = 3;
    private static final int DEFAULT_MESSAGES = 30;
    private static final int MAX_MESSAGES = 50;

    private final SupportTicketRepository ticketRepository;
    private final SupportMessageRepository messageRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    // ------------------------------------------------------------------ user side

    @Transactional
    public TicketResponse openByUser(Long userId, String subject, String message, Long relatedReportId) {
        if (ticketRepository.countByUserIdAndStatus(userId, TicketStatus.OPEN) >= MAX_OPEN_TICKETS_PER_USER) {
            throw ApiException.conflict("You can have at most " + MAX_OPEN_TICKETS_PER_USER
                    + " open tickets; close one or add to an existing ticket");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        // Users may only link reports they filed or that are about them
        Report report = relatedReportId == null ? null : reportRepository.findWithDetailsById(relatedReportId)
                .filter(found -> found.getReporter().getId().equals(userId) || found.getTargetUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Report not found"));
        SupportTicket ticket = ticketRepository.save(SupportTicket.builder()
                .user(user).subject(subject.strip()).relatedReport(report).build());
        addMessage(ticket, user, false, message);
        return TicketResponse.from(ticket, 0);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> listMine(Long userId, Integer page, Integer size) {
        Page<SupportTicket> tickets = ticketRepository.findForUser(userId, PageQuery.of(page, size, Sort.unsorted()));
        return withUnread(tickets, ticketRepository::countUnreadForUser);
    }

    @Transactional(readOnly = true)
    public TicketResponse getMine(Long userId, Long ticketId) {
        SupportTicket ticket = loadOwn(userId, ticketId);
        return TicketResponse.from(ticket, unread(ticket, ticketRepository::countUnreadForUser));
    }

    @Transactional(readOnly = true)
    public List<SupportMessageResponse> messagesForUser(Long userId, Long ticketId, Long beforeId, Integer size) {
        loadOwn(userId, ticketId);
        return page(ticketId, beforeId, size).stream().map(SupportMessageResponse::forUser).toList();
    }

    @Transactional
    public SupportMessageResponse sendAsUser(Long userId, Long ticketId, String content) {
        SupportTicket ticket = loadOwn(userId, ticketId);
        requireOpen(ticket);
        return SupportMessageResponse.forUser(addMessage(ticket, ticket.getUser(), false, content));
    }

    @Transactional
    public void markReadByUser(Long userId, Long ticketId) {
        SupportTicket ticket = loadOwn(userId, ticketId);
        messageRepository.findLatestId(ticketId).ifPresent(id -> ticket.markReadUpTo(false, id));
    }

    @Transactional
    public TicketResponse closeByUser(Long userId, Long ticketId) {
        return TicketResponse.from(close(loadOwn(userId, ticketId)), 0);
    }

    // ------------------------------------------------------------------ staff side

    /** Staff contact a user first, e.g. to hear their side before deciding on a disputed review. */
    @Transactional
    public TicketResponse openByStaff(Long staffId, Long userId, String subject, String message, Long relatedReportId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        Report report = relatedReportId == null ? null : reportRepository.findById(relatedReportId)
                .orElseThrow(() -> ApiException.notFound("Report not found"));
        SupportTicket ticket = ticketRepository.save(SupportTicket.builder()
                .user(user).subject(subject.strip()).openedByStaff(true).relatedReport(report).build());
        addMessage(ticket, userRepository.getReferenceById(staffId), true, message);
        return TicketResponse.from(ticket, 0);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> queue(TicketStatus status, Integer page, Integer size) {
        Page<SupportTicket> tickets = ticketRepository.findQueue(status, PageQuery.of(page, size, Sort.unsorted()));
        return withUnread(tickets, ticketRepository::countUnreadForStaff);
    }

    @Transactional(readOnly = true)
    public TicketResponse getForStaff(Long ticketId) {
        SupportTicket ticket = load(ticketId);
        return TicketResponse.from(ticket, unread(ticket, ticketRepository::countUnreadForStaff));
    }

    @Transactional(readOnly = true)
    public List<SupportMessageResponse> messagesForStaff(Long ticketId, Long beforeId, Integer size) {
        load(ticketId);
        return page(ticketId, beforeId, size).stream().map(SupportMessageResponse::forStaff).toList();
    }

    @Transactional
    public SupportMessageResponse sendAsStaff(Long staffId, Long ticketId, String content) {
        SupportTicket ticket = load(ticketId);
        requireOpen(ticket);
        return SupportMessageResponse.forStaff(addMessage(ticket, userRepository.getReferenceById(staffId), true, content));
    }

    @Transactional
    public void markReadByStaff(Long ticketId) {
        SupportTicket ticket = load(ticketId);
        messageRepository.findLatestId(ticketId).ifPresent(id -> ticket.markReadUpTo(true, id));
    }

    @Transactional
    public TicketResponse closeByStaff(Long ticketId) {
        return TicketResponse.from(close(load(ticketId)), 0);
    }

    // ------------------------------------------------------------------ shared

    private SupportMessage addMessage(SupportTicket ticket, User sender, boolean fromStaff, String content) {
        String text = content.strip();
        if (text.isEmpty()) {
            throw ApiException.badRequest("Message must not be empty");
        }
        SupportMessage message = messageRepository.save(SupportMessage.builder()
                .ticket(ticket).sender(sender).fromStaff(fromStaff).content(text).build());
        ticket.setLastMessageAt(Instant.now());
        // Your own message is never unread for your side
        ticket.markReadUpTo(fromStaff, message.getId());
        events.publishEvent(new SupportMessageSentEvent(ticket,
                SupportMessageResponse.forUser(message), SupportMessageResponse.forStaff(message)));
        return message;
    }

    private SupportTicket close(SupportTicket ticket) {
        if (ticket.isOpen()) {
            ticket.setStatus(TicketStatus.CLOSED);
            ticket.setClosedAt(Instant.now());
        }
        return ticket;
    }

    private List<SupportMessage> page(Long ticketId, Long beforeId, Integer size) {
        int limit = size == null ? DEFAULT_MESSAGES : Math.clamp(size, 1, MAX_MESSAGES);
        return messageRepository.findPage(ticketId, beforeId, PageRequest.of(0, limit));
    }

    private static void requireOpen(SupportTicket ticket) {
        if (!ticket.isOpen()) {
            throw ApiException.conflict("This ticket is closed; open a new one if you need more help");
        }
    }

    private SupportTicket load(Long ticketId) {
        return ticketRepository.findWithDetailsById(ticketId)
                .orElseThrow(() -> ApiException.notFound("Ticket not found"));
    }

    /** Another user's ticket behaves as if it does not exist. */
    private SupportTicket loadOwn(Long userId, Long ticketId) {
        return ticketRepository.findWithDetailsById(ticketId)
                .filter(ticket -> ticket.belongsTo(userId))
                .orElseThrow(() -> ApiException.notFound("Ticket not found"));
    }

    private static long unread(SupportTicket ticket, Function<Collection<Long>, List<TicketUnreadCount>> counter) {
        return counter.apply(List.of(ticket.getId())).stream().mapToLong(TicketUnreadCount::count).sum();
    }

    private static PageResponse<TicketResponse> withUnread(Page<SupportTicket> tickets,
            Function<Collection<Long>, List<TicketUnreadCount>> counter) {
        List<Long> ids = tickets.getContent().stream().map(SupportTicket::getId).toList();
        Map<Long, Long> unread = ids.isEmpty() ? Map.of() : counter.apply(ids).stream()
                .collect(Collectors.toMap(TicketUnreadCount::ticketId, TicketUnreadCount::count));
        return PageResponse.of(tickets, ticket -> TicketResponse.from(ticket, unread.getOrDefault(ticket.getId(), 0L)));
    }
}
