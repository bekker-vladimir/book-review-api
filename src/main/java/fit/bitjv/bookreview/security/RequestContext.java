package fit.bitjv.bookreview.security;

import fit.bitjv.bookreview.model.entity.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Getter
@Setter
@Component
@RequestScope
public class RequestContext {
    private String username;
    private User currentUser;
}
