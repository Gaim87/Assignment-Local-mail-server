import java.io.IOException;
import java.util.*;

//A class that represents an email. Contains methods to 1) clear, 2) process or 3) reset all buffers and one (4) that finalizes the mail and prepares it for dispatch.
public class Email
{
    private String _reversePathBuffer;                  //Contains the sender's and any subsequent relaying host's email addresses. (p. 3, 18)
    private ArrayList<String> _forwardPathBuffer;       //Contains the recipient's and any previous relaying host's email addresses. (p.3-4, 13, 18)
    private String _mailDataBuffer;                     //Contains the email's main body content. (p.18, 20)
    private String _sender;
    private ArrayList<String> _recipient = new ArrayList<>();
    private String _mailBody;
    private String _subject;

    public Email(String senderEmail, String recipientEmail, String subject, String mailBody) throws IOException
    {
        _reversePathBuffer = "";
        _forwardPathBuffer = new ArrayList<String>();
        _mailDataBuffer = "";
        _sender = senderEmail;
        _recipient.add(recipientEmail);
        _mailBody = mailBody;
        _subject = subject;
    }

    public Email() throws IOException
    {
        _reversePathBuffer = "";
        _forwardPathBuffer = new ArrayList<>();
        _mailDataBuffer = "";
        _sender = "";
        _recipient = new ArrayList<>();
        _mailBody = "";
        _subject = "";
    }

    public String getSender()
    {
        return _sender;
    }

    public String getRecipient()
    {
        String valueToReturn = _recipient.toString();
        return valueToReturn.substring(1, valueToReturn.length() - 1);              //Used .substring() because toString() automatically adds brackets around the converted value
    }

    public String getSubject()
    {
        return _subject;
    }

    public String getMailBody()
    {
        return _mailBody;
    }

    public void setReversePathBuffer(String senderEmail)
    {
        _reversePathBuffer = senderEmail.trim();
    }

    public void setForwardPathBuffer(String recipient)
    {
        if (!_forwardPathBuffer.contains(recipient))
        {
            if (_forwardPathBuffer.size() >= 1)
                _forwardPathBuffer.add("," + recipient.trim());     //In case of multiple recipients, adds a comma between the email addresses .
            else
                _forwardPathBuffer.add(recipient.trim());
        }
    }

    public void setMailDataBuffer(String mailBody)
    {
        _mailDataBuffer = _mailDataBuffer + " " + mailBody;     //Used inside a loop.
    }

    public void setMailSubject(String mailSubject)
    {
        _subject = mailSubject.trim();
    }

    public void clearEmailBuffers()
    {
        _reversePathBuffer = "";
        _forwardPathBuffer.clear();
        _mailDataBuffer = "";
        _subject = "";
    }

    //Called when the DATA command (mail's main body) ends. The returned value is not used anywhere, because the SEND command was not required for the assignment.
    //After processing the buffers, a timestamp is inserted as mail info (p. 32), and after the mail is sent, a reverse-path is also included. (p. 20-22 and 32)
    public String finalizeEmail(String serverDomainName)
    { 
        Date emailTimeStamp = new Date();
        String clientDomainName = _reversePathBuffer.substring(_reversePathBuffer.indexOf("@") +1);

        return "Return path: " + "<" + _sender + ">" + "\n"                     //As in RFC's p. 22
                + "Received: from " + clientDomainName
                + " by " + serverDomainName
                + " ; " + emailTimeStamp + "\n"
                + "From: " + _sender + "\n"
                + "Subject: " + _subject + "\n"
                + "To: " + _recipient + "\n\n"
                + _mailBody;
    }

    public void processEmailBuffers()
    {
        _sender = _reversePathBuffer;
        _recipient = _forwardPathBuffer;
        _mailBody = _mailDataBuffer;
    }

    public void applyRSETCommand()
    {
        clearEmailBuffers();
        _sender = "";
        _recipient.clear();
        _mailBody = "";
        _subject = "";
    }
}