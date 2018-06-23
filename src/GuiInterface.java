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

	// void OperationInSync(Channel ch);

	// void OperationOutOfSync(Channel ch);
}
