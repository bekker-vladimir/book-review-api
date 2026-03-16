package fit.bitjv.bookreview.model.dto;

public record EmailMessageDto(String to,
                              String subject,
                              String body) {
}
