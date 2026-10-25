package com.haining.birthday.remind

import com.haining.birthday.data.Prefs
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class SmtpException(val code: Int, message: String) : IOException(message) {
    /** 5xx 是账号/配置问题，重试也没用 */
    val permanent get() = code in 500..599
}

data class MailConfig(
    val host: String,
    val port: Int,
    val ssl: Boolean,
    val user: String,
    val pass: String,
    val to: List<String>,
) {
    companion object {
        fun from(p: Prefs): MailConfig {
            val user = p.smtpUser.trim()
            val to = p.mailTo.split(',', '，', ';', '；', ' ').map { it.trim() }.filter { it.isNotEmpty() }
            return MailConfig(p.smtpHost.trim(), p.smtpPort, p.smtpSsl, user, p.smtpPass, to.ifEmpty { listOf(user) })
        }
    }
}

/**
 * 极简 SMTP 客户端（约 150 行，无第三方库）。
 * 只连接你在设置里填写的邮件服务器，全程 TLS 加密并校验服务器证书。
 */
object Smtp {
    private const val TIMEOUT = 20_000

    fun send(cfg: MailConfig, subject: String, body: String) {
        var socket = Socket()
        try {
            socket.connect(InetSocketAddress(cfg.host, cfg.port), TIMEOUT)
            socket.soTimeout = TIMEOUT
            if (cfg.ssl) socket = tls(socket, cfg.host, cfg.port)
            var c = Conn(socket)
            c.expect(220)
            var caps = c.cmd("EHLO localhost", 250)
            if (!cfg.ssl) {
                if (caps.none { it.uppercase().startsWith("STARTTLS") }) {
                    throw SmtpException(530, "服务器不支持 STARTTLS 加密，请改用 SSL（通常是 465 端口）")
                }
                c.cmd("STARTTLS", 220)
                socket = tls(socket, cfg.host, cfg.port)
                c = Conn(socket)
                caps = c.cmd("EHLO localhost", 250)
            }
            val auth = caps.firstOrNull { it.uppercase().startsWith("AUTH") }.orEmpty().uppercase()
            if (auth.contains("LOGIN") || !auth.contains("PLAIN")) {
                c.cmd("AUTH LOGIN", 334)
                c.cmd(b64(cfg.user), 334)
                c.cmd(b64(cfg.pass), 235)
            } else {
                c.cmd("AUTH PLAIN " + b64("\u0000${cfg.user}\u0000${cfg.pass}"), 235)
            }
            c.cmd("MAIL FROM:<${cfg.user}>", 250)
            for (r in cfg.to) c.cmd("RCPT TO:<$r>", 250, 251)
            c.cmd("DATA", 354)
            c.write(message(cfg, subject, body))
            c.expect(250)
            runCatching { c.cmd("QUIT", 221) }
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun tls(plain: Socket, host: String, port: Int): Socket {
        val s = (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(plain, host, port, true) as SSLSocket
        s.soTimeout = TIMEOUT
        s.startHandshake()
        if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(host, s.session)) {
            s.close()
            throw IOException("服务器证书与 $host 不匹配，已中止连接")
        }
        return s
    }

    private fun message(cfg: MailConfig, subject: String, body: String): String = buildString {
        val domain = cfg.user.substringAfter('@', "localhost")
        append("Date: ").append(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now())).append("\r\n")
        append("From: ").append(word("生日提醒")).append(" <").append(cfg.user).append(">\r\n")
        append("To: ").append(cfg.to.joinToString(", ") { "<$it>" }).append("\r\n")
        append("Subject: ").append(word(subject)).append("\r\n")
        append("Message-ID: <").append(UUID.randomUUID()).append('@').append(domain).append(">\r\n")
        append("MIME-Version: 1.0\r\n")
        append("Content-Type: text/plain; charset=UTF-8\r\n")
        append("Content-Transfer-Encoding: base64\r\n\r\n")
        append(Base64.getMimeEncoder(76, "\r\n".toByteArray()).encodeToString(body.toByteArray(Charsets.UTF_8)))
        append("\r\n.\r\n")
    }

    /** RFC 2047 编码中文标题，按字符切成不超过 45 字节的片段 */
    private fun word(s: String): String {
        val parts = ArrayList<String>()
        val cur = StringBuilder()
        var bytes = 0
        var i = 0
        while (i < s.length) {
            val cp = s.codePointAt(i)
            val ch = String(Character.toChars(cp))
            val n = ch.toByteArray(Charsets.UTF_8).size
            if (bytes + n > 45 && cur.isNotEmpty()) {
                parts += cur.toString()
                cur.setLength(0)
                bytes = 0
            }
            cur.append(ch)
            bytes += n
            i += Character.charCount(cp)
        }
        if (cur.isNotEmpty()) parts += cur.toString()
        return parts.joinToString("\r\n ") { "=?UTF-8?B?" + b64(it) + "?=" }
    }

    private fun b64(s: String) = Base64.getEncoder().encodeToString(s.toByteArray(Charsets.UTF_8))

    private class Conn(s: Socket) {
        private val input = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
        private val output: OutputStream = BufferedOutputStream(s.getOutputStream())

        /** 读一条（可能多行的）响应，返回去掉状态码后的各行 */
        fun expect(vararg ok: Int): List<String> {
            val lines = ArrayList<String>()
            while (true) {
                val line = input.readLine() ?: throw IOException("服务器断开了连接")
                lines += line
                if (line.length < 4 || line[3] != '-') break
            }
            val last = lines.last()
            val code = last.take(3).toIntOrNull() ?: throw IOException("无法识别的服务器响应：$last")
            if (code !in ok) throw SmtpException(code, lines.joinToString(" "))
            return lines.map { if (it.length > 4) it.substring(4) else "" }
        }

        fun cmd(line: String, vararg ok: Int): List<String> {
            write(line + "\r\n")
            return expect(*ok)
        }

        fun write(data: String) {
            output.write(data.toByteArray(Charsets.UTF_8))
            output.flush()
        }
    }

    /** 把异常翻译成看得懂的话 */
    fun explain(t: Throwable): String = when {
        t is SmtpException && (t.code == 535 || t.code == 534 || t.code == 530) ->
            "登录失败：请检查邮箱账号和授权码（注意是邮箱设置里生成的“授权码”，不是登录密码）。\n服务器返回：${t.message}"
        t is SmtpException && t.code == 550 -> "收件人地址被拒绝，请检查收件邮箱。\n服务器返回：${t.message}"
        t is SmtpException -> "服务器拒绝：${t.message}"
        t is java.net.UnknownHostException -> "找不到服务器 ${t.message}，请检查 SMTP 地址和网络"
        t is java.net.SocketTimeoutException -> "连接超时，请检查网络、端口和加密方式是否匹配"
        t is java.net.ConnectException -> "连不上服务器，请检查端口（SSL 一般是 465）"
        t is javax.net.ssl.SSLException -> "加密连接失败，请检查端口和加密方式是否匹配（465 用 SSL，587 用 STARTTLS）"
        else -> t.message ?: t.javaClass.simpleName
    }
}
