import java.util.ArrayList;

//A class representing a user's email account.
public class Account
{
    private String _username;
    private String _password;
    private String _fullName;
    private ArrayList<Mailbox> _mailingList = new ArrayList<>();

    public Account (String username, String password, String fullName)
    {
        _username = username;
        _password = password;
        _fullName = fullName;
    }

    public String getUsername()
    {
        return _username;
    }

    public void setUsername(String username)
    {
        _username = username;
    }

        public String getPassword()
    {
        return _password;
    }

    public void setPassword(String password)
    {
        _password = password;
    }

        public String getFullname()
    {
        return _fullName;
    }

    public void setFullname(String fullName)
    {
        _fullName = fullName;
    }

    public ArrayList<Mailbox> getMailingList()
    {
        return _mailingList;
    }

    public void setMailingList(ArrayList<Mailbox> mailingList)
    {
        _mailingList = mailingList;
    }
}
