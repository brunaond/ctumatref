package cz.cvut.fel.wavrecordtest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.EventListener;
import java.util.Calendar;
import java.math.*;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.CorrectionInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import cz.cvut.fel.wavrecordtest.R.id;
import cz.cvut.fel.wavrecordtest.simpleMath;
import android.view.View.OnClickListener;


public class WavRecordTest extends Activity {
	private MediaPlayer mPlayer = null;
	private AudioRecord mRecord = null;
	private static final int RECORDER_SAMPLERATE = 44100;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private Thread recordingThread = null;
	private boolean isRecording = false;
	int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
	int BytesPerElement = 2; // 2 bytes in 16bit format
	private static String filePath = null;
	private static String file_wav = null;
	private Calendar cal;
	double[] data_sub = null;
	double[] impedance = null;
	double[] frequency = null;
	int noiseStart;
	final int NOISE_CUTOFF = 12000;
	//short[] data_sub = null;
	double distance;
	
	private void startPlaying() {
		mPlayer = new MediaPlayer();
		Uri path = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test);	
		try {
			mPlayer.setDataSource(getBaseContext(), path);
			mPlayer.prepare();
			mPlayer.start();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	private void startRecording() {

/*	    mRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
	            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
	            RECORDER_AUDIO_ENCODING, AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
			            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING));

	    mRecord.startRecording();*/
	    isRecording = true;
	    recordingThread = new Thread(new Runnable() {
	        public void run() {
	            writeAudioDataToFile();
	        }
	    }, "AudioRecorder Thread");
	    recordingThread.start();
	    TextView mText = (TextView) findViewById(R.id.status_text_view);
	    mText.setText("Recording now...");
	}		
	
	private void stopRecording() {
	    // stops the recording activity

	    if (null != mRecord) {
	        isRecording = false;
	        mRecord.stop();
	        mRecord.release();
	        mRecord = null;
	        
	    }
	}

	
	private void plotData() {
		Long length = null;
		int lengthNoise;
		int lengthBurst;
		int[] indices;
		byte[] buffer = null;
		byte[] bufferNoise = null;
		byte[] bufferBurst = null;
		short[] plotData = null;
		short[] noiseData = null;
		short[] burstData = null;
		short[] burstRegion = null;
		short[] noiseExtracted = null;
		FileInputStream is = null;
		InputStream noiseTemplate = null;
		InputStream burstTemplate = null;
		double[] data_corr = null;
		double[] data_corr_butter = null;
		//double[] data_sub = null;
		double time;
		//double distance;
		int maximum;
		int peak;	
		distance = 0;
				
		TextView mText = (TextView) findViewById(R.id.status_text_view);
				
		if (filePath==null){ 			
			mText.setText("No file recorded yet");
			Log.d("PLR", "No file recorded yet");
			return;		
		}
		try {
		is = new FileInputStream(filePath);		
		length = is.getChannel().size();
		buffer = new byte[length.intValue()];
		plotData = new short[length.intValue()];				
		is.read(buffer);		
		
		
		//Uri pathNoise = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.noise);		
		noiseTemplate = this.getResources().openRawResource(R.raw.noise);
		burstTemplate = this.getResources().openRawResource(R.raw.burst6);
		lengthNoise = noiseTemplate.available();
		lengthBurst = burstTemplate.available();
		//lengthNoise = noiseTemplate.getChannel().size();
		Log.d("PLR", "Size of noise is: " + String.valueOf(lengthNoise));
		Log.d("PLR", "Size of burst is: " + String.valueOf(lengthBurst));
		bufferNoise = new byte[lengthNoise];		
		bufferBurst = new byte[lengthBurst];
		noiseTemplate.read(bufferNoise);	
		burstTemplate.read(bufferBurst);
		
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}		
		plotData = byte2short(buffer);
		Log.d("PLR", "plotData converted successfully.");
		noiseData = byte2short(bufferNoise);
		Log.d("PLR", "noiseData converted successfully.");
		burstData = byte2short(bufferNoise);
		Log.d("PLR", "burstData converted successfully.");
		// The noise is recognized to be between following indices: 4300 and 22100
		//noiseExtracted = simpleMath.getNoiseStart(noiseData, plotData);		
		noiseStart = simpleMath.getNoiseStartIndex(noiseData, plotData);
		noiseExtracted = simpleMath.getSubsequent(noiseStart, noiseStart+NOISE_CUTOFF, plotData);
		Log.d("PLR", "Noise extracted successfully.");
		// Detecting the regions with bursts - there should be no more than as many bursts as in the test file 
		// burstRegion = simpleMath.getBurstRegion(noiseData, plotData);
		burstRegion = simpleMath.getSubsequent(noiseStart+NOISE_CUTOFF, plotData.length-1, plotData);
		Log.d("PLR", "Burst region extracted...");
		indices = simpleMath.getBurstIndices(burstRegion); // returns burst indices from the data without noise
		Log.d("PLR", "Burst indices obtained.");
		
		data_corr = new double[2*noiseExtracted.length];
		data_corr = simpleMath.correlation_fast(noiseExtracted);
		Log.d("PLR", "Correlation computed.");
		//drawPlot(data_corr);
		data_corr = simpleMath.abs(data_corr);		
		Log.d("PLR", "Abs computed.");
		data_corr_butter = simpleMath.butterworth(data_corr);
		Log.d("PLR", "Filter applied.");
		//drawPlot(data_corr_butter);
		//Log.d("PLR", "Data plotted.");
		
		peak = simpleMath.localMax(data_corr_butter);
		data_sub = simpleMath.getSubsequent(peak+50, peak+140, data_corr_butter);		
		maximum = simpleMath.localMax(data_sub);
		time = (double)(maximum+50)/RECORDER_SAMPLERATE;
		distance = (time*340/2)*100;
		
		TextView bursts = (TextView) findViewById(R.id.busrsts_detected_text_view);
		bursts.setText("#Bursts: "+ indices.length);
		impedance = new double[indices.length];
		frequency = new double[indices.length];
		int i = 0;
		
 		for (int d : indices) { 			
 			// It is expected that the burst length is 4ms resulting in 200 samples. Allowing twice the size for safe detection.
			burstData = simpleMath.getSubsequent(d, d+400, burstRegion);
			
			// drawPlotClean(burstData);
			
			impedance[i] = simpleMath.calcImpedance(burstData, 0.15, distance, 0.0 );
			Log.d("PLR", "Impedance calculated");
			frequency[i] = simpleMath.calcFrequency(simpleMath.getSubsequent(d, d+197, burstRegion));
			Log.d("PLR", "Frequency calculated");
			i++;
		}
		
		viz();
		writeLog(indices);
	}	
	
	private void writeLog(int[] burstsIndices){
		String fileLog = null;
		//fileLog = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
		if (file_wav == null){
			return;
		}
		fileLog = file_wav;
		fileLog += ".log";
		FileOutputStream os;
		PrintWriter writer;
		
        	
        try {            
			os = new FileOutputStream(fileLog);
			writer = new PrintWriter(os);            
    		writer.write("Distance:" + Double.toString(distance) + "\n");
    		writer.write("Bursts detected:" + Integer.toString(burstsIndices.length) + "\n");
    		writer.write("Impedance calculated at region from index d to d+200 samples\n");
    		writer.write("Noise starts at: " + Integer.toString(noiseStart) + "\n");    
    		writer.write("Noise cutoff is: " + Integer.toString(NOISE_CUTOFF) + "\n\n");    		
			writer.write("===================================\n");
			
    		for (int i = 0; i < impedance.length; i++) {
				writer.write("Impedance calculated:" + Double.toString(impedance[i]) + "\n");
				writer.write("Frequency calculated:" + Double.toString(frequency[i]) + "\n");
				writer.write("Burst index number:" + Double.toString(burstsIndices[i]) + "\n");
				writer.write("===================================\n");
			}    		
    		
    		writer.close();        	    		
        } catch (IOException ex) {
        	ex.printStackTrace();
        }
	}
	
	
	private void viz(){
		TextView mText = (TextView) findViewById(R.id.status_text_view);			
		mText.setText("Distance: "+distance+" cm");
		drawPlot(frequency, impedance);
	}
	
	private byte[] short2byte(short[] sData) {
	    int shortArrsize = sData.length;
	    byte[] bytes = new byte[shortArrsize * 2];
	    for (int i = 0; i < shortArrsize; i++) {
	        bytes[i * 2] = (byte) (sData[i] & 0x00FF);
	        bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
	        sData[i] = 0;
	    }
	    return bytes;
	}

	private short[] byte2short(byte[] bData) {
		short[] sData;
		short sDataTemp;
		int bDataLength = bData.length;
		int sDataLength;
		
		if (bDataLength%2>0) sDataLength=bDataLength/2+1;
		else sDataLength = bDataLength/2;
		
		sData = new short[sDataLength];
		
		for(int i=0;i<sDataLength;i++) {
			sData[i] = 0x0000;
			sDataTemp = 0x0000;
			sData[i] = bData[i*2+1];
			sData[i] = (short) (sData[i]<<8);
			sDataTemp = bData[i*2];
			sData[i] = (short) (sData[i]|sDataTemp);			
		}
		return sData;
	}
	
	private void writeAudioDataToFile() {
	    // Write the output audio in byte
		cal = Calendar.getInstance();
		int sec = cal.get(Calendar.SECOND);
		int min = cal.get(Calendar.MINUTE);
		int h = cal.get(Calendar.HOUR_OF_DAY);
		int d = cal.get(Calendar.DAY_OF_MONTH);
		int m = cal.get(Calendar.MONTH)+1; // Months are counted from 0 = January.
		int y = cal.get(Calendar.YEAR);
		byte[] header=null;
		Long length;
		byte[] buffer;
		
		filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        filePath += "/temp"+".pcm";	    
	    short sData[] = new short[BufferElements2Rec];

	    FileOutputStream os = null;
	    FileInputStream is = null;
	    
	    try {
	        os = new FileOutputStream(filePath);	     
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    }
	    
	    while (isRecording) {
	        // gets the voice output from microphone to byte format

	        mRecord.read(sData, 0, BufferElements2Rec);
	        //System.out.println("Short writing to file" + sData.toString());
	        try {
	            // // writes the data to file from buffer
	            // // stores the voice buffer
	            byte bData[] = short2byte(sData);
	            os.write(bData, 0, BufferElements2Rec * BytesPerElement);	 
	            if (!mPlayer.isPlaying()) {
	             	stopRecording();
	            }
	                	            
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }	    	    
	    try {	    	
	        os.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }	   
		try {
		//file_wav = Environment.getExternalStorageDirectory().getAbsolutePath();
		file_wav = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
        file_wav += "/rec_"+y+"-"+m+"-"+d+"-"+h+min+sec+".wav";		
        os = new FileOutputStream(file_wav);
		is = new FileInputStream(filePath);
		length = is.getChannel().size();
		buffer = new byte[length.intValue()];						
		is.read(buffer);		
		header = assembleHeader(length);
		os.write(header);
		os.write(buffer);
		os.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}		    
			
	}
	
	private byte[] assembleHeader(long l) {
		// TODO Auto-generated method stub
		Long size = l;
		byte[] RIFF = {0x52, 0x49, 0x46, 0x46};
		byte[] header_1_size = int2byte(size.intValue()-8);
		byte[] WAVE = {0x57, 0x41, 0x56, 0x45};
		byte[] fmt = {0x66, 0x6D, 0x74};
		byte[] format = {0x20, 0x10, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x44, (byte) 0xAC, 0x00, 0x00, (byte) 0x88, 0x58, 0x01, 0x00, 0x02, 0x00, 0x10, 0x00};
		byte[] DATA = {0x64, 0x61, 0x74, 0x61};
		byte[] header_2_size = int2byte(size.intValue());
		byte[] write_d = new byte[44];
		
		write_d[0] = RIFF[0];
		write_d[1] = RIFF[1];
		write_d[2] = RIFF[2];
		write_d[3] = RIFF[3];
		write_d[4] = header_1_size[0];
		write_d[5] = header_1_size[1];
		write_d[6] = header_1_size[2];
		write_d[7] = header_1_size[3];
		write_d[8] = WAVE[0];
		write_d[9] = WAVE[1];
		write_d[10] = WAVE[2];
		write_d[11] = WAVE[3];
		write_d[12] = fmt[0];
		write_d[13] = fmt[1];
		write_d[14] = fmt[2];
		write_d[15] = format[0];
		write_d[16] = format[1];
		write_d[17] = format[2];
		write_d[18] = format[3];
		write_d[19] = format[4];
		write_d[20] = format[5];
		write_d[21] = format[6];
		write_d[22] = format[7];
		write_d[23] = format[8];
		write_d[24] = format[9];
		write_d[25] = format[10];
		write_d[26] = format[11];
		write_d[27] = format[12];
		write_d[28] = format[13];
		write_d[29] = format[14];
		write_d[30] = format[15];
		write_d[31] = format[16];
		write_d[32] = format[17];
		write_d[33] = format[18];
		write_d[34] = format[19];
		write_d[35] = format[20];
		write_d[36] = DATA[0];
		write_d[37] = DATA[1];
		write_d[38] = DATA[2];
		write_d[39] = DATA[3];
		write_d[40] = header_2_size[0];
		write_d[41] = header_2_size[1];
		write_d[42] = header_2_size[2];
		write_d[43] = header_2_size[3];						
		
		return write_d;
	}
	
	private byte[] int2byte(int sData) {	    
	    byte[] bytes = new byte[4];

	        bytes[0] = (byte) (sData & 0x000000FF);
	        bytes[1] = (byte) ((sData >> 8) & 0x000000FF);
	        bytes[2] = (byte) ((sData >> 16) & 0x000000FF);
	        bytes[3] = (byte) ((sData >> 24) & 0x000000FF);
	        	    
	    return bytes;
	}
	
	private void playAndRecord(){
	    mRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
	            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
	            RECORDER_AUDIO_ENCODING, AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
			            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING));	    
	    if (mRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {	    	
			Log.d("PLR", "Player not yet initialized");
		} else {
			Log.d("PLR", "Player successfully initialized");
		}
	    mRecord.startRecording();	   
		startPlaying();
		startRecording();						
	}
	
	public void drawPlot(double[] plotData) {
		XYPlot plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
		Number[] series1Numbers, domainNumbers;
		series1Numbers = new Number[plotData.length];
		domainNumbers = new Number[plotData.length];
		int i = 0;
		int start = 50;		
        // Create a couple arrays of y-values to plot:
		for (double s : plotData) {
			series1Numbers[i]=s;
			domainNumbers[i]=0.375*(start + i)+1.25;
			i++;
		}
        //Number[] series1Numbers = {1, 8, 5, 2, 7, 4};
        //Number[] series2Numbers = {4, 6, 3, 8, 2, 10};                

        // Turn the above arrays into XYSeries':
        XYSeries series1 = new SimpleXYSeries(
        		Arrays.asList(domainNumbers),          // SimpleXYSeries takes a List so turn our array into a List
                Arrays.asList(series1Numbers), // Y_VALS_ONLY means use the element index as the x value
                "Series1");                             // Set the display title of the series
 
        // same as above
        //XYSeries series2 = new SimpleXYSeries(Arrays.asList(series2Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series2");
 
        // Create a formatter to use for drawing a series using LineAndPointRenderer
        // and configure it from xml:
        LineAndPointFormatter series1Format = new LineAndPointFormatter();     
        series1Format.setPointLabelFormatter(new PointLabelFormatter());
        series1Format.configure(getApplicationContext(),
                R.xml.line_formatter_1);
 
        // add a new series' to the xyplot:
        plot.addSeries(series1, series1Format);
 
        // same as above:
        /*LineAndPointFormatter series2Format = new LineAndPointFormatter();
        series2Format.setPointLabelFormatter(new PointLabelFormatter());
        series2Format.configure(getApplicationContext(),
                R.xml.line_formatter_2);
        plot.addSeries(series2, series2Format);
*/
 
        // reduce the number of range labels
        plot.setTicksPerRangeLabel(3);
        plot.getGraphWidget().setDomainLabelOrientation(-45);
        plot.redraw();
		
	}
	
	public void drawPlot(double[] xData, double[] yData) {
		XYPlot plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
		Number[] series1Numbers, domainNumbers;
		if (xData.length != yData.length){
			return;
		}
		series1Numbers = new Number[xData.length];
		domainNumbers = new Number[yData.length];
		
        // Create a couple arrays of y-values to plot:
		for (int i = 0; i< xData.length; i++) {
			series1Numbers[i]=yData[i];
			domainNumbers[i]=xData[i];			
		}
        //Number[] series1Numbers = {1, 8, 5, 2, 7, 4};
        //Number[] series2Numbers = {4, 6, 3, 8, 2, 10};                

        // Turn the above arrays into XYSeries':
        XYSeries series1 = new SimpleXYSeries(
        		Arrays.asList(domainNumbers),          // SimpleXYSeries takes a List so turn our array into a List
                Arrays.asList(series1Numbers), // Y_VALS_ONLY means use the element index as the x value
                "Series1");                             // Set the display title of the series
 
        // same as above
        //XYSeries series2 = new SimpleXYSeries(Arrays.asList(series2Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series2");
 
        // Create a formatter to use for drawing a series using LineAndPointRenderer
        // and configure it from xml:
        LineAndPointFormatter series1Format = new LineAndPointFormatter();     
        series1Format.setPointLabelFormatter(new PointLabelFormatter());
        series1Format.configure(getApplicationContext(),
                R.xml.line_formatter_1);
 
        // add a new series' to the xyplot:
        plot.addSeries(series1, series1Format);        
 
        // same as above:
        /*LineAndPointFormatter series2Format = new LineAndPointFormatter();
        series2Format.setPointLabelFormatter(new PointLabelFormatter());
        series2Format.configure(getApplicationContext(),
                R.xml.line_formatter_2);
        plot.addSeries(series2, series2Format);
*/
 
        // reduce the number of range labels
        plot.setTicksPerRangeLabel(3);
        plot.getGraphWidget().setDomainLabelOrientation(-45);
        plot.redraw();
		
	}	
	
	public void drawPlotClean(short[] plotData) {
		XYPlot plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
		Number[] series1Numbers;
		series1Numbers = new Number[plotData.length];
		int i = 0;
        // Create a couple arrays of y-values to plot:
		for (double s : plotData) {
			series1Numbers[i]=s;
			i++;
		}
 
        // Turn the above arrays into XYSeries':
        XYSeries series1 = new SimpleXYSeries(
                Arrays.asList(series1Numbers),          // SimpleXYSeries takes a List so turn our array into a List
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // Y_VALS_ONLY means use the element index as the x value
                "Series1");                             // Set the display title of the series
 
  
        // Create a formatter to use for drawing a series using LineAndPointRenderer
        // and configure it from xml:
        LineAndPointFormatter series1Format = new LineAndPointFormatter();     
        series1Format.setPointLabelFormatter(new PointLabelFormatter());
        series1Format.configure(getApplicationContext(),
                R.xml.line_formatter_1);
 
        // add a new series' to the xyplot:
        plot.addSeries(series1, series1Format);
  
        // reduce the number of range labels
        plot.setTicksPerRangeLabel(3);
        plot.getGraphWidget().setDomainLabelOrientation(-45);
        plot.redraw();
		
	}
	
	public void drawPlotClean(int[] plotData) {
		XYPlot plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
		Number[] series1Numbers;
		series1Numbers = new Number[plotData.length];
		int i = 0;
        // Create a couple arrays of y-values to plot:
		for (double s : plotData) {
			series1Numbers[i]=s;
			i++;
		}
 
        // Turn the above arrays into XYSeries':
        XYSeries series1 = new SimpleXYSeries(
                Arrays.asList(series1Numbers),          // SimpleXYSeries takes a List so turn our array into a List
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // Y_VALS_ONLY means use the element index as the x value
                "Series1");                             // Set the display title of the series
 
  
        // Create a formatter to use for drawing a series using LineAndPointRenderer
        // and configure it from xml:
        LineAndPointFormatter series1Format = new LineAndPointFormatter();     
        series1Format.setPointLabelFormatter(new PointLabelFormatter());
        series1Format.configure(getApplicationContext(),
                R.xml.line_formatter_1);
 
        // add a new series' to the xyplot:
        plot.addSeries(series1, series1Format);
  
        // reduce the number of range labels
        plot.setTicksPerRangeLabel(3);
        plot.getGraphWidget().setDomainLabelOrientation(-45);
        plot.redraw();
		
	}		
	
	public void writeTest(){
		
	}


		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.layout);
			Button mPlay = (Button) findViewById(R.id.play_button);
			mPlay.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					startPlaying();					
				}
			});
						
			Button mPlayAndRec = (Button) findViewById(R.id.play_and_record_button);
			mPlayAndRec.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					playAndRecord();
				}
			});
			
			Button mPlot = (Button) findViewById(R.id.plot_button);	
			mPlot.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub					
					plotData();
					/*
					short[] data_in = {0, 0, 0, 0, 1, 1, 1, 0, 0, 0};
					double[] plot_data = new double[data_in.length];
					plot_data = simpleMath.correlation_fast(data_in);					
					drawPlot(plot_data);*/
				}
			});
			distance = 0;
		}	
				

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		//plotData();
	}		


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.wav_record_test, menu);
		return true;
	}
	
}
