package co.com.fcv.training.citas.application;

public class PasswordResetFailure extends RuntimeException {
    public PasswordResetFailure() { super("Token de recuperación inválido o expirado"); }
}
