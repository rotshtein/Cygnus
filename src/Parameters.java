import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;

public final class Parameters
{

	static Logger		logger		= Logger.getLogger("Parameters");
	static File			configFile	= null;
	static Properties	props;

	private static void Init()
	{
		try
		{
			configFile = new File("config.properties");
			if (!configFile.exists())
			{
				if (!configFile.createNewFile())
				{
					throw (new Exception("No configuration file"));
				}
			}
			FileReader config = new FileReader(configFile);
			props = new Properties();
			props.load(config);
			config.close();
		}
		catch (Exception e)
		{
			logger.error("Failed to open configuration file \"" + configFile.getAbsolutePath() + "\"", e);
		}
	}

	public static String Get(String name)
	{
		return Get(name, "");
	}

	public static String Get(String name, String defaultValue)
	{
		if (props == null)
		{
			Init();
		}

		String value = "";
		try
		{
			value = props.getProperty(name);
		}
		catch (Exception e)
		{
			logger.error("Error getting property", e);
		}

		if (value == null)
		{
			try
			{
				Set(name, defaultValue);
				value = defaultValue;
			}
			catch (IOException e)
			{
				logger.error("Error setting property", e);
			}
		}
		return value;
	}

	public static boolean Set(String name, String Value) throws IOException
	{
		if (props == null)
		{
			Init();
		}

		props.setProperty(name, Value);
		FileWriter writer = new FileWriter(configFile);
		props.store(writer, "mediation settings");
		writer.close();
		return true;
	}

	public static String getFilename()
	{
		if (props == null)
		{
			Init();
		}
		return configFile.getAbsolutePath();
	}

}
