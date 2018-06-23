import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

public class OutputServer extends Thread
{

	static final Logger					logger			= Logger.getLogger("OutputServer");
	static int							PACKETSIZE		= 1500;
	DatagramSocket						serverSocket	= null;
	HashMap<Integer, EndPort>			EndPorts		= new HashMap<Integer, EndPort>();
	HashMap <Integer, ConcurrentLinkedQueue<Byte>>		queues			= null;
	ArrayList<Integer>					Ports = null;					
	Boolean								stopThread		= false;
	URI									orionAddress	= null;
	InetAddress							serverAddress	= null;
	GuiInterface gui = null;
	long	txByteCount		= 0;
	long	txFrameCount	= 0;

	public OutputServer(URI OrionAddress, HashMap <Integer, ConcurrentLinkedQueue<Byte>> Queues, GuiInterface Gui)
			throws SocketException, UnknownHostException
	{
		orionAddress = OrionAddress;
		if (serverSocket == null)
		{
			serverAddress = InetAddress.getByName(OrionAddress.getHost());
			serverSocket = new DatagramSocket(OrionAddress.getPort());
			serverSocket.setSoTimeout(500);
		}

		if (queues == null)
		{
			queues = Queues;
		}
		Ports = new ArrayList<Integer>();
		gui = Gui;
	}

	public void AddPort(int E1Port) throws IOException
	{
		if (EndPorts.get(E1Port) != null)
		{

		}
		
		EndPort ep = new EndPort(E1Port, orionAddress);
		EndPorts.put(E1Port, ep);
		Ports.add(E1Port);
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

		for (int Port : EndPorts.keySet())
		{
			Stop(Port);
		}

		EndPorts.clear();

		if (serverSocket != null)
		{
			serverSocket.close();
			serverSocket = null;
		}
		queues = null;
		Ports.clear();
		Ports = null;
	}

	public void Stop(int E1Port)
	{
		if (queues != null)
		{
			ConcurrentLinkedQueue<Byte> queue = queues.get(E1Port);
			if (queue != null)
			{
				queue.clear();
				queue = null;
			}
		}
		
		EndPort ep = EndPorts.get(E1Port);

		if (ep.socket != null)
		{
			ep.socket.close();
			ep.socket = null;
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
					packet.setPort(1);
					byte[] data = GetBytes(packet.getPort(), packet.getLength());
					
					
					EndPort ep = EndPorts.get(packet.getPort());
					if ((ep != null) & (ep.socket != null))
					{
						txFrameCount++;
						byte [] SAToPPacket = ep.Satop.GetBuffer(data);
						DatagramPacket SendPacket = new DatagramPacket(SAToPPacket, SAToPPacket.length, serverAddress, orionAddress.getPort());
						ep.socket.send(SendPacket);
						txByteCount += SAToPPacket.length;
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
				try
				{
					gui.UpdateStatistics(0, 0, txByteCount);
				}
				catch (Exception e)
				{
					logger.error("Can't collect receive statistics",e );
					gui.UpdateStatistics(0,0,txByteCount);
				}
			}
			catch (Exception e)
			{
				logger.error("Thread exception", e);
			}
		}
		logger.debug("UdpServer thread exit");
	}

	public long getTxByteCount()
	{
		return txByteCount;
	}

	public long getTxFrameCount()
	{
		return txFrameCount;
	}

	private byte[] GetBytes(int Port, int Length)
	{
		//TODO Add SAToP header
		byte[] data = new byte[Length];
		ConcurrentLinkedQueue<Byte> queue = queues.get(Port);
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
