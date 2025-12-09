package com.focusfortress.service;

import com.focusfortress.dto.UserReportDTO;
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

    public void sendProgressReport(String to, UserReportDTO report) {
        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Your FocusFortress Progress Report</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Arial, sans-serif;
                        background-color: #f7f8fa;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 30px auto;
                        background: #fff;
                        border-radius: 16px;
                        box-shadow: 0 20px 60px rgba(255, 107, 53, 0.15);
                        overflow: hidden;
                    }
                    .header {
                        background: linear-gradient(135deg, rgb(255, 140, 66) 0%, rgb(255, 107, 53) 100%);
                        color: #fff;
                        padding: 36px 30px 28px;
                        text-align: center;
                    }
                    .header h1 {
                        font-size: 32px;
                        margin: 0;
                        font-weight: 700;
                    }
                    .header p {
                        font-size: 14px;
                        margin: 8px 0 0;
                        opacity: 0.95;
                    }
                    .content {
                        padding: 30px;
                    }
                    .section {
                        margin-bottom: 28px;
                        padding: 20px;
                        background: #f8f9fa;
                        border-radius: 12px;
                        border-left: 4px solid #ff6b35;
                    }
                    .section h2 {
                        font-size: 18px;
                        color: #ff6b35;
                        margin: 0 0 12px 0;
                        font-weight: 600;
                    }
                    .stat {
                        display: flex;
                        justify-content: space-between;
                        padding: 10px 0;
                        border-bottom: 1px solid #e0e0e0;
                    }
                    .stat:last-child {
                        border-bottom: none;
                    }
                    .stat-label {
                        color: #666;
                        font-size: 14px;
                    }
                    .stat-value {
                        color: #333;
                        font-weight: 600;
                        font-size: 16px;
                    }
                    .interest-item {
                        display: inline-block;
                        padding: 8px 14px;
                        margin: 4px;
                        background: linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%);
                        color: white;
                        border-radius: 20px;
                        font-size: 13px;
                        font-weight: 500;
                    }
                    .footer {
                        text-align: center;
                        font-size: 12px;
                        color: #999;
                        padding: 20px;
                        background: #f8f9fa;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📊 Your Progress Report</h1>
                        <p>Generated on %GENERATED_AT%</p>
                    </div>
                    <div class="content">
                        <p style="font-size: 16px; color: #555; margin-bottom: 24px;">
                            Hello <strong>%USER_NAME%</strong>! Here's your FocusFortress progress summary.
                        </p>
            """);

        html.append(generateReportSections(report));

        html.append("""
                        <div style="text-align: center; margin-top: 32px; padding-top: 24px; border-top: 2px solid #e0e0e0;">
                            <p style="color: #666; font-size: 14px; margin-bottom: 16px;">
                                Keep building your fortress of good habits! 🏰
                            </p>
                            <a href="http://localhost:3000/dashboard" 
                               style="display: inline-block;
                                      background: linear-gradient(135deg, rgb(255, 140, 66) 0%, rgb(255, 107, 53) 100%);
                                      color: white;
                                      padding: 12px 28px;
                                      border-radius: 8px;
                                      text-decoration: none;
                                      font-weight: 600;
                                      font-size: 15px;">
                                View Dashboard
                            </a>
                        </div>
                    </div>
                    <div class="footer">
                        © 2025 FocusFortress. Build your fortress of good habits.
                    </div>
                </div>
            </body>
            </html>
            """);

        String finalHtml = html.toString()
                .replace("%USER_NAME%", report.getUserName())
                .replace("%GENERATED_AT%", report.getGeneratedAt());

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(to);
            helper.setSubject("📊 Your FocusFortress Progress Report");
            helper.setText(finalHtml, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Error sending progress report email: " + e.getMessage());
        }
    }

    private String generateReportSections(UserReportDTO report) {
        StringBuilder sections = new StringBuilder();

        // Habits section
        if (report.getActiveHabitsCount() != null || report.getCompletedHabitsCount() != null) {
            sections.append("<div class=\"section\">");
            sections.append("<h2>🎯 Habits</h2>");
            if (report.getActiveHabitsCount() != null) {
                sections.append("<div class=\"stat\">");
                sections.append("<span class=\"stat-label\">Active Habits</span>");
                sections.append("<span class=\"stat-value\">").append(report.getActiveHabitsCount()).append("</span>");
                sections.append("</div>");
            }
            if (report.getCompletedHabitsCount() != null) {
                sections.append("<div class=\"stat\">");
                sections.append("<span class=\"stat-label\">Completed Habits</span>");
                sections.append("<span class=\"stat-value\">").append(report.getCompletedHabitsCount()).append("</span>");
                sections.append("</div>");
            }
            sections.append("</div>");
        }

        // Interests section
        if (report.getInterests() != null && !report.getInterests().isEmpty()) {
            sections.append("<div class=\"section\">");
            sections.append("<h2>⭐ Your Interests</h2>");
            sections.append("<div style=\"margin-top: 12px;\">");
            for (UserReportDTO.InterestInfo interest : report.getInterests()) {
                sections.append("<span class=\"interest-item\">")
                        .append(interest.getEmoji())
                        .append(" ")
                        .append(interest.getName())
                        .append("</span>");
            }
            sections.append("</div>");
            sections.append("</div>");
        }

        // Media Library section
        if (report.getMediaLibraryStats() != null) {
            UserReportDTO.MediaLibraryStats media = report.getMediaLibraryStats();
            sections.append("<div class=\"section\">");
            sections.append("<h2>🎬 Media Library</h2>");
            sections.append("<div class=\"stat\">");
            sections.append("<span class=\"stat-label\">Watch Later</span>");
            sections.append("<span class=\"stat-value\">").append(media.getWatchLater()).append("</span>");
            sections.append("</div>");
            sections.append("<div class=\"stat\">");
            sections.append("<span class=\"stat-label\">Currently Watching</span>");
            sections.append("<span class=\"stat-value\">").append(media.getCurrentlyWatching()).append("</span>");
            sections.append("</div>");
            sections.append("<div class=\"stat\">");
            sections.append("<span class=\"stat-label\">Finished</span>");
            sections.append("<span class=\"stat-value\">").append(media.getFinished()).append("</span>");
            sections.append("</div>");
            sections.append("<div class=\"stat\">");
            sections.append("<span class=\"stat-label\"><strong>Total Videos</strong></span>");
            sections.append("<span class=\"stat-value\"><strong>").append(media.getTotal()).append("</strong></span>");
            sections.append("</div>");
            sections.append("</div>");
        }

        // Counters section
        if (report.getCountersStats() != null) {
            UserReportDTO.CountersStats counters = report.getCountersStats();
            sections.append("<div class=\"section\">");
            sections.append("<h2>🔢 Counters</h2>");
            sections.append("<div class=\"stat\">");
            sections.append("<span class=\"stat-label\">Total Counters</span>");
            sections.append("<span class=\"stat-value\">").append(counters.getTotalCounters()).append("</span>");
            sections.append("</div>");
            sections.append("<div class=\"stat\">");
            sections.append("<span class=\"stat-label\">Longest Streak</span>");
            sections.append("<span class=\"stat-value\">").append(counters.getLongestStreak()).append(" days</span>");
            sections.append("</div>");
            if (counters.getLongestStreakCounterName() != null && !counters.getLongestStreakCounterName().equals("N/A")) {
                sections.append("<div class=\"stat\">");
                sections.append("<span class=\"stat-label\">Best Counter</span>");
                sections.append("<span class=\"stat-value\">").append(counters.getLongestStreakCounterName()).append("</span>");
                sections.append("</div>");
            }
            sections.append("</div>");
        }

        return sections.toString();
    }
}