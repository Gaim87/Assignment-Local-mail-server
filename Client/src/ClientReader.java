import java.io.DataInputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

//This class manages the client's incoming data stream.
public class ClientReader implements Runnable
{
    public static String LF = "\n";
    
    Socket crSocket = null;
    AtomicBoolean isDATAflag;
    String BYTESin= "";

    public ClientReader (Socket inputSoc, AtomicBoolean isDATA)
    {
        crSocket = inputSoc;
        this.isDATAflag = isDATA;
    }
  
    public void run()
    {
        while(!crSocket.isClosed() && !isDATAflag.get())                            //While connection is open and not in data exchange state.
        {
            try
            {
                DataInputStream dataIn = new DataInputStream(crSocket.getInputStream());
                BYTESin = dataIn.readUTF();

                if (BYTESin.contains("220"))         //Relays different info to the client, depending on the server's reply code. Not all codes are used by the rest of the program. (p. 33-34)
                {
                    System.out.println(BYTESin);                          //220, "Service ready".
                }
                else if (BYTESin.contains("221"))                       //"Service closing transmission channel".
                {
                    System.out.println("...closing socket");
                    crSocket.close();
                    return;
                }
                else if (BYTESin.contains("214"))                       //"Help message".
                {
                    while (BYTESin.contains("214-"))                        //Multi-line responses include a hyphen. (p. 49)
                    {
                        System.out.println(BYTESin.substring(4));   //Shows the reply without the "214-" part.
                        BYTESin = dataIn.readUTF();
                    }
                    System.out.println(BYTESin.substring(4) + "OK -> CLIENT going to state SUCCESS");   //Last "HELP"/multi-line response starts with plain "214".
                }
                else if (BYTESin.contains("250"))           //"Requested mail action okay, completed".
                {
                    System.out.println("Reply: " + BYTESin + LF + "OK -> CLIENT going to state SUCCESS");
                }   
                else if (BYTESin.contains("500"))
                    System.out.println("SERVER Error--> Syntax error, command unrecognized");
                else if (BYTESin.contains("503"))
                    System.out.println("SERVER Error--> Bad sequence of commands");
                else if (BYTESin.contains("550"))           //"Requested action not taken: mailbox unavailable".
                    System.out.println(BYTESin);
                else if (BYTESin.contains("553"))           //"Requested action not taken: mailbox name not allowed".
                    System.out.println(BYTESin);
                else if (BYTESin.contains("354"))           //"Start mail input; end with <CRLF>.<CRLF>". Reply that initiates the data exchange of the mail's main body.
                {
                    System.out.println("OK -> CLIENT going to state I (wait for data)" + LF + BYTESin);
                    isDATAflag.set(true);

                    while (isDATAflag.get())
                    {
                        BYTESin = dataIn.readUTF();

                        if (BYTESin.contains("250"))
                        {
                            System.out.println("Reply: " + BYTESin + LF + "OK -> CLIENT going to state SUCCESS");
                            isDATAflag.set(false);                  //Data exchange state terminated only if server has processed the sent data and replied "OK". (p. 20)
                        }
                    }
                }
                else
                    System.out.println(BYTESin);
            }  
            catch (Exception except)
            {
              System.out.println("Error in ClientReader --> " + except.getMessage());
            }
        }
    }
}