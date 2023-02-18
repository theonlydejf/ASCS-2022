package team.hobbyrobot.utils;

public class ProgressReporter
{
	private Object _reportChangedLock = new Object();
	private boolean _reportChanged;
	
	private float _progress;
	private String _message;
	private boolean _done;
	
	public void setDone()
	{
		_done = true;
		setReportChanged();
	}
	
	public boolean isDone()
	{
		return _done;
	}
	
	public boolean checkReportChanged()
	{
		synchronized(_reportChangedLock)
		{
			if(_reportChanged)
			{
				_reportChanged = false;
				return true;
			}
			return false;
		}
	}
	public void setReportChanged()
	{
		synchronized(_reportChangedLock)
		{
			_reportChanged = true;
		}
	}
	public float getProgress()
	{
		return _progress;
	}
	public void setProgress(float _progress)
	{
		this._progress = _progress;
	}
	public String getMessage()
	{
		return _message;
	}
	public void setMessage(String _message)
	{
		this._message = _message;
	}
	
	
}
