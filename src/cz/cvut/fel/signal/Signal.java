package cz.cvut.fel.signal;

import java.io.FileInputStream;
import java.io.InputStream;

public class Signal {
	private long length;
	private short[] data;
	int maximum;
	private double mean;
	private double variance;
	
	
	public Signal() {
		// TODO Auto-generated constructor stub
		length = 0;
		data = null;
		mean = 0;
		variance = 0;
		
	}
	
	public Signal(FileInputStream inStream) {
		// TODO Auto-generated constructor stub
	}
	
	public static double[] autoCorrelation(short[] data_in) {
		  int length;
		  length = data_in.length;
		  double[] data = null;
		  double[] corr = null;
		  int shift;		
		  double R = 0;
		  
		  data = new double[3*length-2];
		  corr = new double[2*length];
		  
		  for(int i = 0; i<3*length-2;i++){
			  if(i<length||i>2*length-1){
				  data[i] = 0;
			  } else {
				  data[i] = data_in[i-length];
			  }
		  }
		  /*
		  for(int i = 0; i<2*length; i++){	  
			  corr[i] = 0;
		  }*/
		  
		  for (shift = 0; shift<2*length-1; shift++){
		    for(int j = 0; j<length;j++){
		      if (data[j+shift]==0 || data_in[j]==0) 
		    	  continue; 
		      else
		      R += data[j+shift]*data_in[j];
		    }
		    corr[shift] = R;
		    R = 0;
		  }						
		return corr;
	}
	
	/**Calculates the mean value of the signal and saves it in the class variable.*/
	public void calculateMean() {		
		for (short s : data) {
			mean += s;
		}
		mean /= data.length;		
	} // Calculate mean
	
	/** Calculates the standard deviation of the data.*/
	public void calculateStd() {
		double std = 0;				
		
		for (short s : data) {
			std += ((s-mean)*(s-mean));
		}		
		std /= (data.length-1);
		variance = Math.sqrt(std);		
	} // calcStd
	
	public double getMean(){
		return mean;
	} // getMean

	public double getVariance(){
		return variance;
	} // getMean
}
