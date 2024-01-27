import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

//Represents a user's mailbox, where his emails are saved. Includes methods that 1) save/load the mailbox's contents from permanent storage, 2) show its contents to the user,
//3) delete an email or 4) allow the user to read an email.
public class Mailbox
{
    private ArrayList<Email> _userMailbox = new ArrayList<Email>();

    public ArrayList<Email> getUserMailbox()
    {
        return _userMailbox;
    }

    public void saveInUserMailbox (Email email)
    {
        _userMailbox.add(email);
    }

    public void loadMailbox(String username)                            //Loads a .txt file containing the user's saved emails from permanent storage.
    {
        try (FileReader fileReader = new FileReader("./" + username + "mailbox.txt");
             Scanner scanner = new Scanner (fileReader);)
        {                        
            String txtFile = "";

            while (scanner.hasNextLine())
            {
                txtFile = txtFile + scanner.nextLine();
            }

            String[] txtSplitByEmail = txtFile.split("%");                     //Splitting the .txt into parts, converting them to Email objects and then populating the Mailbox.

            for (int x = 0; x < txtSplitByEmail.length;x += 1)
            {
                String[] txtSplitBySector = txtSplitByEmail[x].split("&");
                _userMailbox.add (new Email(txtSplitBySector[0], txtSplitBySector[1], txtSplitBySector[2], txtSplitBySector[3]));   //Sender, "&", recipient, "&", subject, "&", main body, "%", next email etc.
            }
        }
        catch (IOException e)
        {
            System.out.println("Error while loading mailbox" + e.getMessage());
        }
    }

    public void saveMailboxToDisk(String username)
    {
        File mailboxFile = new File("./" + username + "mailbox.txt");

        String emailsForSaving = "";

        for (Email email : _userMailbox)
        {
            emailsForSaving = emailsForSaving + email.getSender() +
                              "&" + email.getRecipient() +
                              "&" + email.getSubject() +
                              "&" + email.getMailBody() + "%";
        }

        //"False", to ovewrite all file's contents.
        try (FileWriter fileWriter = new FileWriter(mailboxFile, false);)
        {
            fileWriter.write(emailsForSaving);
            fileWriter.flush();
            System.out.println("Mailbox's contents successfully saved to " + mailboxFile.getPath());
        }
        catch (IOException except)
        {
            System.out.println("Error while saving mailbox to file --> " + except.getMessage());
        }
    }

    public String showMailbox()
    {
        String emailsForShowing = "";
        int counter = 1;

        for (Email email : _userMailbox)
        {
            emailsForShowing =  emailsForShowing + counter + ": " +
                               "FROM: " + email.getSender() + ", " +
                               "TO: " + email.getRecipient() + ", " +
                               "SUBJECT: " + email.getSubject() + "\n";

            counter += 1;
        }
        return emailsForShowing;
    }

    public String deleteEmail (int mailNumber)
    {
        _userMailbox.remove(mailNumber - 1);

        return showMailbox();                       //Shows the updated mailbox again, so that the user knows that the email was really deleted.
    }

    public String readEmail (int mailNumber)
    {
        Email helper = _userMailbox.get(mailNumber - 1);

        String emailToRead = "FROM: " + helper.getSender() + "\n" +
                             "TO: " + helper.getRecipient() + "\n" +
                             "SUBJECT: " + helper.getSubject() + "\n" +
                             "MESSAGE: " + helper.getMailBody();

        return emailToRead;
    }
}
