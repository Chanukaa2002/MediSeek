package com.example.mediseek.service

import android.util.Log
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object GmailSender {
    // Configuration constants - replace with your values
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"
    private const val FROM_EMAIL = "info.mediseek@gmail.com" // Your Gmail address
    private const val FROM_PASSWORD = "wffz jivl wzmm basi " // Use Google App Password

    // Email data class for better organization
    data class Email(
        val to: String,
        val subject: String,
        val body: String,
        val isHtml: Boolean = false
    )

    fun sendEmail(email: Email, callback: ((Boolean, String?) -> Unit)? = null) {
        val props = Properties().apply {
            put("mail.smtp.host", SMTP_HOST)
            put("mail.smtp.port", SMTP_PORT)
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
        }

        val session = Session.getInstance(props,
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD)
                }
            })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(FROM_EMAIL))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(email.to))
                subject = email.subject

                if (email.isHtml) {
                    setContent(email.body, "text/html; charset=utf-8")
                } else {
                    setText(email.body)
                }
            }

            Thread {
                try {
                    Transport.send(message)
                    callback?.invoke(true, "Email sent successfully")
                    Log.d("GmailSender", "Email sent to ${email.to}")
                } catch (e: MessagingException) {
                    callback?.invoke(false, "Failed to send email: ${e.message}")
                    Log.e("GmailSender", "Email sending failed", e)
                }
            }.start()

        } catch (e: Exception) {
            callback?.invoke(false, "Email preparation failed: ${e.message}")
            Log.e("GmailSender", "Email preparation failed", e)
        }
    }

    /**
     * Convenience method for sending a welcome email
     */
    fun sendWelcomeEmail(recipient: String, username: String, callback: ((Boolean, String?) -> Unit)? = null) {
        val email = Email(
            to = recipient,
            subject = "Welcome to Our Service!",
            body = """
                Hi $username,
                
                Thank you for registering with us!
                
                We're excited to have you on board.
                
                Best regards,
                The Team
            """.trimIndent()
        )
        sendEmail(email, callback)
    }

    fun sendHtmlEmail(recipient: String, subject: String, htmlBody: String, callback: ((Boolean, String?) -> Unit)? = null) {
        val email = Email(
            to = recipient,
            subject = subject,
            body = htmlBody,
            isHtml = true
        )
        sendEmail(email, callback)
    }
}