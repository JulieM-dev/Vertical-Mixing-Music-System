package testsimple.main;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;

import static javax.sound.sampled.AudioSystem.getAudioInputStream;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

public class Track
{
	private AtomicBoolean running = new AtomicBoolean(false);
	private AtomicBoolean mute = new AtomicBoolean(false);
	private AtomicBoolean loop = new AtomicBoolean(false);
	
	private AtomicInteger loopN = new AtomicInteger(-1);
	
	Thread thread = null;
	
	private String file;
	private AudioFormat outFormat;
	private Info info;
	private SourceDataLine line;
	private AudioInputStream in = null;
	FloatControl gainControl;
	
	
	public Track(String file)
	{
		this.file = file;
		try 
		{
			init();
		} catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	private void init() throws UnsupportedAudioFileException, IOException, LineUnavailableException
	{
		in = getAudioInputStream(new File(file).getAbsoluteFile());
		outFormat = getOutFormat(in.getFormat());
		System.out.println(outFormat.getProperty("bitrate"));
        info = new Info(SourceDataLine.class, outFormat);
        line = (SourceDataLine) AudioSystem.getLine(info);
        AudioFileFormat form = AudioSystem.getAudioFileFormat(new File(file));
        System.out.println(form.properties());
        
        System.out.println(outFormat.getFrameRate()); // num of frames / sec
        System.out.println(outFormat.getFrameSize());  // size of a frame in bytes
        System.out.println(outFormat.getSampleRate()); // num of samples / sec
        System.out.println(outFormat.getSampleSizeInBits()); // size of a sample in bits
        
        System.out.println(in.available());
        //in.skip(410*1411*1000/8); // J'ai besoin du nombre de bytes / secondes
        System.out.println(in.available());
        
		//clip = AudioSystem.getClip();
		//clip.open(audioInputStream); 
		//gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
	}
	
	
	public void start()
	{
        if(line != null && !running.get())
        {
			try {
				line.open(outFormat);
				line.start();
				gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
				gainControl.setValue((float) 5.0);
	            initThread();
	            //in.skip((60*320*1000)/8);
	            running.set(true);
	            thread.start();
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
	}
	
	public void loop(boolean loop)
	{
		this.loop.set(loop);
	}
	
	// 0 = end of the loop, -1 = infinite
	public void loop(int number)
	{
		if(number >= -1)
		{
			this.loop.set(true);
			loopN.set(number);
		}
	}
	
	public void pause()
	{
		if(line != null && running.get())
        {
			running.set(false);			
        }
	}
	
	public void stop()
	{
		if(line != null && running.get())
        {
			running.set(false);	
			try {
				in.reset();
			} catch (IOException e) {
				forceStop();
			}
        }
	}
	
	public void forceStop()
	{
		running.set(false);
		try 
		{
			init();
		} catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public void mute(boolean mute)
	{
		BooleanControl bc = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
		
		if (bc != null) 
		{
			bc.setValue(mute); // true to mute the line, false to unmute
		}
	}
	
	//Volume 0.0F = 0%
	// 1.0F = 100%
	// 1.07F = Max
	public void setVolume(Float volume)
	{
		float value = (float) (1/(Math.log(2)/Math.log(volume))*20);
		if(value > gainControl.getMaximum())
		{
			value = gainControl.getMaximum();
		}
		else if(value < gainControl.getMinimum())
		{
			value = gainControl.getMinimum();
		}
		gainControl.setValue(value);
	}
	
	public float getVolume()
	{
		return (float) Math.pow(2.0, gainControl.getValue()/20.0);
	}
	
	public float getVolumeMax()
	{
		return (float) Math.pow(2.0, gainControl.getMaximum()/20.0);
	}
	
	public float getVolumeMin()
	{
		return (float) Math.pow(2.0, gainControl.getMinimum()/20.0);
	}
	
	private AudioFormat getOutFormat(AudioFormat inFormat) {
        final int ch = inFormat.getChannels();

        final float rate = inFormat.getSampleRate();
        return new AudioFormat(PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
    }
	
    private void initThread() 
    {
    	thread = new Thread() 
		{
			public void run()
			{
				do
				{
					play();
					if(loop.get())
					{
						forceStop();
						try {
							line.open(outFormat);
							line.start();
							gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
							running.set(true);
						} catch (LineUnavailableException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					
				}while(loop.get() && running.get());
		        line.drain();
		        line.stop();
		        running.set(false);
			}
			
			public void play()
			{
				AudioInputStream ins = getAudioInputStream(outFormat, in);
				final byte[] buffer = new byte[4096];
		        try {
					for (int n = 0; n != -1 && running.get(); n = ins.read(buffer, 0, buffer.length)) 
					{
						line.write(buffer, 0, n);
					}
					System.out.println("End");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	    };
    }
    
    public void fadeout()
    {
    	fadeout(3000);
    }
    
    public void fadeout(int durationInMs)
    {
    	Thread fade = new Thread()
		{
    		public void run()
    		{
    			int time = durationInMs / 200;
    			float volumeDown = getVolume() / time;
    			for(float i = getVolume(); i > gainControl.getMinimum() && running.get() && time >= 0 ; i = (float) (i-volumeDown))
    			{
    				time--;
    				try
					{
    					setVolume(i);
						sleep(200);
						
					} catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    		}
		};
		fade.start();
    }
    
    
    public boolean isRunning()
    {
    	return running.get();
    }
}

