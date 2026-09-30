package co.com.fcv.training.citas.application;

import co.com.fcv.training.citas.domain.Account;
import co.com.fcv.training.citas.domain.Identity;

public class ProfileService {
    private final Ports.Accounts accounts;
    private final Ports.Transactions transactions;

    public ProfileService(Ports.Accounts accounts, Ports.Transactions transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    public Account me(Long userId) {
        return accounts.byId(userId).orElseThrow(AuthFailure::new);
    }

    public Account updatePhone(Long userId, String phone) {
        return transactions.run(() -> accounts.updatePhone(userId, Identity.required(phone)));
    }
}
