package team.hobbyrobot.tdn.core;

class ArrayUtils
{
	public static <T> int indexOf(T[] array, T val)
	{
		for(int i = 0; i < array.length; i++)
			if(array[i].equals(val))
				return i;
		return -1;
	}
}
