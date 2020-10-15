package phil.tools.meetingroom

import java.security.Security
import java.util.*
import javax.activation.DataHandler
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.util.ByteArrayDataSource

class GMailSender(user:String, password:String): Authenticator() {

    private val mailhost = "smtp.gmail.com"
    private val user:String = user
    private val password:String = password
    private var session: Session

    init {
        val props = Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", mailhost);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
            "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.quitwait", "false");

        session = Session.getDefaultInstance(props, this);
    }

    protected override fun getPasswordAuthentication(): PasswordAuthentication {
        return PasswordAuthentication(user, password)
    }

    @Synchronized fun sendEmail(subject:String, body:String, recipient:String){

        val message = MimeMessage(session);
        val handler = DataHandler( ByteArrayDataSource(body.byteInputStream(), "text/plain"));
        message.sender = InternetAddress("MeetingRoomFeedback@gmail.com");
        message.subject = subject;
        message.dataHandler = handler;

        message.setRecipient(Message.RecipientType.TO, InternetAddress(recipient));
        Transport.send(message);
    }
}

