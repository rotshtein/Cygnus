

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class ManagementServer extends WebSocketServer
{

	Logger														logger				= Logger
			.getLogger("ManagementServer");
	BlockingQueue<AbstractMap.SimpleEntry<byte[], WebSocket>>	queue				= null;
	ManagementParser											parser;
	Boolean														_stop				= false;
	Thread														monitorProcesThread	= null;
	String														paramFile			= null;

	public ManagementServer(InetSocketAddress address)
	{
		super(address);
		try
		{
			queue = new ArrayBlockingQueue<SimpleEntry<byte[], WebSocket>>(1);
			parser = new ManagementParser(queue, this);
			parser.start();
			monitorProcesThread = new Thread(MonitorProcesThread);
			monitorProcesThread.start();
		}
		catch (Exception e)
		{
			logger.error("Parse error", e);
		}
	}

	public void dispose()
	{
		Stop();
	}

	public void Stop()
	{
		logger.info("Clossing connections");
		_stop = true;
		try
		{
			this.stop(1);
			for (WebSocket conn : this.connections())
			{
				logger.info("\tClossing connection " + conn.getLocalSocketAddress().getHostString());
				conn.close();
			}
		}
		catch (Exception e)
		{
			logger.error("Error while closing Web socket server", e);
		}
		if (monitorProcesThread != null)
		{
			try
			{
				monitorProcesThread.join(100);
			}
			catch (InterruptedException e)
			{
				logger.error("Error while joining the MonitorProcessThread", e);
			}
		}
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake)
	{
		// conn.send("Welcome to the server!"); // This method sends a message to the
		// new client
		// broadcast( "new connection: " + handshake.getResourceDescriptor() ); //This
		// method sends a message to all clients connected
		logger.info("new connection to " + conn.getRemoteSocketAddress());
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote)
	{
		logger.info(
				"closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
	}

	@Override
	public void onMessage(WebSocket conn, String message)
	{
		logger.warn("received message from " + conn.getRemoteSocketAddress() + ": " + message);
		conn.send("Meesage should send as ByteBuffer in protocol buffer according MedCic.proto");
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer message)
	{

		// conn.send("received ByteBuffer from " + conn.getRemoteSocketAddress());
		// parser.Parse(message.array(), conn);
		try
		{
			queue.put(new SimpleEntry<byte[], WebSocket>(message.array(), conn));
		}
		catch (Exception e)
		{
			logger.error("Error putting message in parser queue", e);
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex)
	{

		logger.error("an error occured on connection ", ex);
	}

	@Override
	public void onStart()
	{
		logger.info("server started successfully");
	}

	Runnable MonitorProcesThread = new Runnable()
	{

		public void run()
		{

			while (!_stop)
			{
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					logger.error("Failed to sleep", e);
				}
			}
		}
	};

}
