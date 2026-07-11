package davi.spf.supportflow.comment.mapper;

import davi.spf.supportflow.comment.dto.TicketCommentResponseDTO;
import davi.spf.supportflow.comment.entity.TicketComment;
import org.springframework.stereotype.Component;

@Component
public class TicketCommentMapper {

    public TicketCommentResponseDTO toResponse(TicketComment comment) {
        return new TicketCommentResponseDTO(
                comment.getId(),
                comment.getTicket().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                comment.getAuthor().getRole().name(),
                comment.getMessage(),
                comment.getCreatedAt()
        );
    }
}
