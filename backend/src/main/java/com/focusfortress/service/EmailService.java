package com.focusfortress.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String token) {
        String link = "http://localhost:8080/api/auth/verify?token=" + token;

        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Account Verification</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Arial, sans-serif;
                        background-color: #f7f8fa;
                        margin: 0;
                        padding: 0;
                        text-align: center;
                    }
                    .container {
                        max-width: 480px;
                        margin: 50px auto;
                        background: #fff;
                        border-radius: 16px;
                        box-shadow: 0 20px 60px rgba(255, 107, 53, 0.15);
                        overflow: hidden;
                    }
                    .header {
                        background: linear-gradient(135deg, rgb(255, 140, 66) 0%, rgb(255, 107, 53) 100%);
                        color: #fff;
                        padding: 36px 20px 28px;
                    }
                    .header h1 {
                        font-size: 28px;
                        margin: 0;
                        font-weight: 700;
                    }
                    .quote {
                        font-style: italic;
                        font-size: 14px;
                        margin-top: 8px;
                        color: #fff9f5;
                    }
                    .content {
                        padding: 30px 40px;
                    }
                    .content p {
                        color: #555;
                        font-size: 15px;
                        line-height: 1.6;
                        margin-bottom: 24px;
                    }
                    .button {
                        display: inline-block;
                        background: linear-gradient(135deg, rgb(255, 140, 66) 0%, rgb(255, 107, 53) 100%);
                        color: white;
                        padding: 12px 28px;
                        border-radius: 8px;
                        text-decoration: none;
                        font-weight: 600;
                        font-size: 15px;
                        transition: opacity 0.2s ease-in-out;
                    }
                    .button:hover {
                        opacity: 0.9;
                    }
                    .footer {
                        font-size: 12px;
                        color: #999;
                        padding: 20px 0 25px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>FocusFortress</h1>
                        <p class="quote">"The journey of a thousand miles begins with one step."<br>— Lao Tzu</p>
                    </div>
                    <div class="content">
                        <p>Click the button below to verify your email and activate your account.</p>
                        <a href="%LINK%" class="button">Activate Account</a>
                    </div>
                    <div class="footer">
                        © 2025 FocusFortress. Build your fortress of good habits.
                    </div>
                </div>
            </body>
            </html>
        """.replace("%LINK%", link);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(to);
            helper.setSubject("Verify your FocusFortress account");
            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Error sending email: " + e.getMessage());
        }
    }
}