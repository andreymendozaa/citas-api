package co.com.fcv.training.citas.adapter.web;

import co.com.fcv.training.citas.application.ProfileService;
import co.com.fcv.training.citas.domain.Account;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
class UserController {
    record UserResponse(Long id, String firstName, String lastName, String documentType,
                        String documentNumber, String email, String phone, Set<String> roles) {}

    private final ProfileService profile;
    UserController(ProfileService profile) { this.profile = profile; }

    @GetMapping("/me")
    UserResponse me(Authentication a) { return toResponse(profile.me(userId(a))); }

    @PatchMapping("/me")
    UserResponse patch(Authentication a, @RequestBody Map<String, Object> body) {
        if (!Set.of("phone").equals(body.keySet())) throw new IllegalArgumentException("Solo se permite actualizar el teléfono");
        Object phone = body.get("phone");
        if (!(phone instanceof String s) || s.isBlank() || s.length() > 40) throw new IllegalArgumentException("Teléfono inválido");
        return toResponse(profile.updatePhone(userId(a), s));
    }

    private UserResponse toResponse(Account account) {
        return new UserResponse(account.id(), account.firstName(), account.lastName(), account.documentType(),
                account.documentNumber(), account.email(), account.phone(), account.roles());
    }

    private Long userId(Authentication a) { try { return Long.valueOf(a.getName()); } catch (Exception e) { throw new IllegalStateException("Principal inválido"); } }
}
