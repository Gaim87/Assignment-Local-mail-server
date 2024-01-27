import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

//This class asks for a port number and then executes two objects that manage the client's incoming and outgoing communication with the server.
public class Client
{
    public static void main(String[] args)
    { 
        String serverIP = "localhost";
        Scanner userInput = new Scanner(System.in);

        System.out.println("Please define the port number that is going to be used:");

        int portNumber = userInput.nextInt();                                                           //Port number is specified at runtime

        try
        {
            Socket clientSocket = new Socket(serverIP, portNumber);
            AtomicBoolean isDATA = new AtomicBoolean(false);                              //Access is relinquished only after a thread has finished its work with the variable
            ClientReader clientReader = new ClientReader(clientSocket, isDATA);                        //Client's data input stream.
            Thread clientReadThread = new Thread(clientReader);

            clientReadThread.start();

            ClientWriter clientWriter = new ClientWriter(clientSocket, isDATA);                        //Client's data output stream.
            Thread clientWriteThread = new Thread(clientWriter);

            clientWriteThread.start();
        }
        catch (Exception except)
        {
            System.out.println("Error in SMTP_Client --> " + except.getMessage());
        }
    }
}