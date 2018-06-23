import java.net.URI;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import cygnus_proto.Cygnus.Header;
import cygnus_proto.Cygnus.OPCODE;
import cygnus_proto.Cygnus.STATUS;
import cygnus_proto.Cygnus.StartCommand;
import cygnus_proto.Cygnus.StatusMessage;
import cygnus_proto.Cygnus.StatusReplay;


public class ManagementClient extends WebSocketClient
{

	static final Logger	logger				= Logger.getLogger("ManagementClient");
	GuiInterface		gui					= null;
	Boolean				connectionStatus	= false;
	Boolean				gotAck				= false;
	Boolean				gotNck				= false;
	Boolean				operationStarted	= false;
	Boolean				syncTimeout			= false;
	Boolean				runThread			= true;

	public ManagementClient(URI serverUri, GuiInterface gui)
	{
		super(serverUri);
		this.gui = gui;
		this.connect();
		logger.debug("ManagementClient and the SyncTimeoutThread started");
	}

	public void Stop()
	{
		// syncTimeoutThread.stop();
		runThread = false;
		logger.debug("Stop ManagementClient and the SyncTimeoutThread");
		this.close();
	}

	@Override
	public void onOpen(ServerHandshake handshakedata)
	{
		gui.UpdateStatus("Connected to the server");
		logger.info("Connected");
	}

	@Override
	public void onMessage(String message)
	{
		logger.info("got message: " + message);
	}

	@Override
	public void onMessage(ByteBuffer buffer)
	{
		Header h = null;
		try
		{
			h = Header.parseFrom(buffer.array());

			if (h != null)
			{
				// logger.debug("Got header. Command = " + h.getOpcode());
			}
			// int i = h.getOpcodeValue();
			switch (h.getOpcode())
			{
			case HEADER:
				logger.error("Got header only");
				break;

			case STOP_CMD:
				logger.error("Client got Stop command");
				break;

			case STATUS_REQUEST:
				logger.error("Client got Status request");
				break;

			case ACK:
				gotAck = true;
				break;

			case NACK:
				gotNck = true;
				break;

			case STATUS_REPLAY:
				StatusReplay sr = StatusReplay.parseFrom(h.getMessageData());

				// gui.UpdateStatus(sr.getStatusDescription());

				if (sr.getError())
				{
					gui.UpdateStatus(sr.getErrorMMessage());
				}
				else if (sr.getWarning())
				{
					gui.UpdateStatus(sr.getWarningMessage());
				}

				if (sr.getStatus() == STATUS.STOP)
				{
					// gui.UpdateStatus(sr.getStatusDescription());
					gui.OperationCompleted();
					operationStarted = false;

				}
				else if (sr.getStatus() == STATUS.RUN)
				{
					// gui.UpdateStatus(sr.getStatusDescription());
					if (operationStarted == false)
					{
						gui.OperationStarted();
						operationStarted = true;
					}
					
					gui.UpdateStatistics(sr.getStream1InputBytes(), sr.getStream2InputBytes(), sr.getOutputBytes());
				}

				
				break;

			case STATUS_MESSAGE:
				StatusMessage sm = StatusMessage.parseFrom(h.getMessageData());
				gui.UpdateStatus(sm.getMessage());
				break;

			default:
				logger.error("Unknown command.");
				break;
			}

		}
		catch (InvalidProtocolBufferException e)
		{
			logger.error("Protocol buffer Header parsing error", e);
		}

	}

	@Override
	public void onClose(int code, String reason, boolean remote)
	{
		logger.info("Disconnected");
		gui.UpdateStatus("Connection to the server closed!");
	}

	@Override
	public void onError(Exception ex)
	{
		gui.UpdateStatus("Websocket error received from the server");
		logger.error("Wensocket error", ex);
	}

	public Boolean SendStartCommand(String input1_url, String input2_url, int Port1, int Port2, String OrionUrl)
	{
  	try
		{
			StartCommand sc = StartCommand.newBuilder().setInput1Url(input1_url)
					.setInput2Url(input2_url).setE1Port1(Port1).setE1Port2(Port2).setBoxUrl(OrionUrl).build();

			send(0, OPCODE.START_CMD, sc.toByteString());
			return true;
		}
		catch (Exception e)
		{
			logger.error("Failed to send Start command", e);
		}
		return false;
	}

	public Boolean SendStopCommand()
	{
		try
		{
			Header h = Header.newBuilder().setSequence(0).setOpcode(OPCODE.STOP_CMD).build();
			if (this.isOpen())
			{
				this.send(h.toByteArray());
			}
			send(0, OPCODE.STOP_CMD, null);
		}
		catch (Exception e)
		{
			logger.error("Send SendStopCommand error", e);
			return false;
		}
		return true;
	}

	public Boolean SendStatus(String status)
	{
		try
		{
			StatusMessage sm = StatusMessage.newBuilder().setMessage(status).build();
			Header h = Header.newBuilder().setSequence(0).setOpcode(OPCODE.STATUS_MESSAGE)
					.setMessageData(sm.toByteString()).build();

			if (this.isOpen())
			{
				this.send(h.toByteArray());
			}
		}
		catch (Exception e)
		{
			logger.error("Send SendStatus error", e);
			return false;
		}
		return true;
	}

	public void send(int Sequence, OPCODE opcode, ByteString data)
	{
		gotAck = false;
		gotNck = false;

		Header h = null;
		try
		{
			if (data != null)
			{
				h = Header.newBuilder().setSequence(Sequence).setOpcode(opcode).setMessageData(data).build();
			}
			else
			{
				h = Header.newBuilder().setSequence(Sequence).setOpcode(opcode).build();
			}
			if (this.isOpen())
			{
				this.send(h.toByteArray());
			}
		}
		catch (Exception e)
		{
			logger.error("Send error", e);
		}

	}

	public Boolean WaitForAck(long milliseconds)
	{
		long Start = System.currentTimeMillis();

		while (gotAck != true)
		{
			if (System.currentTimeMillis() - Start > milliseconds)
			{
				logger.warn("Timeout in getting Ack");
				return false;
			}
		}
		return true;
	}

	public Boolean isAck()
	{
		return gotAck;
	}


}