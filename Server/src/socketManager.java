import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

//This Class is used by the server and manages data communication (both incoming and outgoing) with a given socket (the client's).
public class socketManager
{
	public Socket _clientSoc = null;
	public DataInputStream _input = null;
	public DataOutputStream _output = null;
	
	public socketManager(Socket socket) throws IOException
	{
		_clientSoc = socket;
		_input = new DataInputStream(_clientSoc.getInputStream());
		_output = new DataOutputStream(_clientSoc.getOutputStream());
	}
}