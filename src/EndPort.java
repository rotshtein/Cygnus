import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

public class EndPort
{
	static final Logger					logger			= Logger.getLogger("EndPort");
	public SAToP Satop;
	public DatagramSocket socket = null;
	
	public EndPort(int E1Port, URI orionAddress) throws SocketException, UnknownHostException
	{
		socket = new DatagramSocket();
		InetAddress orionInet = InetAddress.getByName( orionAddress.getHost());
		socket = new DatagramSocket(E1Port, orionInet);
		socket.setSoTimeout(500);
	}
	
}
