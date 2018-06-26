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
				WriteParameter(name, defaultValue);
				value = defaultValue;
		}
		return value;
	}

	public static boolean Set(String name, String Value) throws IOException
	{
		if (props == null)
		{
			Init();
		}

		String currentValue = Get(name);
		if (currentValue != Value)
		{
			return WriteParameter(name, Value);
		}
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
	
	private static boolean WriteParameter(String name, String Value)
	{
		props.setProperty(name, Value);
		try
		{
			FileWriter writer = new FileWriter(configFile);
			props.store(writer, "Cygnus settings");
			writer.close();
		}
		catch (IOException e)
		{
			logger.error("Failed to write parameter to the file",e);
			return false;
		}
		return true;
	}
}
