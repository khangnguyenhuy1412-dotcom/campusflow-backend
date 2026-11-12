package com.campusflow.notification.service;

import com.campusflow.comment.entity.ItemComment;
import com.campusflow.exchange.entity.ExchangeRequest;
import com.campusflow.exchange.event.ExchangeEvent;
import com.campusflow.item.entity.Item;
import com.campusflow.moderation.entity.Report;
import com.campusflow.moderation.entity.ReportStatus;
import com.campusflow.review.entity.Review;
import com.campusflow.support.entity.SupportTicket;
import com.campusflow.notification.entity.NotificationType;
import com.campusflow.review.entity.Review;
import com.campusflow.user.entity.User;
import com.campusflow.wanted.entity.WantedPost;
import java.util.ArrayList;
import java.util.List;

/**
 * Decides who is notified about an event and writes the (Vietnamese) text.
 * Kept separate from storage so the wording can be changed and tested on its own.
 */
public final class NotificationMessages {

    private static final int MAX_ITEM_TITLE = 60;

    private NotificationMessages() {
    }

    public static NotificationDraft forExchange(ExchangeEvent event) {
        ExchangeRequest request = event.request();
        Item item = request.getItem();
        User owner = request.getOwner();
        User requester = request.getRequester();
        String itemTitle = quote(item.getTitle());

        return switch (event.type()) {
            case REQUESTED -> new NotificationDraft(owner, NotificationType.EXCHANGE_REQUESTED,
                    "Yêu cầu mới cho " + itemTitle,
                    requester.getFullName() + " " + requestVerb(item) + " " + itemTitle + ".",
                    requester, item, request);
            case ACCEPTED -> new NotificationDraft(requester, NotificationType.EXCHANGE_ACCEPTED,
                    "Yêu cầu đã được chấp nhận",
                    owner.getFullName() + " đã chấp nhận yêu cầu cho " + itemTitle
                            + ". Mở yêu cầu để xem thông tin liên hệ và hẹn gặp.",
                    owner, item, request);
            case REJECTED -> new NotificationDraft(requester, NotificationType.EXCHANGE_REJECTED,
                    "Yêu cầu bị từ chối",
                    owner.getFullName() + " đã từ chối yêu cầu của bạn cho " + itemTitle + ".",
                    owner, item, request);
            case AUTO_REJECTED -> new NotificationDraft(requester, NotificationType.EXCHANGE_REJECTED,
                    "Món đồ đã có người nhận",
                    itemTitle + " đã được trao cho người khác. Hãy thử tìm món tương tự nhé!",
                    null, item, request);
            case CANCELLED -> {
                boolean byRequester = request.isRequester(event.actorId());
                User actor = byRequester ? requester : owner;
                User recipient = byRequester ? owner : requester;
                yield new NotificationDraft(recipient, NotificationType.EXCHANGE_CANCELLED,
                        "Yêu cầu đã bị hủy",
                        actor.getFullName() + " đã hủy giao dịch cho " + itemTitle + ".",
                        actor, item, request);
            }
            case AUTO_CANCELLED -> new NotificationDraft(owner, NotificationType.EXCHANGE_CANCELLED,
                    "Yêu cầu đã tự động hủy",
                    "Yêu cầu của " + requester.getFullName() + " cho " + itemTitle
                            + " đã hủy vì món đồ họ đề nghị đổi không còn nữa.",
                    null, item, request);
            case ITEM_REMOVED -> new NotificationDraft(requester, NotificationType.EXCHANGE_CANCELLED,
                    "Yêu cầu đã bị hủy",
                    itemTitle + " không còn trên CampusFlow nên yêu cầu của bạn đã bị hủy.",
                    null, item, request);
            case COMPLETED -> {
                // Either side can complete (owner directly, receiver with the meetup code): tell the other one
                boolean byRequester = request.isRequester(event.actorId());
                User actor = byRequester ? requester : owner;
                User recipient = byRequester ? owner : requester;
                yield new NotificationDraft(recipient, NotificationType.EXCHANGE_COMPLETED,
                        "Giao dịch hoàn tất",
                        "Giao dịch " + itemTitle + " với " + actor.getFullName()
                                + " đã hoàn tất. Hãy để lại đánh giá cho họ!",
                        actor, item, request);
            }
        };
    }

    public static NotificationDraft forReview(Review review) {
        ExchangeRequest request = review.getExchangeRequest();
        return new NotificationDraft(review.getReviewee(), NotificationType.REVIEW_RECEIVED,
                "Bạn nhận được đánh giá " + review.getRating() + "★",
                review.getReviewer().getFullName() + " đã đánh giá bạn " + review.getRating()
                        + "★ sau giao dịch " + quote(request.getItem().getTitle()) + ".",
                review.getReviewer(), request.getItem(), request);
    }

    public static NotificationDraft forReportHandled(Report report) {
        String target = switch (report.getTargetType()) {
            case ITEM -> "tin " + quote(report.getTargetItem().getTitle());
            case REVIEW -> "đánh giá của " + report.getTargetUser().getFullName();
            case COMMENT -> "bình luận của " + report.getTargetUser().getFullName();
            case USER -> "người dùng " + report.getTargetUser().getFullName();
        };
        String message = report.getStatus() == ReportStatus.DISMISSED
                ? "Báo cáo của bạn về " + target + " đã được xem xét và chưa thấy vi phạm quy định."
                : "Cảm ơn bạn! Báo cáo về " + target + " đã được xử lý.";
        return new NotificationDraft(report.getReporter(), NotificationType.REPORT_RESOLVED,
                "Báo cáo đã được xem xét", message, null, report.getTargetItem(), null);
    }

    /** The moderator is not named, to protect them from harassment. */
    public static NotificationDraft forItemRemoved(Item item, String reason) {
        String why = reason == null || reason.isBlank() ? "vi phạm quy định cộng đồng" : reason.strip();
        return new NotificationDraft(item.getOwner(), NotificationType.ITEM_REMOVED,
                "Tin " + quote(item.getTitle()) + " đã bị gỡ",
                "Kiểm duyệt viên đã gỡ tin của bạn. Lý do: " + truncate(why, 300) + ".",
                null, item, null);
    }

    public static NotificationDraft forWantedMatch(WantedPost post, Item item) {
        return new NotificationDraft(post.getUser(), NotificationType.WANTED_MATCH,
                "Có món phù hợp với tin “Cần tìm” của bạn",
                quote(item.getTitle()) + " vừa được đăng, khớp với " + quote(post.getTitle()) + ". Xem ngay kẻo lỡ!",
                item.getOwner(), item, null);
    }

    public static NotificationDraft forReviewReplied(Review review) {
        ExchangeRequest request = review.getExchangeRequest();
        return new NotificationDraft(review.getReviewer(), NotificationType.REVIEW_REPLIED,
                review.getReviewee().getFullName() + " đã trả lời đánh giá của bạn",
                truncate(review.getReply(), 300),
                review.getReviewee(), request.getItem(), request);
    }

    public static NotificationDraft forReviewHidden(Review review, String reason) {
        String why = reason == null || reason.isBlank() ? "vi phạm quy định cộng đồng" : reason.strip();
        return new NotificationDraft(review.getReviewer(), NotificationType.REVIEW_HIDDEN,
                "Đánh giá của bạn đã bị ẩn",
                "Kiểm duyệt viên đã ẩn đánh giá của bạn cho " + review.getReviewee().getFullName()
                        + ". Lý do: " + truncate(why, 300) + ". Bạn có thể liên hệ ban quản trị qua mục Hỗ trợ.",
                null, null, review.getExchangeRequest());
    }

    /** The moderation team wrote in a ticket; the moderator stays anonymous. */
    public static NotificationDraft forSupportReply(SupportTicket ticket, String message) {
        return new NotificationDraft(ticket.getUser(), NotificationType.SUPPORT_REPLY,
                "Ban quản trị đã trả lời: " + truncate(ticket.getSubject(), 120),
                truncate(message, 300), null, null, null, ticket);
    }

    /**
     * The item owner hears about every comment on their listing; the author of the original comment
     * hears about replies to it. Nobody is told twice about the same comment.
     */
    public static List<NotificationDraft> forComment(ItemComment comment) {
        Item item = comment.getItem();
        User author = comment.getAuthor();
        String text = truncate(comment.getContent(), 300);
        List<NotificationDraft> drafts = new ArrayList<>();
        User parentAuthor = comment.getParent() == null ? null : comment.getParent().getAuthor();
        if (parentAuthor != null) {
            drafts.add(new NotificationDraft(parentAuthor, NotificationType.COMMENT_REPLIED,
                    author.getFullName() + " đã trả lời bình luận của bạn", text, author, item, null));
        }
        if (parentAuthor == null || !parentAuthor.getId().equals(item.getOwner().getId())) {
            drafts.add(new NotificationDraft(item.getOwner(), NotificationType.ITEM_COMMENTED,
                    author.getFullName() + " đã bình luận về " + quote(item.getTitle()), text, author, item, null));
        }
        return drafts;
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private static String requestVerb(Item item) {
        return switch (item.getItemType()) {
            case GIVEAWAY -> "muốn nhận";
            case SALE -> "muốn mua";
            case EXCHANGE -> "muốn đổi lấy";
        };
    }

    /** Quotes an item title, shortened so the notification stays within its column limits. */
    static String quote(String title) {
        String shortTitle = title.length() > MAX_ITEM_TITLE ? title.substring(0, MAX_ITEM_TITLE - 1) + "…" : title;
        return "“" + shortTitle + "”";
    }
}
