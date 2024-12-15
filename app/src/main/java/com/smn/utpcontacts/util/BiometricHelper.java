package com.smn.utpcontacts.util;

import android.content.Context;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import java.util.concurrent.Executor;

public class BiometricHelper {
    private final Context context;
    private final FragmentActivity activity;
    private final BiometricAuthListener listener;

    public interface BiometricAuthListener {
        void onBiometricAuthenticationSuccess();
        void onBiometricAuthenticationError(int errorCode, String errorMessage);
    }

    public BiometricHelper(FragmentActivity activity, BiometricAuthListener listener) {
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.listener = listener;
    }

    public boolean isBiometricAvailable() {
        BiometricManager biometricManager = BiometricManager.from(context);
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(context);

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        listener.onBiometricAuthenticationSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        listener.onBiometricAuthenticationError(errorCode, errString.toString());
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación requerida")
                .setSubtitle("Use su huella digital para ver los contactos privados")
                .setNegativeButtonText("Cancelar")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}