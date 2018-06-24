import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class SAToP
{
	final byte L_BIR = 0x04;
	final byte R_BIR = 0x08;
	public static int SAToP_HEADER_SIZE = 4;
	final int NOT_INITIALIZE = 0xFFFFFF;
	
	int rxSeq;
	int txSeq;
	public long rxByteCount;
	public long rxFrameCount;
	public long rxFrameLostCount;
	public long txByteCount;
	public long txFrameCount;
	
	public SAToP()
	{
		rxSeq = NOT_INITIALIZE;
		txSeq = new Random().nextInt() & 0xFFFF;

		rxByteCount = 0;
		rxFrameCount = 0;
		rxFrameLostCount = 0;
		txByteCount = 0;
		txFrameCount = 0;
	}
	
	public byte[] GetBuffer(byte[] data)
	{
		byte[] result = new byte[data.length + SAToP_HEADER_SIZE];
		ByteBuffer DataStrem = ByteBuffer.wrap(result);
		DataStrem.order(ByteOrder.BIG_ENDIAN);
		DataStrem.put((byte) '\0');
		if (result.length < 64)
		{
			DataStrem.put((byte) result.length);
		}
		else
		{
			DataStrem.put((byte) '\0');  
		}
		
		DataStrem.putShort((short) txSeq);
		
	    System.arraycopy(data, 0, result, SAToP_HEADER_SIZE, data.length); 
	    incTxSeq();
	    return result;
	}
	
	
	public byte[] GetData(byte[] data)
	{
		rxFrameCount++;
		
		byte [] StriptData = null;
		ByteBuffer DataStrem = ByteBuffer.wrap(data);
		DataStrem.order(ByteOrder.BIG_ENDIAN);
		
		byte flags = DataStrem.get();  
		if ((flags & L_BIR) != 0)
		{
			StriptData = new byte[0];
			return StriptData;
		}
		
		byte len = (byte) (DataStrem.get() & 0x3f); // 
		if (len == 0)
		{
			StriptData = new byte[data.length - SAToP_HEADER_SIZE];
			rxByteCount += data.length;
		}
		else
		{
			StriptData = new byte[len - SAToP_HEADER_SIZE];
			
			rxByteCount += len;
		}
		System.arraycopy(data, SAToP_HEADER_SIZE, StriptData, 0, StriptData.length);
		
		int nSeq = DataStrem.getShort();
		if (rxSeq == NOT_INITIALIZE)
		{
			rxSeq = nSeq;
		}
		else
		{
			if (rxSeq + 1 == nSeq)
			{
				incRxSeq();
			}
			else
			{
				rxFrameLostCount += nSeq - rxSeq;
				for (int i=0; i < nSeq-rxSeq; i++)
				{
					incRxSeq();
				}
			}
		}
		return StriptData;
	}
	
	void incRxSeq()
	{
		rxSeq++;
		if (rxSeq == 0x10000)
		{
			rxSeq = 0;
		}
	}
	
	void incTxSeq()
	{
		rxSeq++;
		if (txSeq == 0x10000)
		{
			txSeq = 0;
		}
	}
	
	public long getRxByteCount()
	{
		return rxByteCount;
	}
	
	public long getTxByteCount()
	{
		return txByteCount;
	}
}
