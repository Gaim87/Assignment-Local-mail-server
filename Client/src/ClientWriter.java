import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

//This class manages the client's outgoing data stream.
public class ClientWriter implements Runnable
{
    public static String CRLF = "\r\n";
//    public static String LF = "\n";
    public static String EC = " ";
    public static String ClientDomainName = "client-domain.gr";
    public static String ClientEmailAddress = "johndoe@" + ClientDomainName;
    Socket cwSocket = null;
    AtomicBoolean isDATAflag = new AtomicBoolean(false);
    AtomicBoolean sentHELOToServer = new AtomicBoolean(false);
    AtomicBoolean sentMAILFROMToServer = new AtomicBoolean(false);
    AtomicBoolean sentRCPTTOToServer = new AtomicBoolean(false);

    public ClientWriter(Socket outputSoc, AtomicBoolean isDATA)
    {
        cwSocket = outputSoc;
    }

    public void run()
    {
        String msgToServer = "";

        try
        {
            DataOutputStream dataOut = new DataOutputStream(cwSocket.getOutputStream());
            Scanner scanner = new Scanner(System.in);

            while(!cwSocket.isClosed())
            {
                Thread.sleep(250);                                         //Otherwise, the next line's message appears before the server's reply, increasing the chance of going unnoticed.
                System.out.println("Select a command by pressing the respective number:\n1...HELO 2...MAIL FROM 3...RCPT TO 4...DATA 5...RSET " +
                                                                                        "6...READ MAIL 7...DELETE MAIL 8...NOOP 9...VRFY 10...HELP 11...QUIT");

                int userInput = Integer.parseInt(scanner.nextLine());

                switch (userInput)
                {
                    case 1:
                    {
                        System.out.println("Sending \"HELO\" to server");       //Initiates a session.
                        System.out.println("--------------------------");

                        isDATAflag.set(false);                                             //HELO confirms that the client is in its initial state. (p. 18)
                        sentMAILFROMToServer.set(false);
                        sentRCPTTOToServer.set(false);

                        msgToServer = ("HELO" + EC + ClientDomainName + CRLF);                      //The syntax of all the client's commands is in p. 28.
                        dataOut.writeUTF(msgToServer);
                        dataOut.flush();
                        sentHELOToServer.set(true);
                        break;
                    }
                    case 2:
                    {
                        System.out.println("Sending \"MAIL FROM " + ClientEmailAddress + "\" to server");   //Sends the user's/sender's email to the server.
                        System.out.println("--------------------------");

                        msgToServer = ("MAIL" + EC + "FROM:" + "<" + ClientEmailAddress + ">" + CRLF);
                        dataOut.writeUTF(msgToServer);
                        dataOut.flush();

                        if(sentHELOToServer.get())                                          //To prevent accidentally entering mail transaction status, if HELO has not yet been sent.
                            sentMAILFROMToServer.set(true);
                        break;
                    }
                    case 3:     //RCPT TO
                    {
                        if (sentMAILFROMToServer.get())                                     //The RCPT TO command can only be sent after the MAIL FROM command. (p. 3-4, 26)
                        {
                            System.out.println("Please write the recipient's email address:");
                            String recipientEmailInput = scanner.nextLine();

                            System.out.println("Sending \"RCPT TO " + recipientEmailInput + "\" to server");    //Sends the recipient's email to the server.
                            System.out.println("--------------------------");

                            msgToServer = ("RCPT" + EC + "TO:" + "<" + recipientEmailInput + ">" + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                            sentRCPTTOToServer.set(true);
                        }
                        else
                        {                                                             //If the MAIL FROM command has not yet been sent, a plain (without the "@") RCPT TO command is forwarded,
                            System.out.println("Sending \"RCPT TO\" to server");    //which triggers an error response.
                            System.out.println("--------------------------");

                            msgToServer = ("RCPT" + EC + "TO:" + "<" + ">" + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                        }
                        break;
                    }
                    case 4:
                    {
                        if (sentRCPTTOToServer.get())                                       //The DATA command can only be sent after the RCPT TO command. (p. 3-4, 26)
                        {
                            System.out.println("Sending \"DATA\" to server");             ////Sends the email's main body/contents to the server.
                            System.out.println("--------------------------");

                            msgToServer = ("DATA" + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                            isDATAflag.set(true);                                           //Initiating the data exchange state.

                            while (isDATAflag.get())
                            {
                                String mailContentInput = scanner.nextLine();

                                while (!mailContentInput.equals("."))             //As long as the user does not send a sentence whose contents are only a period ("."),
                                {                                                          //the DATA command is not terminated. (p. 20)
                                    dataOut.writeUTF(mailContentInput);
                                    dataOut.flush();

                                    mailContentInput = scanner.nextLine();
                                }

                                dataOut.writeUTF(CRLF + "." + CRLF);
                                dataOut.flush();
                                isDATAflag.set(false);                            //Resetting all variables related to the current mail transaction, to enable sending another mail.
                                sentMAILFROMToServer.set(false);
                                sentRCPTTOToServer.set(false);
                            }
                        }
                        else
                        {
                            System.out.println("Sending \"DATA\" to server");            //If the RCPT TO command has not yet been sent, the DATA command is still forwarded,
                            System.out.println("--------------------------");            //so that the server issues an error message.

                            msgToServer = ("DATA" + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                        }
                        break;
                    }
                    case 5:
                    {
                        System.out.println("Sending \"RSET\"");                          //Aborts the email transaction.
                        System.out.println("--------------------------");

                        msgToServer = ("RSET" + CRLF);
                        dataOut.writeUTF(msgToServer);
                        dataOut.flush();
                        sentMAILFROMToServer.set(false);   //If the client sends RSET after having sent MAIL FROM, RCPT TO or both, those bools/the client's state has to be reset as well,
                        sentRCPTTOToServer.set(false);     //so that the DATA command cannot be run.
                        break;
                    }
                    case 6:
                    {
                        System.out.println("Sending \"READ MAIL\"");                     //Command not included in RFC 821.
                        System.out.println("--------------------------");

                        msgToServer = ("READ MAIL" + CRLF);
                        dataOut.writeUTF(msgToServer);
                        dataOut.flush();

                        if (sentHELOToServer.get() && !sentMAILFROMToServer.get())         //The command is sent only if a session with the server has been initiated
                        {                                                                  //AND no mail transaction is in progress.
                            String numberOfMailToRead = scanner.nextLine();

                            dataOut.writeUTF(numberOfMailToRead);
                            dataOut.flush();
                        }
                        break;
                    }
                    case 7:
                    {
                            System.out.println("Sending \"DELETE MAIL\"");               //Command not included in RFC 821.
                            System.out.println("--------------------------");

                            msgToServer = ("DELETE MAIL" + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();

                        if (sentHELOToServer.get() && !sentMAILFROMToServer.get())         //Also sent only if a session with the server has been initiated
                        {                                                                  //AND no mail transaction is in progress
                            String numberOfMailToDelete = scanner.nextLine();

                            dataOut.writeUTF(numberOfMailToDelete);
                            dataOut.flush();
                        }
                        break;
                    }
                    case 8:
                    {
                        System.out.println("Sending \"NOOP\"");                          //Prompts an "OK" response from the server.
                        System.out.println("--------------------------");

                        msgToServer = ("NOOP" + CRLF);
                        dataOut.writeUTF(msgToServer);
                        dataOut.flush();
                        break;
                    }
                    case 9:     //VRFY
                    {
                        if (sentHELOToServer.get())                                        //Works only if HELO has already been sent.
                        {
                            System.out.println("Write the username you want to verify (case-sensitive):");
                            String userToBeVerified = scanner.nextLine();

                            System.out.println("Sending \"VRFY " + userToBeVerified + "\" to server");      //Checks if a user exists in the server's users list.
                            System.out.println("--------------------------");

                            msgToServer = ("VRFY" + EC + "<" + userToBeVerified + ">" + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                        }
                        else                                                       //If the HELO command has not yet been sent, the VRFY command is still forwarded,
                        {                                                          //so that the server issues an error message.
                            System.out.println("Sending \"VRFY\"");
                            System.out.println("--------------------------");

                            msgToServer = ("VRFY" + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                        }
                        break;
                    }
                    case 10:
                    {
                        System.out.println("Sending \"HELP\"");                  //Displays instructions on how to use the program.
                        System.out.println("--------------------------");

                        msgToServer = ("HELP" + CRLF);
                        dataOut.writeUTF(msgToServer);
                        dataOut.flush();
                        break;
                    }
                    case 11:
                    {
                        System.out.print("Sending \"QUIT\"");

                        msgToServer = ("QUIT" + CRLF);
                        dataOut.writeUTF(msgToServer);
                        dataOut.flush();
                        System.out.println("Quiting...");
                        return;
                    }
                    default:
                        System.out.println("Type a number corresponding to the available commands.");
                }
            }
        }
        catch (Exception except)
        {
            System.out.println("Error in ClientWriter --> " + except.getMessage());
            this.run();
        }
    }
}