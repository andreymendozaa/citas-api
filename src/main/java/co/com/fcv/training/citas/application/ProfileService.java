package co.com.fcv.training.citas.application;

import co.com.fcv.training.citas.domain.Account;
import co.com.fcv.training.citas.domain.Identity;
import java.util.Optional;

public class ProfileService {
    private final Ports.Accounts accounts;
    private final Ports.Transactions transactions;
    private final Ports.Affiliations affiliations;

    public ProfileService(Ports.Accounts accounts, Ports.Transactions transactions, Ports.Affiliations affiliations) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.affiliations = affiliations;
    }

    public Account me(Long userId) {
        return accounts.byId(userId).orElseThrow(AuthFailure::new);
    }

    public Account updatePhone(Long userId, String phone) {
        return transactions.run(() -> accounts.updatePhone(userId, Identity.required(phone)));
    }

    /** HU-011: the caller's current affiliation, if any. */
    public Optional<Ports.Affiliation> affiliation(Long userId) {
        return affiliations.current(userId);
    }

    /** HU-011: switch the caller's current plan without duplicating plans in their affiliation history. */
    public Ports.Affiliation changeAffiliation(Long userId, Long planId) {
        if (planId == null) throw new IllegalArgumentException("Plan obligatorio");
        return affiliations.changeCurrent(userId, planId);
    }
}
