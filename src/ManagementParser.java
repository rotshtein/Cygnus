import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import cygnus_proto.Cygnus.Header;
import cygnus_proto.Cygnus.OPCODE;
import cygnus_proto.Cygnus.STATUS;
import cygnus_proto.Cygnus.StartCommand;
import cygnus_proto.Cygnus.StatusMessage;
import cygnus_proto.Cygnus.StatusReplay;

public class ManagementParser extends Thread implements GuiInterface
{

	static final Logger											logger			= Logger.getLogger("ManagmentParser");
	ManagementServer											server			= null;
	BlockingQueue<AbstractMap.SimpleEntry<byte[], WebSocket>>	queue			= null;
	WebSocket													currentConn		= null;
	Boolean														runThread;
	HashMap<Integer, ConcurrentLinkedQueue<Byte>>				queues			= null;
	List<InputServer>											inputServers	= null;
	OutputServer												outputServer	= null;
	int															Port1, Port2;
	boolean orionConnectionStatus = false;
	boolean e1Port1ConnectionStatus = false;
	boolean e1Port2ConnectionStatus = false;
	int port1QueueSize = 0;
	int port2QueueSize = 0;
	
	static long	rxBytes1	= 0;
	static long	rxBytes2	= 0;
	static long	txBytes		= 0;
	

	public ManagementParser(BlockingQueue<AbstractMap.SimpleEntry<byte[], WebSocket>> queue, ManagementServer server)
			throws Exception
	{
		this.queue = queue;
		this.server = server;
	}

	public void Stop()
	{
		runThread = false;
	}

	@Override
	public void run()
	{
		runThread = true;
		while (runThread)
		{
			try
			{
				if (Thread.interrupted())
				{
					logger.debug("Management Parser thread interrupted");
					break;
				}

				AbstractMap.SimpleEntry<byte[], WebSocket> request = null;
				try
				{
					request = queue.take();
				}
				catch (InterruptedException e)
				{
					logger.error("Error getting request from queue", e);
					continue;
				}
				Parse(request.getKey(), request.getValue());
			}
			catch (Exception e)
			{
				logger.error("Thread exception", e);
			}
		}
		logger.debug("Management Parser thread exit");
	}

	public void Parse(byte[] buffer, WebSocket conn)
	{
		currentConn = conn;

		Header h = getHeader(buffer);
		if (h == null)
		{
			SendNck(h, conn);
			return;
		}
		try
		{
			switch (h.getOpcode())
			{
			case HEADER:
				SendNck(h, conn);
				break;

			case START_CMD:
				StartCommand p = null;
				try
				{
					p = StartCommand.parseFrom(h.getMessageData());
				}
				catch (InvalidProtocolBufferException e)
				{
					logger.error("Failed to parse Start Command", e);
					SendNck(h, conn);
				}

				try
				{
					SendStatusMessage("Starting ...", conn);
					logger.info("Starting...");
					URI Url1 = new URI(p.getInput1Url());
					URI Url2 = new URI(p.getInput2Url());
					URI OraionUrl = new URI(p.getBoxUrl());
					Port1 = p.getE1Port1();
					Port2 = p.getE1Port2();
					StartForward(Port1, Port2, Url1, Url2, OraionUrl);
					SendStatusMessage("Forward process started");
					logger.info("Forward process started");

					SendAck(h, conn);
					SendProcessStartMessage();
				}
				catch (Exception e)
				{
					SendStatusMessage("Failed to start forwarding to Orion", conn);
					logger.error("Failed to start forwarding to Orion", e);
					if (h != null & conn != null)
					{
						SendNck(h, conn);
					}
				}
				break;

			case STOP_CMD:
				SendAck(h, conn);
				StopForward();
				SendProcessStopMessage();
				SendStatusMessage("Stop sending date to Orion");
				break;

			case STATUS_REQUEST:
				if (outputServer != null)
				{
					SendStatusReplay(STATUS.RUN);
				}
				else
				{
					 SendStatusReplay(STATUS.RUN);;
				}
				break;

			case STATUS_MESSAGE:
				BroadcastMessage(h.toByteString());
			default:
				SendNck(h, conn);
				break;
			}
		}
		catch (Exception e)
		{
			logger.error("Something want wrong in the parser", e);
		}
		currentConn = null;
	}

	private Boolean StartForward(int E1Port1, int E1Port2, URI Url1, URI Url2, URI BoxUrl)
			throws URISyntaxException, IOException
	{
		// StartForward(p.getE1Port1(), p.getE1Port2() , p.getInput1Url(),
		// p.getInput2Url(), p.getBoxUrl())

		queues = new HashMap<Integer, ConcurrentLinkedQueue<Byte>>();
		queues.put(E1Port1, new ConcurrentLinkedQueue<Byte>());
		queues.put(E1Port2, new ConcurrentLinkedQueue<Byte>());

		inputServers = new ArrayList<InputServer>();
		inputServers.add(new InputServer(E1Port1, Url1, queues.get(E1Port1), this));
		inputServers.add(new InputServer(E1Port2, Url2, queues.get(E1Port2), this));

		outputServer = new OutputServer(new URI(Parameters.Get("OraionURI")), queues, this);
		outputServer.AddPort(E1Port1);
		outputServer.AddPort(E1Port2);

		for (InputServer is : inputServers)
		{
			if (is != null)
			{
				is.start();
			}
		}

		outputServer.start();

		return true;
	}

	private Boolean StopForward()
	{
		for (InputServer s : inputServers)
		{
			s.Stop();
			s = null;
		}
		inputServers.clear();
		inputServers = null;

		outputServer.Stop();
		outputServer = null;
		return true;
	}

	private void SendAck(Header h, WebSocket conn)
	{
		try
		{
			Header hh = Header.newBuilder().setSequence(h.getSequence()).setOpcode(OPCODE.ACK).build();
			SendMessage(hh.toByteString(), conn);
		}
		catch (Exception e)
		{
			logger.error("Failed to send Ack to a connection", e);
		}

	}

	private void SendNck(Header h, WebSocket conn)
	{
		try
		{
			Header hh = Header.newBuilder().setSequence(h.getSequence()).setOpcode(OPCODE.NACK).build();
			SendMessage(hh.toByteString(), conn);
		}
		catch (Exception e)
		{
			logger.error("Failed to send Nack to a connection", e);
		}

	}

	private Header getHeader(byte[] buffer)
	{
		Header h = null;
		try
		{
			h = Header.parseFrom(buffer);
		}
		catch (InvalidProtocolBufferException e)
		{
			logger.error("Failed to parse message header", e);
		}

		return h;
	}

	private void SendProcessStopMessage()
	{
		SendStatusReplay(STATUS.STOP);
	}

	private void SendProcessStartMessage()
	{
		SendStatusReplay(STATUS.RUN);
	}

	private void SendStatusMessage(String message)
	{
		for (WebSocket conn : server.connections())
		{
			if (conn.isOpen())
			{
				SendStatusMessage(message, conn);
			}
		}
	}

	private void SendStatusMessage(String message, WebSocket conn)
	{
		try
		{
			StatusMessage s = StatusMessage.newBuilder().setMessage(message).build();
			Header h = Header.newBuilder().setSequence(0).setOpcode(OPCODE.STATUS_MESSAGE)
					.setMessageData(s.toByteString()).build();

			SendMessage(h.toByteString(), conn);
		}
		catch (Exception e)
		{
			logger.error("Failed to send StatusMessage to a connection", e);
		}
	}

	private void SendMessage(ByteString buffer, WebSocket conn)
	{
		try
		{
			if (conn.isOpen())
			{
				conn.send(buffer.toByteArray());
			}
		}
		catch (Exception e)
		{
			logger.error("Failed to send Message to a connection", e);
		}
	}

	private void BroadcastMessage(ByteString buffer)
	{
		// SendMessage(buffer);

		try
		{
			for (WebSocket conn : server.connections())
			{
				if (conn.isOpen())
				{
					conn.send(buffer.toByteArray());
				}
			}
		}
		catch (Exception e)
		{
			logger.error("Failed to Broadcast Message to a connection", e);
		}
	}

	public Boolean isRunning()
	{
		return (outputServer != null);
	}

	@Override
	public void OperationCompleted()
	{
		SendStatusReplay(STATUS.STOP);
	}

	public void OperationStarted()
	{
		SendStatusReplay(STATUS.RUN);
	}

	@Override
	public void UpdateStatus(String status)
	{
		try
		{
			StatusMessage s = StatusMessage.newBuilder().setMessage(status).build();
			Header h = Header.newBuilder().setSequence(0).setOpcode(OPCODE.STATUS_MESSAGE)
					.setMessageData(s.toByteString()).build();

			BroadcastMessage(h.toByteString());
		}
		catch (Exception e)
		{
			logger.error("Failed to send StatusMessage to a connection", e);
		}
	}

	@Override
	public void onConnectionChange(Boolean status)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void UpdateStatistics(long s, long s1, long txBytes)
	{
		if (inputServers != null)
		{
			if (inputServers.get(0) != null)
			{
				rxBytes1 = inputServers.get(0).Satop.getRxByteCount();
			}
			if (inputServers.get(1) != null)
			{
				rxBytes2 = inputServers.get(1).Satop.getRxByteCount();
			}
		}
		txBytes += txBytes;

	}
	
	private void SendStatusReplay(STATUS OperationStatus)
	{
		SendStatusReplay(OperationStatus,0,null,null);
	}

	@SuppressWarnings("unused")
	private void SendStatusReplay(STATUS OperationStatus, int ErrorNumber, String ErrorMessage)
	{
		SendStatusReplay(OperationStatus,ErrorNumber,ErrorMessage,null);
	}
	
	@SuppressWarnings("unused")
	private void SendStatusReplay(STATUS OperationStatus, String WarningMessage)
	{
		SendStatusReplay(OperationStatus,0,null,WarningMessage);
	}
	
	private void SendStatusReplay(STATUS OperationStatus, int ErrorNumber, String ErrorMessage, String WarningMessage)
	{
		StatusReplay sr = StatusReplay.newBuilder()
				.setStatus(OperationStatus)
				.setStream1InputBytes(rxBytes1).setStream2InputBytes(rxBytes2)
				.setE1Port1ConnectionStatus(outputServer.EndPorts.get(Port1).Satop.isConnectionStatus())
				.setE1Port2ConnectionStatus(outputServer.EndPorts.get(Port1).Satop.isConnectionStatus())
				.setOrionConnectionStatus(orionConnectionStatus)
				.setPort1QueueSize(inputServers.get(0).getQSize()).setPort2QueueSize(inputServers.get(1).getQSize())
				.setError(ErrorNumber != 0).setErrorMMessage(ErrorMessage == null ? "" : ErrorMessage).setErrorNumber(ErrorNumber)
				.setWarning(WarningMessage != null).setErrorMMessage(WarningMessage == null ? "" : WarningMessage)
				.setOutputBytes(txBytes).build();

		Header h = Header.newBuilder().setOpcode(OPCODE.STATUS_REPLAY).setMessageData(sr.toByteString()).build();

		BroadcastMessage(h.toByteString());
	}

	@Override
	public void OrionConnectionStatus(boolean status)
	{
		orionConnectionStatus = status;
	}

	@Override
	public void E1Port1ConnectionStatus(boolean status)
	{
		e1Port1ConnectionStatus = status;
	}

	@Override
	public void E1Port2ConnectionStatus(boolean status)
	{
		e1Port2ConnectionStatus = status;
	}

	@Override
	public void Port1QueueSize(int Qsize)
	{
		port1QueueSize = Qsize;
		
	}

	@Override
	public void Port2QueueSize(int Qsize)
	{
		port2QueueSize = Qsize;
	}

}
