package team.hobbyrobot.subos.hardware;

import lejos.robotics.Encoder;
import lejos.robotics.SampleProvider;

public class EncoderSampleProvider implements SampleProvider, Encoder
{
	private Encoder encoder;
	
	public EncoderSampleProvider(Encoder encoder)
	{
		this.encoder = encoder;
	}
	
	@Override
	public int sampleSize()
	{
		return 1;
	}

	@Override
	public void fetchSample(float[] sample, int offset)
	{
		sample[offset] = encoder.getTachoCount();
	}

	@Override
	public int getTachoCount()
	{
		return encoder.getTachoCount();
	}

	@Override
	public void resetTachoCount()
	{
		encoder.resetTachoCount();
	}

}
