package testsimple.main;

import java.io.IOException;
import java.util.Scanner;

public class Main
{
	public static void main(String[] args) throws Exception
	{
        String path = "..\\soundpacks\\";
        
        
	    Track track = new Track(path + "L1.mp3");
	    track.start();
	    track.loop(true);
	    
	    Scanner sc = new Scanner(System.in);
	    int val = 1;
	    System.out.println("Volume max :" + track.getVolumeMax());
	    System.out.println("Volume min :" + track.getVolumeMin());
	    while(val != -1)
	    {
	    	System.out.println("Volume à " + track.getVolume());
	    	System.out.println("Gain à " + track.gainControl.getValue());
	    	val = sc.nextInt();
	    	track.fadeout(val);
	    }
	    
	    
	    
	    
	    enter("Enter to fade out");
	    track.fadeout();
	    //track3.start();
	    enter("Enter to stop");
	    track.stop();
	    enter("Enter to play again");
	    track.start();
	    enter("Enter to end");
	    System.exit(-1);
	}
	
	public static void enter(String msg)
	{
		System.out.println(msg);
		try {
            int read = System.in.read(new byte[2]);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	
}
