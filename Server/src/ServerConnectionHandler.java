import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

//A Simple Mail Transfer Protocol server program, created in accordance with RFC 821 (https://datatracker.ietf.org/doc/html/rfc821).
//All page (p.) references can be found in said RFC.

//This class manages all server-side SMTP procedures.
//All sessions must start with a HELO message. To send a mail, you have to send MAIL FROM, RCPT TO and DATA in this specific order.
//RSET is only accepted during a mail sending transaction.
//The only command that is accepted without having first sent HELO is HELP.
//READ MAIL and DELETE MAIL cannot be selected, if a mail transaction is in progress.
public class ServerConnectionHandler implements Runnable
{
    public static String CRLF = "\r\n";
    public static String LF = "\n";
    public static String EC = " ";
    public static String ServerDomainName = "ServerDomain.gr";
    private static File logFile = new File("./Systemlog.txt");
    private static FileWriter fileWriter;
    private static Email email;
    private static AtomicBoolean clientHasSentMAILFROM = new AtomicBoolean(false);
    private static AtomicBoolean clientHasSentRCPTTO = new AtomicBoolean(false);
    private Mailbox mailbox = new Mailbox();
    private Account currentAccount = new Account ("currentUser", "", "");
    private static AtomicBoolean clientHasSaidHELO = new AtomicBoolean(false);

    static      //Static blocks are the first piece of code that gets executed, when instantiating an object of the class they are in.
    {
        try
        {
            fileWriter = new FileWriter(logFile, true);     //Appends new data at the file's end.
            email = new Email();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    socketManager _clientSocketManager = null;
    ArrayList<socketManager> _active_clients = null;

    public ServerConnectionHandler (ArrayList<socketManager> inArrayListVar, socketManager inSocMngVar)
    {
        _clientSocketManager = inSocMngVar;
        _active_clients = inArrayListVar;
    }

    public void run()
    {
        try
        {
            System.out.println("Client " + currentAccount.getUsername() + " connected");
            fileWriter.write("\n\nNEW SESSION ----------------- \nClient " + currentAccount.getUsername() + " connected \n");
            fileWriter.flush();
            System.out.println("Active clients: " + _active_clients.size());
            _clientSocketManager._output.writeUTF("220" + EC + ServerDomainName + " Simple Mail Transfer Service Ready" + CRLF);     //As in p.36 (partially implemented).
            mailbox.loadMailbox(currentAccount.getUsername());

            while (!_clientSocketManager._clientSoc.isClosed())                                          //While the communication socket is not closed.
            {
                String clientMSG = _clientSocketManager._input.readUTF();

                while (!clientHasSaidHELO.get())             //Ensures that the session starts with HELO but also allows execution of the NOOP, HELP and QUIT commands,
                {                                            //if HELO has not yet been selected. (p. 26)
                    if (!clientMSG.contains("HELO"))
                    {
                        if (clientMSG.contains("HELP"))
                        {
                            Server_SMTP_Handler(_clientSocketManager, clientMSG);
                        }
                        else if (clientMSG.contains("QUIT"))                                             //Ends current session and removes the client from the clients list.
                        {
                            System.out.println("Client " + currentAccount.getUsername() + " ending session");

                            _clientSocketManager._output.writeUTF("221" + LF + ServerDomainName + LF + " Service closing transmission channel" + CRLF);      //p. 12
                            _active_clients.remove(_clientSocketManager);
                            System.out.print("Active clients: "+_active_clients.size());

                            fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + "QUIT");        //Every error reply and command received from the client is
                            fileWriter.flush();                                                                     //catalogued in the server's log.
                            return;
                        }
                        else
                        {
                            System.out.println("Error 503 -> Bad sequence of commands");
                            _clientSocketManager._output.writeUTF("503" + CRLF);
                            fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + clientMSG + "Error 503 -> Bad sequence of commands \n");
                            fileWriter.flush();
                        }
                        clientMSG = _clientSocketManager._input.readUTF();
                    }
                    else
                        clientHasSaidHELO.set(true);
                }

                System.out.println("Message from client: " + currentAccount.getUsername() + " --> " + clientMSG);

                if (clientMSG.contains("QUIT"))
                {
                    System.out.println("Client \"" + currentAccount.getUsername() + "\" ending session");

                    _clientSocketManager._output.writeUTF("221" + LF + ServerDomainName + LF + " Service closing transmission channel" + CRLF);
                    _active_clients.remove(_clientSocketManager);
                    System.out.print("Active clients: "+_active_clients.size());

                    fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " QUIT");
                    fileWriter.flush();
                    return;
                }

                Server_SMTP_Handler(_clientSocketManager, clientMSG);
            }
        }
        catch (Exception except)
        {
            System.out.println("Error in Server Connection Handler --> " + except.getMessage());
        }
    }

    private void Server_SMTP_Handler(socketManager client, String clientMSG) throws IOException
    {
        String ServerDomainName = "ServerDomain.gr";
        String serverResponseToClient = "";

        ArrayList<String> recipientsKnownByServer = new ArrayList<String>();                        //The users (mailboxes) that the SMTP server recognizes.
        recipientsKnownByServer.add("howardphilips@client-domain.gr");
        recipientsKnownByServer.add("bobsalvatore@client-domain.gr");
        recipientsKnownByServer.add("rogerwaters@client-domain.gr");
        recipientsKnownByServer.add("rogerfederer@client-domain.gr");

        try
        {
            if (clientMSG.contains(CRLF))                                                           //The majority of the commands (keywords) sent by a client end with a CRLF. (p. 18)
            {
                if (clientMSG.contains("HELO"))
                {
                    if (!clientHasSentMAILFROM.get())                                               //Prevents accepting a HELO command, if a mail transaction is in progress.
                    {
                        email.clearEmailBuffers();                                                  //HELO confirms that the server is in its initial state. (p. 18)
                        serverResponseToClient = "250" + EC + ServerDomainName + CRLF;
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " HELO \n");
                        fileWriter.flush();
                    }
                    else
                    {
                        System.out.println("Error 503 -> Bad sequence of commands");
                        serverResponseToClient = "503" + CRLF;
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " HELO - Error 503 -> Bad sequence of commands \n");
                        fileWriter.flush();
                    }
                }
                else if (clientMSG.contains("MAIL") && clientMSG.contains("FROM") && clientMSG.contains("@"))
                {
                    if (clientHasSentMAILFROM.get())                  //Prevents client from sending a MAIL FROM command a second time during the same mail transaction.
                    {
                        System.out.println("Error 503 -> Bad sequence of commands");
                        serverResponseToClient = "503" + CRLF;
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " MAIL FROM - Error 503 -> Bad sequence of commands \n");
                        fileWriter.flush();
                    }
                    else
                    {
                        String isolateEmail = clientMSG.substring((clientMSG.indexOf("<") + 1), clientMSG.indexOf(">"));                //Isolates sender's email.
                        email.clearEmailBuffers();                                                      //Clears buffers and inserts the reverse-path into the respective buffer. (p. 19)
                        email.setReversePathBuffer(isolateEmail);
                        serverResponseToClient = "250" + CRLF;
                        clientHasSentMAILFROM.set(true);                                       //Activates the mail transaction status.
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " MAIL FROM " + isolateEmail + "\n");
                        fileWriter.flush();
                    }
                }
                else if (clientMSG.contains("RCPT"))
                {
                    if (clientHasSentMAILFROM.get())                                                    //Proceeds only if a MAIL FROM command has already been executed. (p. 3-4 and 26)
                    {
                        if (clientMSG.contains("@"))
                        {
                            String isolateEmail = clientMSG.substring((clientMSG.indexOf("<") + 1), clientMSG.indexOf(">"));                //Isolates recipient's email.

                            if (verifyUser(isolateEmail, recipientsKnownByServer))               //The recipient's address is processed only if it is known by the server.
                            {
                                email.setForwardPathBuffer(isolateEmail);
                                serverResponseToClient = "250" + CRLF;
                                clientHasSentRCPTTO.set(true);
                                fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " RCPT TO " + isolateEmail + "\n");
                                fileWriter.flush();
                            }
                            else
                            {
                                serverResponseToClient = "550 -> Requested action not taken: recipient not in server's list" + CRLF;
                                System.out.println("Error 550 -> Requested action not taken: recipient not in server's list");
                                fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " RCPT TO" + isolateEmail +
                                                    " - Error 550 -> Requested action not taken: recipient not in server's list \n");
                                fileWriter.flush();
                            }
                        }
                        else    //The string the user provided is not a valid email address.
                        {
                            serverResponseToClient = "500"+ CRLF;
                            System.out.println("Error 500 -> Syntax error, command unrecognized");
                            fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " RCPT TO - Error 500 -> Syntax error, command unrecognized \n");
                            fileWriter.flush();
                        }
                    }
                    else        //If the client selected RCPT TO without having first selected MAIL FROM.
                    {
                        System.out.println("Error 503 -> Bad sequence of commands");
                        serverResponseToClient = "503" + CRLF;
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " RCPT TO - Error 503 -> Bad sequence of commands \n");
                        fileWriter.flush();
                    }
                }
                else if (clientMSG.contains("DATA"))                                //Only proceeds if a RCPT TO command has been executed (p. 3-4 and 26)
                {                                                                     //and ends when the client sends a dot (".").
                    if (clientHasSentRCPTTO.get())
                    {
                        serverResponseToClient = "354" + EC + "Start mail input; Type the subject, press \"Enter\" and then type the main body of the email; End DATA message with a dot (\".\")" + CRLF;
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " DATA: \n");
                        fileWriter.flush();
                    }
                    else
                    {
                        System.out.println("Error 503 -> Bad sequence of commands");
                        serverResponseToClient = "503" + CRLF;
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " DATA - Error 503 -> Bad sequence of commands \n");
                        fileWriter.flush();
                    }
                }
                else if (clientMSG.contains("HELP"))                        //Command with multi-line response. (p. 49)
                {
                    serverResponseToClient = "214-" + "-->Press 1 to send the HELO command (resets states, transactions and buffers). A session can only start by sending this command.";
                    client._output.writeUTF(serverResponseToClient);
                    serverResponseToClient = "214-" + "-->Press 2 to send the MAIL FROM command (initiate a new mail transaction by sending your email address to the server).";
                    client._output.writeUTF(serverResponseToClient);
                    serverResponseToClient = "214-" + "-->Press 3 to send the RCPT TO command (send the recipient's email address to the server). Can only be sent after the MAIL FROM command.";
                    client._output.writeUTF(serverResponseToClient);
                    serverResponseToClient =  "214-" + "-->Press 4 to send the DATA command (send the email's subject and main content to the server and terminate the mail transaction). " +
                                         "Can only be sent after the RCPT TO command.";
                    client._output.writeUTF(serverResponseToClient);
                    serverResponseToClient =  "214-" + "-->Press 5 to send the RSET command [aborts the current mail transaction (clears email buffers and any already saved sender," +
                                         " recipient and mail body data)].";
                    client._output.writeUTF(serverResponseToClient);
                    serverResponseToClient =  "214-" + "-->Press 6 to send the READ MAIL command (read an email saved in your mailbox. Cannot be used while there is an ongoing mail transaction).";
                    client._output.writeUTF(serverResponseToClient);
                    serverResponseToClient =  "214-" + "-->Press 7 to send the DELETE MAIL command (delete an email saved in your mailbox).";
                    client._output.writeUTF(serverResponseToClient);
                    serverResponseToClient = "214-" + "-->Press 8 to send the NOOP command (instruct the server to reply with an \"OK\" reply).";
                    client._output.writeUTF(serverResponseToClient);
                    serverResponseToClient =  "214-" + "-->Press 9 to send the VRFY command (type a user name and if a user with this name is included in the server's list of email addresses/users," +
                                         " you will receive his/her email address).";
                    client._output.writeUTF(serverResponseToClient);
                    serverResponseToClient =  "214-" + "-->Press 10 to send the HELP command (reveals the current message).";
                    client._output.writeUTF(serverResponseToClient);
                    serverResponseToClient =  "214-" + "-->Press 11 to send the QUIT command, close the communication channel and exit the program.\n";
                    client._output.writeUTF(serverResponseToClient);
                    serverResponseToClient = "214" + EC + "Additional info about the SMTP can be found at https://datatracker.ietf.org/doc/html/rfc821\n" + CRLF;
                    fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " HELP \n");
                    fileWriter.flush();
                }
                else if (clientMSG.contains("NOOP"))                        //Just sends a 250 reply. (p. 25)
                {
                    serverResponseToClient = "250" + CRLF;
                    fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " NOOP \n");
                    fileWriter.flush();
                }
                else if (clientMSG.contains("RSET"))                       //Clears all mail buffers and resets current mail transaction.
                {
                    if (clientHasSentMAILFROM.get())                         //It is only accepted if a mail transaction is in progress. (p. 24)
                    {
                        email.applyRSETCommand();                            //Aborts current mail transaction, clears buffers and stored email content (recipient, mail body, sender) and resets states.
                        clientHasSentMAILFROM.set(false); 
                        if (clientHasSentRCPTTO.get())
                            clientHasSentRCPTTO.set(false);
                        serverResponseToClient = "250" + CRLF;
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " RSET \n");
                        fileWriter.flush();
                    }
                    else                                                     //If the client selected RSET without having first initiated a mail transaction.
                    {
                        System.out.println("Error 503 -> Bad sequence of commands");
                        serverResponseToClient = "503" + CRLF;
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " RSET - Error 503 -> Bad sequence of commands \n");
                        fileWriter.flush();
                    }
                }
                else if (clientMSG.contains("VRFY"))                                //Verifies that a user is known by the server. (p. 7-8)
                {
                    int counter = -1;
                    String sameUsernameFinder = "";
                    String isolateUsername = clientMSG.substring((clientMSG.indexOf("<") + 1), clientMSG.indexOf(">"));

                    for (String string : recipientsKnownByServer)                       //Searches the server's recipients list and adds 1 to the counter each time the search criterion matches
                    {                                                                   //the recipient's username or part of it.
                        if (string.contains(isolateUsername))
                        {
                            counter += 1;
                            sameUsernameFinder = sameUsernameFinder + string;
                        }
                    }

                    if (counter == -1)                      //If the counter stays as it was before searching, the user is unknown.
                    {
                        serverResponseToClient = "Error 550 -> User not in server's list" + CRLF;
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " VRFY" + clientMSG + " - Error 550 -> User not in server's list \n");
                        fileWriter.flush();
                    }
                    else if (counter == 0)                  //If there is only one username that matches the given search criterion, the counter's value is 0.
                    {
                        serverResponseToClient = "250" + EC + "<" + sameUsernameFinder + ">" + CRLF;
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " VRFY" + clientMSG + " \n");
                        fileWriter.flush();
                    }
                    else                                    //If there are more than 1 users that match the search criterion, the counter's value is 1 or bigger.
                    {
                        serverResponseToClient = "Error 553 -> User ambiguous (More than 1 users with this username)" + CRLF;
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " VRFY" + clientMSG + " - Error 553 -> User ambiguous (More than 1 users with this username) \n");
                        fileWriter.flush();
                    }
                }
                else if (clientMSG.contains("READ MAIL"))                                 //Command not included in RFC 821. Allows the client to read an email saved in his/her mailbox.
                {
                    if (clientHasSaidHELO.get() && !clientHasSentMAILFROM.get())            //Only accessible if HELO has been sent to the server and if no mail transactions are in progress.
                    {
                        serverResponseToClient = mailbox.showMailbox();
                        client._output.writeUTF(serverResponseToClient + "\nType the number of the email you want to read or any other number, to return to the main menu:");

                        clientMSG = _clientSocketManager._input.readUTF();

                        if (Integer.parseInt(clientMSG) >= 1 && Integer.parseInt(clientMSG) <= mailbox.getUserMailbox().size())
                        {
                            serverResponseToClient = mailbox.readEmail(Integer.parseInt(clientMSG));
                            client._output.writeUTF(serverResponseToClient);
                            client._output.writeUTF("250" + CRLF);
                            fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " READ MAIL \n");
                            fileWriter.flush();
                        }
                        serverResponseToClient = "";
                    }
                    else
                    {
                        System.out.println("Error 503 -> Bad sequence of commands");
                        serverResponseToClient = "503" + CRLF;
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " READ MAIL - Error 503 -> Bad sequence of commands \n");
                        fileWriter.flush();
                    }
                }
                else if (clientMSG.contains("DELETE MAIL"))                                 //Command not included in RFC 821. Allows the client to delete an email saved in his/her mailbox and then
                {                                                                             //saves the updated mailbox to the disk.
                    if (clientHasSaidHELO.get() && !clientHasSentMAILFROM.get())              //Only accessible if a HELO has been sent to the server and if no mail transactions are in progress.
                    {
                        serverResponseToClient = mailbox.showMailbox();
                        client._output.writeUTF(serverResponseToClient + "\nType the number of the email you want to delete or any other number, to return to the main menu:");

                        clientMSG = _clientSocketManager._input.readUTF();

                        if (Integer.parseInt(clientMSG) >= 1 && Integer.parseInt(clientMSG) <= mailbox.getUserMailbox().size())
                        {
                            serverResponseToClient = mailbox.deleteEmail(Integer.parseInt(clientMSG));
                            client._output.writeUTF(serverResponseToClient);
                            mailbox.saveMailboxToDisk(currentAccount.getUsername());
                            fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " DELETE MAIL \n");
                            fileWriter.flush();
                        }
                        serverResponseToClient = "";
                    }
                    else
                    {
                        System.out.println("Error 503 -> Bad sequence of commands");
                        serverResponseToClient = "503" + CRLF;
                        fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " DELETE MAIL - Error 503 -> Bad sequence of commands \n");
                        fileWriter.flush();
                    }
                }
                else
                {
                    serverResponseToClient = "500"+ CRLF;                                            //General error message.
                    System.out.println("Error 500 -> Syntax error, command unrecognized");
                    fileWriter.write(getTimeStamp() + currentAccount.getUsername() + ": " + " Error 500 -> Syntax error, command unrecognized \n");
                    fileWriter.flush();
                }
            }
            else                                                                            //Checking for input without CRLF, meaning that it is input following the DATA command (a mail transaction
            {                                                                               //has already started and the user has to type the mail's subject and main body).
                email.setMailSubject(clientMSG);                                            //Client's first input is saved as the mail's subject.
                fileWriter.write("\t Subject: " + clientMSG + "\n");
                fileWriter.flush();
                clientMSG = _clientSocketManager._input.readUTF();

                while(!clientMSG.equals(CRLF + "." + CRLF))                                 //As long as the client does not send a sentence whose contents are only a period ("."),
                {                                                                           //the data exchange continues (the server saves all input as the mail's body - p.20).
                    email.setMailDataBuffer(clientMSG);
                    fileWriter.write("\t\t" + clientMSG + "\n");
                    fileWriter.flush();
                    clientMSG = _clientSocketManager._input.readUTF();
                }

                email.processEmailBuffers();                                                //After the DATA command, the buffers are processed and then cleared. (p. 20)
                serverResponseToClient = "250" + CRLF;
                clientHasSentMAILFROM.set(false);                                  //Resets all static variables related to the current mail transaction.
                clientHasSentRCPTTO.set(false);
                email.finalizeEmail(ServerDomainName);
                mailbox.saveInUserMailbox(email);                                           //The email is saved in the current user's mailbox.
                mailbox.saveMailboxToDisk(currentAccount.getUsername());                    //The updated mailbox is saved to the disk.
                email.clearEmailBuffers();
            }

            client._output.writeUTF(serverResponseToClient);
        }
        catch (Exception except)
        {
            System.out.println("Error --> " + except.getMessage());
        }
    }
    
    private boolean verifyUser(String userAddress, ArrayList<String> userList)              //Checks if the inputted username exists in the given (the server's) users list.
    {
        boolean helperBool = false;

        for (String string : userList)
        {
            if (string.contains(userAddress))
            {
                helperBool = true;
                break;
            }
        }
        return helperBool;
    }

    private Date getTimeStamp()
    {
        Date currentTime = new Date();
        return currentTime;
    }
}