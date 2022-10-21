package team.hobbyrobot.subos;

/**
 * Datovy typ diky kteremu se da predavat reference na promennou.
 * 
 * @author David Krcmar
 * @version 1.0
 * @param <T> Datovy typ promenne
 */
public class Referenceable<T>
{
	/** Aktualni hodnota */
	private Object value;

	public Referenceable(T value)
	{
		this.value = value;
	}

	/** Uloz hodnotu */
	public void setValue(T value)
	{
		this.value = value;
	}

	/** Nacti hodnotu */
	public T getValue()
	{
		return (T) value;
	}
}
