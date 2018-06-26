import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import cygnus_proto.Cygnus.OPCODE;

import javax.swing.BorderFactory;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;

import java.awt.Color;
import java.awt.ComponentOrientation;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.StringTokenizer;
import java.awt.event.ActionEvent;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.text.DefaultFormatter;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class MainScreen implements GuiInterface
{

	class IPAddressFormatter extends DefaultFormatter
	{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public String valueToString(Object value) throws ParseException
		{
			if (!(value instanceof byte[])) throw new ParseException("Not a byte[]", 0);
			byte[] a = (byte[]) value;
			if (a.length != 4) throw new ParseException("Length != 4", 0);
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < 4; i++)
			{
				int b = a[i];
				if (b < 0) b += 256;
				builder.append(String.valueOf(b));
				if (i < 3) builder.append('.');
			}
			return builder.toString();
		}

		public Object stringToValue(String text) throws ParseException
		{
			StringTokenizer tokenizer = new StringTokenizer(text, ".");
			byte[] a = new byte[4];
			for (int i = 0; i < 4; i++)
			{
				int b = 0;
				if (!tokenizer.hasMoreTokens()) throw new ParseException("Too few bytes", 0);
				try
				{
					b = Integer.parseInt(tokenizer.nextToken());
				}
				catch (NumberFormatException e)
				{
					throw new ParseException("Not an integer", 0);
				}
				if (b < 0 || b >= 256) throw new ParseException("Byte out of range", 0);
				a[i] = (byte) b;
			}
			if (tokenizer.hasMoreTokens()) throw new ParseException("Too many bytes", 0);
			return a;
		}
	}

	static final Logger		logger					= Logger.getLogger("MainScreen");
	private JFrame			frmCygnusVersion;
	JFormattedTextField		txtIn1;
	JFormattedTextField		txtIn2;
	JButton					btnStart;
	ManagementServer		server;
	ManagementClient		client;
	private final JButton	btnStop					= new JButton("Stop");
	static String			configurationFilename	= "config.properties";
	JScrollPane				jsp;
	// MessageParser messageParser = null;
	private final JTextArea	textArea				= new JTextArea();
	private final JButton	btnClear				= new JButton("Clear");
	private final JButton	btnSave					= new JButton("Save");
	String					serverUri				= null;
	Boolean					isRunning				= false;
	long					lastUpdateTimeSync		= System.currentTimeMillis();
	long					lastUpdateTimeOutofSync	= System.currentTimeMillis();
	private final JPanel	pnlCounters				= new JPanel();
	private JLabel			lblCicInpoutbytes;
	private JLabel			lblIn1Counter;
	private final JPanel	pnlSetup				= new JPanel();
	private final JLabel	lblCic2InpoutBytes		= new JLabel("2nd Inpout [Bytes]");
	private final JLabel	lblIn2Counter			= new JLabel("0");
	// private final String Version = "1.0";
	private final JSpinner				numPort2			= new JSpinner();
	private final JSpinner				numPort1			= new JSpinner();
	private final JLabel				lblstEPort			= new JLabel("1st E1 Port");
	private final JLabel				lblndEPort			= new JLabel("2nd E1 Port");
	private final JScrollPane			scrollPane			= new JScrollPane();
	private final JFormattedTextField	ftxtOrionAddress	= new JFormattedTextField(new IPAddressFormatter());
	private final JLabel				lblStream			= new JLabel("Stream 1");
	private final JLabel				lblStream_1			= new JLabel("Stream 2");
	private final JLabel				lblOrionIpAddress	= new JLabel("Orion IP Address");
	private final JLabel				labelOut			= new JLabel("0");
	private final JLabel				lblOutpoutbytes		= new JLabel("Outpout [Bytes]");
	private URI							OrionURI			= null;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args)
	{

		if (args.length > 2)
		{
			if (args[0].equals("-c"))
			{
				configurationFilename = args[1];
			}
		}

		EventQueue.invokeLater(new Runnable()
		{

			public void run()
			{
				try
				{
					MainScreen window = new MainScreen();
					window.frmCygnusVersion.setVisible(true);
				}
				catch (Exception e)
				{
					logger.error("Filed to start the application", e);
				}
			}
		});
	}

	/**
	 * Create the application.
	 * 
	 * @throws URISyntaxException
	 */
	public MainScreen() throws URISyntaxException
	{
		initialize();
		// txtIn1 ****://###.###.###.###:#####
		// MaskFormatter formatter = new MaskFormatter("****://###.###.###.###:#####");
		// txtIn1.setFormatterFactory(forrmatter);

		txtIn1.setText(Parameters.Get("url-in-1", "udp://127.0.0.1:5001"));
		txtIn2.setText(Parameters.Get("url-in-2", "udp://127.0.0.1:5002"));
		numPort2.setModel(new SpinnerNumberModel(2, 1, 32, 1));
		numPort2.setBounds(379, 73, 45, 20);
		numPort2.setValue(Integer.parseInt(Parameters.Get("E1Port2", "2")));

		pnlSetup.add(numPort2);
		numPort1.setModel(new SpinnerNumberModel(1, 1, 32, 1));
		numPort1.setToolTipText("Orion Port");
		numPort1.setBounds(379, 42, 45, 20);
		numPort1.setValue(Integer.parseInt(Parameters.Get("E1Port1", "1")));

		pnlSetup.add(numPort1);
		lblstEPort.setBounds(315, 46, 76, 13);

		pnlSetup.add(lblstEPort);
		lblndEPort.setBounds(313, 77, 76, 13);

		pnlSetup.add(lblndEPort);
		ftxtOrionAddress.setBounds(321, 8, 103, 23);
		try
		{
			OrionURI = new URI(Parameters.Get("OraionURI", "udp://127.0.0.1:2421"));
		}
		catch (Exception e)
		{
			try
			{
				Parameters.Set("OraionURI", "udp://127.0.0.1:2421");
			}
			catch (IOException e1)
			{
				// TODO Auto-generated catch block
			}
			OrionURI = new URI(Parameters.Get("OraionURI", "udp://127.0.0.1:2421"));
		}
		ftxtOrionAddress.setText(OrionURI.getHost());
		pnlSetup.add(ftxtOrionAddress);
		lblStream.setBounds(17, 46, 62, 13);

		pnlSetup.add(lblStream);
		lblStream_1.setBounds(17, 77, 62, 13);

		pnlSetup.add(lblStream_1);
		lblOrionIpAddress.setBounds(224, 13, 118, 13);

		pnlSetup.add(lblOrionIpAddress);
		btnClear.setBounds(102, 516, 74, 23);
		btnClear.addActionListener(new ActionListener()
		{

			public void actionPerformed(ActionEvent e)
			{
				textArea.setText("");
			}
		});
		scrollPane.setBounds(10, 158, 441, 264);

		frmCygnusVersion.getContentPane().add(scrollPane);
		scrollPane.setViewportView(textArea);
		textArea.setFocusable(false);
		textArea.setForeground(new Color(0, 0, 255));
		textArea.setToolTipText("Clear the message logger");
		textArea.setEditable(false);
		btnClear.setFont(new Font("Tahoma", Font.PLAIN, 10));

		frmCygnusVersion.getContentPane().add(btnClear);
		btnSave.setToolTipText("Save the message logger to a file");
		btnSave.setBounds(280, 516, 74, 23);
		btnSave.setEnabled(false);
		btnSave.setFont(new Font("Tahoma", Font.PLAIN, 10));

		frmCygnusVersion.getContentPane().add(btnSave);
		pnlCounters.setForeground(Color.LIGHT_GRAY);
		pnlCounters.setBounds(10, 433, 441, 73);

		frmCygnusVersion.getContentPane().add(pnlCounters);
		pnlCounters.setLayout(null);
		pnlCounters.setBorder(BorderFactory.createTitledBorder("Couters"));

		lblCicInpoutbytes = new JLabel("1st Inpout [Bytes]");
		lblCicInpoutbytes.setBounds(10, 13, 104, 20);
		pnlCounters.add(lblCicInpoutbytes);

		lblIn1Counter = new JLabel("0");
		lblCicInpoutbytes.setLabelFor(lblIn1Counter);
		lblIn1Counter.setBorder(new LineBorder(new Color(0, 0, 0)));
		lblIn1Counter.setHorizontalAlignment(SwingConstants.CENTER);
		lblIn1Counter.setBounds(118, 13, 104, 20);
		pnlCounters.add(lblIn1Counter);
		lblCic2InpoutBytes.setBounds(10, 43, 104, 20);

		pnlCounters.add(lblCic2InpoutBytes);
		lblIn2Counter.setBorder(new LineBorder(new Color(0, 0, 0)));
		lblIn2Counter.setHorizontalAlignment(SwingConstants.CENTER);
		lblIn2Counter.setBounds(118, 43, 104, 20);

		pnlCounters.add(lblIn2Counter);
		labelOut.setHorizontalAlignment(SwingConstants.CENTER);
		labelOut.setBorder(new LineBorder(new Color(0, 0, 0)));
		labelOut.setBounds(327, 27, 104, 20);

		pnlCounters.add(labelOut);
		lblOutpoutbytes.setBounds(232, 27, 99, 20);

		pnlCounters.add(lblOutpoutbytes);
		btnStop.setBounds(290, 125, 57, 23);
		frmCygnusVersion.getContentPane().add(btnStop);
		btnStop.setToolTipText("Stop de-encapsulation process.");
		btnStop.setFont(new Font("Tahoma", Font.PLAIN, 10));

		btnStart = new JButton("Start");
		btnStart.setBounds(118, 125, 57, 23);
		frmCygnusVersion.getContentPane().add(btnStart);
		btnStart.setToolTipText("Start de-encapsulation process.");
		btnStart.setFont(new Font("Tahoma", Font.PLAIN, 10));
		btnStart.addActionListener(new StartAction());

		btnStop.addActionListener(new ActionListener()
		{

			public void actionPerformed(ActionEvent e)
			{
				if (client != null)
				{
					client.send(0, OPCODE.STOP_CMD, null);
					try
					{
						Thread.sleep(200);
					}
					catch (InterruptedException e1)
					{
						logger.error("Faild to stop", e1);
					}
				}
				isRunning = false;
				OperationCompleted();
				btnStart.setEnabled(true);
			}
		});
		// frame.getContentPane().add(scroll);

		String host = Parameters.Get("WebSocketListenAddress", "127.0.0.1");
		int port = Integer.parseInt(Parameters.Get("WebSocketListenPort", "8887"));

		server = new ManagementServer(new InetSocketAddress(host, port));
		server.start();
		serverUri = Parameters.Get("WebSocketServerUri", "ws://127.0.0.1:8887");
		client = new ManagementClient(new URI(serverUri), MainScreen.this);
	}

	class StartAction implements ActionListener
	{

		public void actionPerformed(ActionEvent arg0)
		{
			isRunning = true;
			try
			{
				if (client == null)
				{
					client = new ManagementClient(new URI(serverUri), MainScreen.this);
				}
			}
			catch (URISyntaxException e)
			{
				logger.error("Failed to create client", e);
				client = null;
			}
			try
			{
				Parameters.Set("url-in-1", txtIn1.getText());
				Parameters.Set("url-in-2", txtIn2.getText());
				Parameters.Set("E1Port1", numPort1.getValue().toString());
				Parameters.Set("E1Port2", numPort2.getValue().toString());
				OrionURI = URI.create("udp://" + ftxtOrionAddress.getText() + ":" + OrionURI.getPort());
				Parameters.Set("OrionURI", OrionURI.toString());
			}
			catch (IOException e1)
			{
				logger.error("Failed to save parameters", e1);
			}

			client.SendStartCommand(txtIn1.getText(), txtIn2.getText(), (Integer) numPort1.getValue(),
					(Integer) numPort2.getValue(), OrionURI.toString());
			btnStart.setEnabled(false);
		}
	}

	class UrlVerifier extends InputVerifier
	{

		public boolean verify(JComponent input)
		{
			if (!(input instanceof JFormattedTextField)) return true;
			String first = ((JFormattedTextField) input).getText();
			if (first.startsWith("udp://") || first.startsWith("file://"))
			{
				String[] splitted = first.split(":");
				if (splitted[0].equals("udp"))
				{
					if (splitted.length == 3)
					{
						try
						{
							int port = Integer.parseInt(splitted[2]);
							if (port > 0 && port < 0xFFFF)
							{
								return true;
							}

						}
						catch (Exception ex)
						{
							logger.error("Worng port number", ex);
						}
						JOptionPane.showMessageDialog(null,
								"Wrong URL format.:port should be a number between 1 and 65535");
						return false;
					}
				}
				else
				{
					return true;
				}
			}
			JOptionPane.showMessageDialog(null,
					"Wrong URL format. Should be udp://ip:port or file://<file path and name>");
			return false;
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize()
	{
		frmCygnusVersion = new JFrame();
		frmCygnusVersion
				.setIconImage(Toolkit.getDefaultToolkit().getImage(MainScreen.class.getResource("/forward.jpg")));
		frmCygnusVersion.addWindowListener(new WindowAdapter()
		{

			@Override
			public void windowClosing(WindowEvent e)
			{
				Stop();
			}
		});
		frmCygnusVersion.setBounds(100, 100, 480, 604);
		frmCygnusVersion.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmCygnusVersion.getContentPane().setLayout(null);
		pnlSetup.setBounds(10, 11, 441, 104);
		pnlSetup.setBorder(BorderFactory.createTitledBorder("Setup"));
		frmCygnusVersion.getContentPane().add(pnlSetup);
		pnlSetup.setLayout(null);

		txtIn2 = new JFormattedTextField();
		txtIn2.setBounds(70, 73, 197, 20);
		pnlSetup.add(txtIn2);
		txtIn2.setToolTipText("CIC 2 signal source URI in the form of udp://<ip address>:<port>");
		txtIn2.setText("udp://127.0.0.0.2:1000");
		txtIn2.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		txtIn2.setInputVerifier(new UrlVerifier());

		txtIn1 = new JFormattedTextField();
		txtIn1.setBounds(70, 42, 197, 20);
		pnlSetup.add(txtIn1);
		txtIn1.setBackground(Color.WHITE);
		txtIn1.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		txtIn1.setToolTipText("CIC 1 signal source URI in the form of udp://<ip address>:<port>");
		txtIn1.setText("udp://127.0.0.0.1:1000");
		txtIn1.setInputVerifier(new UrlVerifier());
		frmCygnusVersion.setTitle("Cygnus Version 1.0");
		frmCygnusVersion.setVisible(true);
		// create the status bar panel and shove it down the bottom of the frame

	}

	private void Stop()
	{
		if (server != null)
		{
			server.Stop();
		}

		if (client != null)
		{
			client.Stop();
		}
	}

	@Override
	public void UpdateStatus(String status)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{

				@Override
				public void run()
				{
					UpdateStatus(status + System.getProperty("line.separator"));
				}
			});
			return;
		}
		// Now edit your gui objects
		textArea.append(status);
	}

	@Override
	public void onConnectionChange(Boolean status)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{

				@Override
				public void run()
				{
					onConnectionChange(status);
				}
			});
			return;
		}
		// Now edit your gui objects
		/*
		 * if (status) { btnStart.setBackground(Color.GREEN); } else {
		 * btnStart.setBackground(Color.GRAY); }
		 */

	}

	@Override
	public void OperationCompleted()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{

				@Override
				public void run()
				{
					OperationCompleted();
				}
			});
			return;
		}
		isRunning = false;
		// Now edit your gui objects
		txtIn1.setBackground(Color.WHITE);
		txtIn2.setBackground(Color.WHITE);
		// txtOut1.setBackground(Color.WHITE);
		// txtOut2.setBackground(Color.WHITE);
	}

	public JTextArea getTextArea()
	{
		return textArea;
	}

	@Override
	public void OperationStarted()
	{
		if (isRunning)
		{
			if (!SwingUtilities.isEventDispatchThread())
			{
				SwingUtilities.invokeLater(new Runnable()
				{

					@Override
					public void run()
					{
						OperationStarted();
					}
				});
				return;
			}
			// Now edit your gui objects
			// txtIn1.setBackground(Color.ORANGE);
			// txtIn2.setBackground(Color.ORANGE);
		}
	}

	@Override
	public void UpdateStatistics(long rx1Bytes, long rx2Bytes, long txBytes)
	{

		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{

				@Override
				public void run()
				{
					UpdateStatistics(rx1Bytes, rx2Bytes, txBytes);
				}
			});
			return;
		}
		// Now edit your gui objects
		lblIn1Counter.setText(Long.toString(rx1Bytes));
		lblIn2Counter.setText(Long.toString(rx2Bytes));
		labelOut.setText(Long.toString(txBytes));
	}

	@Override
	public void OrionConnectionStatus(boolean status)
	{
		ChangeLabelBackground(lblIn1Counter, status);
	}

	@Override
	public void E1Port1ConnectionStatus(boolean status)
	{
		ChangeLabelBackground(lblIn2Counter, status);
		
	}

	@Override
	public void E1Port2ConnectionStatus(boolean status)
	{
		ChangeLabelBackground(lblOutpoutbytes, status);
	}

	@Override
	public void Port1QueueSize(int Qsize)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void Port2QueueSize(int Qsize)
	{
		// TODO Auto-generated method stub
		
	}
	
	private void ChangeLabelBackground(JLabel control, boolean status)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{

				@Override
				public void run()
				{
					ChangeLabelBackground(control, status);
				}
			});
			return;
		}
		
		if (status) 
		{
			if (control.getBackground() != Color.GREEN)
			{
				control.setBackground(Color.GREEN);
			}
		}
		else
		{
			if (control.getBackground() != Color.RED)
			{
				control.setBackground(Color.RED);
			}
		}
	}
}
