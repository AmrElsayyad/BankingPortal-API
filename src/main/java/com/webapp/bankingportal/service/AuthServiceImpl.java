package com.webapp.bankingportal.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webapp.bankingportal.entity.PasswordResetToken;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.repository.PasswordResetTokenRepository;

import jakarta.transaction.Transactional;

@Service

public class AuthServiceImpl implements AuthService {

    private static final int EXPIRATION_HOURS = 24;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public String generatePasswordResetToken(User user) {
        // Check if there's an existing token for the user that hasn't expired
        PasswordResetToken existingToken = passwordResetTokenRepository.findByUser(user);

        if (existingToken != null) {
            // Check if the existing token has more than 5 minutes left before expiration
            if (existingToken.getExpiryDateTime().isAfter(LocalDateTime.now().plusMinutes(5))) {
                return existingToken.getToken();
            } else {
                // If the token is about to expire in less than 5 minutes, delete the existing
                // token
                passwordResetTokenRepository.delete(existingToken);
            }
        }

        // Generate a new token
        String token = UUID.randomUUID().toString();

        // Calculate token expiry date/time
        LocalDateTime expiryDateTime = LocalDateTime.now().plusHours(EXPIRATION_HOURS);

        // Save the new token in the database
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDateTime);
        passwordResetTokenRepository.save(resetToken);

        return token;
    }

    @Override
    @Transactional
    public boolean verifyPasswordResetToken(String token, User user) {
        return passwordResetTokenRepository.findByToken(token)
                .map(resetToken -> {
                    boolean isTokenValid = resetToken.getExpiryDateTime().isAfter(LocalDateTime.now());
                    boolean isUserMatch = user.equals(resetToken.getUser());
                    deletePasswordResetToken(token);
                    return isTokenValid && isUserMatch;
                })
                .orElse(false);
    }

    @Override
    public void deletePasswordResetToken(String token) {
        // Delete the token from the database
        passwordResetTokenRepository.deleteByToken(token);
    }
}
