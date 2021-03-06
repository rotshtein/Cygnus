import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

public class InputServer extends Thread
{

	static final Logger	logger		= Logger.getLogger("InputServer");
	static int			PACKETSIZE	= 1500;

	DatagramSocket				socket			= null;
	Boolean						stopThread		= false;
	ConcurrentLinkedQueue<Byte>	queue			= null;
	URI							sourceUrl		= null;
	GuiInterface				gui				= null;
	long						rxByteCount		= 0;
	long						rxFrameCount	= 0;
	SAToP						Satop;
	int e1PortNumber = 0;

	public InputServer(int E1PortNumber, URI url, ConcurrentLinkedQueue<Byte> Queue, GuiInterface Gui)
			throws SocketException, UnknownHostException
	{
		e1PortNumber = E1PortNumber;
		try
		{
			if (socket != null)
			{
				socket.close();
			}
		}
		catch (Exception e)
		{
			logger.error("Closing ocket in ctor", e);
		}

		sourceUrl = url;
		socket = new DatagramSocket(null);

		socket.bind(new InetSocketAddress("0.0.0.0", sourceUrl.getPort()));
		socket.setSoTimeout(500);
		Satop = new SAToP();
		queue = Queue;

		gui = Gui;
		logger.info("UDP Server started listtning on port " + sourceUrl.getPort());
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
		socket.close();
		socket = null;
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
					socket.receive(packet);
				}
				catch (SocketTimeoutException te)
				{
					continue;
				}
				catch (IOException e)
				{
					logger.error("Error while receiving packet", e);
				}

				rxFrameCount++;
				try
				{

					byte[] data = packet.getData();
					byte[] SATopData = Satop.GetData(data);
					for (byte b : SATopData)
					{
						queue.add(b);
					}
					rxByteCount += SATopData.length;
				}
				catch (IllegalStateException e)
				{
					logger.error("Udp message queue full", e);
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

	public int getQSize()
	{
		return queue.size();
	}
	
	public long getRxByteCount()
	{
		return rxByteCount;
	}

	public long getRxFrameCount()
	{
		return rxFrameCount;
	}
}
