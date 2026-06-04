package com.ecommerce.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@shopflow.com}")
    private String from;

    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    @Async
    public void sendOrderConfirmation(String to, Long orderId, String total, String customerName, String address) {
        String html = baseTemplate(
                "Siparişiniz Alındı",
                "Merhaba " + escape(customerName) + ",",
                "Siparişiniz başarıyla oluşturuldu. En kısa sürede kargoya verilecektir.",
                detailRow("Sipariş No", "#" + orderId)
                        + detailRow("Toplam", total + " TL")
                        + detailRow("Teslimat", escape(address))
        );
        sendHtml(to, "ShopFlow — Sipariş Onayı #" + orderId, html);
    }

    @Async
    public void sendOrderShipped(String to, Long orderId, String trackingNumber) {
        String html = baseTemplate(
                "Siparişiniz Kargoda",
                "Merhaba!",
                "Siparişiniz kargoya verildi. Takip numaranızla gönderinizi izleyebilirsiniz.",
                detailRow("Sipariş No", "#" + orderId)
                        + detailRow("Takip No", escape(trackingNumber != null ? trackingNumber : "—"))
        );
        sendHtml(to, "ShopFlow — Kargoya Verildi #" + orderId, html);
    }

    @Async
    public void sendOrderDelivered(String to, Long orderId) {
        String html = baseTemplate(
                "Siparişiniz Teslim Edildi",
                "Teşekkürler!",
                "Siparişiniz teslim edildi. ShopFlow'u tercih ettiğiniz için teşekkür ederiz.",
                detailRow("Sipariş No", "#" + orderId)
        );
        sendHtml(to, "ShopFlow — Teslim Edildi #" + orderId, html);
    }

    @Async
    public void sendPaymentSuccess(String to, Long orderId) {
        String html = baseTemplate(
                "Ödeme Başarılı",
                "Harika haber!",
                "Siparişiniz için ödemeniz alındı. Siparişiniz hazırlanıyor.",
                detailRow("Sipariş No", "#" + orderId)
        );
        sendHtml(to, "ShopFlow — Ödeme Onayı #" + orderId, html);
    }

    @Async
    public void sendPasswordReset(String to, String customerName, String resetLink) {
        String html = baseTemplate(
                "Şifre Sıfırlama",
                "Merhaba " + escape(customerName) + ",",
                "Şifrenizi sıfırlamak için aşağıdaki bağlantıya tıklayın. Bağlantı 1 saat geçerlidir.",
                detailRow("Bağlantı", "<a href=\"" + escape(resetLink) + "\" style=\"color:#f27a1a\">Şifremi sıfırla</a>")
        );
        sendHtml(to, "ShopFlow — Şifre Sıfırlama", html);
    }

    @Async
    public void sendLowStockAlert(String productName, int stock) {
        String html = baseTemplate(
                "Düşük Stok Uyarısı",
                "Admin bildirimi",
                "Aşağıdaki ürünün stoğu kritik seviyeye düştü.",
                detailRow("Ürün", escape(productName))
                        + detailRow("Kalan Stok", String.valueOf(stock))
        );
        sendHtml(from, "ShopFlow — Düşük Stok: " + productName, html);
    }

    private String baseTemplate(String title, String greeting, String message, String detailsHtml) {
        return """
                <!DOCTYPE html>
                <html lang="tr">
                <head><meta charset="UTF-8"><title>%s</title></head>
                <body style="margin:0;padding:0;background:#f5f6f8;font-family:Inter,Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f6f8;padding:32px 16px;">
                <tr><td align="center">
                <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                <tr><td style="background:linear-gradient(135deg,#f27a1a,#e06810);padding:24px 28px;">
                <h1 style="margin:0;color:#fff;font-size:22px;">ShopFlow</h1>
                <p style="margin:6px 0 0;color:rgba(255,255,255,0.9);font-size:14px;">%s</p>
                </td></tr>
                <tr><td style="padding:28px;">
                <p style="margin:0 0 12px;color:#1a1d26;font-size:15px;font-weight:600;">%s</p>
                <p style="margin:0 0 20px;color:#5c6370;font-size:14px;line-height:1.6;">%s</p>
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f8f9fb;border-radius:8px;padding:4px 0;">
                %s
                </table>
                <p style="margin:24px 0 0;color:#8b939f;font-size:12px;">Bu e-posta ShopFlow E-Commerce tarafından gönderilmiştir.</p>
                </td></tr>
                </table>
                </td></tr>
                </table>
                </body></html>
                """.formatted(title, title, greeting, message, detailsHtml);
    }

    private String detailRow(String label, String value) {
        return """
                <tr>
                <td style="padding:10px 16px;font-size:13px;color:#8b939f;width:120px;">%s</td>
                <td style="padding:10px 16px;font-size:13px;color:#1a1d26;font-weight:600;">%s</td>
                </tr>
                """.formatted(label, value);
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void sendHtml(String to, String subject, String html) {
        if (!enabled) {
            log.info("Mail disabled — {} to {}", subject, to);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Mail gönderilemedi: {} — {}", subject, e.getMessage());
        }
    }
}
