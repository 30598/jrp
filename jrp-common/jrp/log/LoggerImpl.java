package jrp.log;

import jrp.util.Util;

public class LoggerImpl implements Logger
{
	private boolean enableLog = true;

	public boolean isEnableLog()
	{
		return enableLog;
	}

	public LoggerImpl setEnableLog(boolean enableLog)
	{
		this.enableLog = enableLog;
		return this;
	}

	@Override
	public synchronized void log(String fmt, Object... args)
	{
		if(enableLog)
		{
			System.out.printf("[%s] %s\n", Util.getTime(), String.format(fmt, args));
		}
	}

	@Override
	public synchronized void err(String fmt, Object... args)
	{
		if(enableLog)
		{
			System.err.printf("[%s] %s\n", Util.getTime(), String.format(fmt, args));
		}
	}
}
