public interface GuiInterface
{

	public enum Channel
	{
		INPUT1, INPUT2, OUTPUT
	}

	void onConnectionChange(Boolean status);

	void UpdateStatus(final String status);

	void OperationCompleted();

	void OperationStarted();
	
	void UpdateStatistics(Channel channel, long bytes);

	// void OperationInSync(Channel ch);

	// void OperationOutOfSync(Channel ch);
}
