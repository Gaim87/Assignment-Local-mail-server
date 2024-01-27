import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

//This class connects each client to the mail server. 
public class Server
{
    public static void main(String[] args)
    {
        Scanner userInput = new Scanner(System.in);

        System.out.println("Please define the port number that is going to be used:");

        int portNumber = userInput.nextInt();                                                        //Port number is specified at runtime

        try (ServerSocket serverSoc = new ServerSocket(portNumber);)
        {            
            ArrayList<socketManager> clients = new ArrayList<socketManager>();
            
            while (true)
            {
                System.out.println("Waiting for client");
                Socket soc = serverSoc.accept();                                                     //Blocks and waits for incoming clients to connect

                socketManager temp = new socketManager(soc);                                         //Client's incoming and outgoing communication channel.

                clients.add(temp);

                ServerConnectionHandler sch = new ServerConnectionHandler(clients, temp);            //Connects clients to the SMTP service.
                Thread schThread = new Thread(sch);

                schThread.start();
            }
        }
        catch (Exception except)
        {
            System.out.println("Error --> " + except.getMessage());
        }

         userInput.close();
    }   
}