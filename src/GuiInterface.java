public interface GuiInterface
{

	public static enum Channel
	{
		INPUT1, INPUT2, OUTPUT
	}

	void onConnectionChange(Boolean status);

	void UpdateStatus(final String status);

	void OperationCompleted();

	void OperationStarted();

	void UpdateStatistics(long rx1Bytes, long rx2Bytes, long txBytes);

	void OrionConnectionStatus(boolean status);
	void E1Port1ConnectionStatus(boolean status);
	void E1Port2ConnectionStatus(boolean status);
	void Port1QueueSize(int Qsize);
	void Port2QueueSize(int Qsize);
}
