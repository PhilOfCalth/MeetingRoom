package phil.tools.meetingroom

import android.content.Context
import android.os.AsyncTask
import java.net.URL
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.xml.parsers.SAXParserFactory


//class RetrieveFeedTask(): AsyncTask<String, Void, Void>() {
//
//    private lateinit var emailClient:GMailSender
//
//    override fun onPreExecute() {
//        super.onPreExecute()
//        emailClient = GMailSender(this)
//    }
//
//    protected fun doInBackground( varargs: SafeVarargs) {
//        try {
//            val url = URL(urls[0]);
//            val factory = SAXParserFactory.newInstance();
//            val parser = factory.newSAXParser();
//            val xmlreader = parser.getXMLReader();
//            val theRSSHandler = RssHandler();
//            xmlreader.setContentHandler(theRSSHandler);
//            val is = InputSource(url.openStream());
//            xmlreader.parse(is);
//
//            return theRSSHandler.getFeed();
//        } catch (e:Exception) {
//            this.exception = e;
//        } finally {
//            is.close()
//        }
//    }

class GMailSender(context: Context): Authenticator() {

    private val mailhost = "smtp.gmail.com"
    private val password:String
    private var session: Session

    init {
        val config = context.resources.openRawResource(R.raw.config)
        val configProperties = Properties()
        configProperties.load(config)
        password = configProperties.getProperty("email_password")

        val emailProps = Properties()
        emailProps.setProperty("mail.transport.protocol", "smtp")
        emailProps.setProperty("mail.host", mailhost)
        emailProps["mail.smtp.auth"] = "true"
        emailProps["mail.smtp.port"] = "465"
        emailProps["mail.smtp.socketFactory.port"] = "465"
        emailProps["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        emailProps["mail.smtp.socketFactory.fallback"] = "false"
        emailProps.setProperty("mail.smtp.quitwait", "false")

        session = Session.getDefaultInstance(emailProps, this)
    }

    override fun getPasswordAuthentication(): PasswordAuthentication {
        return PasswordAuthentication("PhilipLarkin8383@gmail.com", password)
    }

    @Synchronized fun sendEmail(subject:String, body:String, recipient:String?){

        val message = MimeMessage(session)
        message.sender = InternetAddress("PhilipLarkin8383@gmail.com")
        message.subject = subject
        message.setText(body)

        var sendTo:String? = recipient
        if(null == sendTo){
            sendTo = "philipmahem@gmail.com"
        }

        message.setRecipient(Message.RecipientType.TO, InternetAddress(sendTo))
        val thread = Thread{
            Transport.send(message)
        }
        thread.start()
    }
}

