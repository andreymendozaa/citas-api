package co.com.fcv.training.citas.adapter.web;

import co.com.fcv.training.citas.adapter.mailbox.LocalPasswordResetMailbox;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Development-only lookup of password recovery tokens; never available outside profile "local". */
@RestController
@RequestMapping("/api/v1/admin/local-mailbox")
@Profile("local")
class LocalMailboxController {
    private final LocalPasswordResetMailbox mailbox;
    LocalMailboxController(LocalPasswordResetMailbox mailbox) { this.mailbox = mailbox; }

    @GetMapping("/password-resets")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<?> passwordResets(@RequestParam(required = false) String email) {
        if (email != null) {
            return mailbox.find(email)
                    .<ResponseEntity<?>>map(e -> ResponseEntity.ok(Map.of(
                            "token", e.token(), "issuedAt", e.issuedAt(), "expiresAt", e.expiresAt())))
                    .orElse(ResponseEntity.notFound().build());
        }
        return ResponseEntity.ok(mailbox.all());
    }
}
