import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

public class OutputServer extends Thread
{

	static final Logger					logger			= Logger.getLogger("OutputServer");
	DatagramSocket						serverSocket	= null;
	HashMap<Integer, DatagramSocket>	sockets			= new HashMap<Integer, DatagramSocket>();
	final int							PACKETSIZE		= 1500;
	ConcurrentLinkedQueue<Byte>[]		queues			= null;
	Boolean								stopThread		= false;
	URI									orionAddress	= null;
	InetAddress							serverAddress	= null;
	GuiInterface gui = null;
	long	rxByteCount		= 0;
	long	rxFrameCount	= 0;

	public OutputServer(URI OrionAddress, ConcurrentLinkedQueue<Byte>[] Queues, GuiInterface Gui)
			throws SocketException, UnknownHostException
	{
		orionAddress = OrionAddress;
		if (serverSocket == null)
		{
			serverAddress = InetAddress.getByName(OrionAddress.getHost());
			serverSocket = new DatagramSocket(OrionAddress.getPort(), serverAddress);
			serverSocket.setSoTimeout(500);
		}

		if (queues == null)
		{
			queues = Queues;
		}
		gui = Gui;
	}

	public void AddPort(int E1Port) throws IOException
	{
		if (sockets.get(E1Port) != null)
		{

		}
		{
			InetAddress orionInet = InetAddress.getByName( orionAddress.getHost());
			DatagramSocket socket = new DatagramSocket(E1Port, orionInet);
			socket.setSoTimeout(500);
			sockets.put(E1Port, socket);
		}
		logger.info("UDP Server started listtning on port " + E1Port);
	}

	public void Stop()
	{
		stopThread = true;
		try
		{
			this.join(1000);
		}
		catch (InterruptedException e)
		{
			logger.error("Failed to close a thread", e);
		}

		for (int Port : sockets.keySet())
		{
			Stop(Port);
		}

		sockets.clear();

		if (serverSocket != null)
		{
			serverSocket.close();
			serverSocket = null;
		}
		queues = null;
	}

	public void Stop(int E1Port)
	{
		if (queues != null)
		{
			ConcurrentLinkedQueue<Byte> queue = queues[E1Port];
			if (queue != null)
			{
				queue.clear();
				queue = null;
			}
		}

		DatagramSocket socket = sockets.get(E1Port);
		if (socket != null)
		{
			socket.close();
			socket = null;
		}
	}

	@Override
	public void run()
	{
		stopThread = false;
		while (!stopThread)
		{
			try
			{
				if (Thread.interrupted())
				{
					logger.debug("UdpServer thread interrupted");
					break;
				}

				DatagramPacket packet = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE);

				try
				{
					// Receive a packet (blocking) with 500 mSec timeout
					serverSocket.receive(packet);
				}
				catch (SocketTimeoutException te)
				{
					continue;
				}
				catch (IOException e)
				{
					logger.error("Error while receiving packet", e);
				}

				try
				{
					byte[] data = GetBytes(packet.getPort(), packet.getLength());
					DatagramPacket SendPacket = new DatagramPacket(data, data.length, serverAddress,
							orionAddress.getPort());
					DatagramSocket dsocket = sockets.get(packet.getPort());
					if (dsocket != null)
					{
						dsocket.send(SendPacket);
					}
				}
				catch (IllegalStateException e)
				{
					logger.error("Udp mssage queue full", e);
					// queue.clear();
				}
				catch (Exception e)
				{
					logger.error("Error during store packet in queue", e);
				}
			}
			catch (Exception e)
			{
				logger.error("Thread exception", e);
			}
		}
		logger.debug("UdpServer thread exit");
	}

	public long getRxByteCount()
	{
		return rxByteCount;
	}

	public long getRxFrameCount()
	{
		return rxFrameCount;
	}

	private byte[] GetBytes(int Port, int Length)
	{
		//TODO Add SAToP header
		byte[] data = new byte[Length];
		ConcurrentLinkedQueue<Byte> queue = queues[Port];
		if (queue.size() >= Length)
		{
			if (queue.size() >= 100 * Length)
			{
				for (int i = 0; i < Length; i++)
				{
					queue.poll();
				}
			}

			for (int i = 0; i < Length; i++)
			{
				data[i] = queue.poll();
			}

		}
		else
		{
			for (int i = 0; i < Length; i++)
			{
				data[i] = Byte.MAX_VALUE;
			}
		}
		return data;
	}

}
