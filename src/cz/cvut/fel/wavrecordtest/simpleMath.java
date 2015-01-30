package cz.cvut.fel.wavrecordtest;

import android.util.Log;

// Energy calculation of reflected wave:
// energy(k,2) = (energy(k,1)*4*pi*D_mic_loudspeaker^2)/(4*pi*((D_mic_loudspeaker + 2*(D_mic_table-Thickness_sample)))^2);

public class simpleMath{
	final static double PI = 3.14;
	final static int SAMPLING_FREQ = 44100;
	public static final double[] ALPHA = {2.2877,1.6509,1.7962,1.1580,0.9992,1.0110,1.0097,0.9822,0.8065,0.8297,0.8215,0.8206,0.8687,0.9528,1.0751,0.5372};
	public static final double[] COEFFA = {55.0833,12.4888,54.0666,10.4352,3.0666,3.0303,2.9293,2.7145,1.7792,2.0170,1.9585,2.0740,2.1382,2.2571,3.5222,0.1455};
	//public static final int[] samples16bursts = {372, 292, 224, 175, 141, 112, 88, 70, 56, 45, 35, 28, 23, 17, 14, 11};
	public static final double[] FREQUENCIES = {118, 151, 196, 252, 313, 394, 501, 630, 787, 980, 1260, 1575, 1917, 2594, 3150, 4009};
	/* Calculation with equivalent number of samples without period detection.
  alpha = {2.9207, 2.1048, 1.7469, 1.5553, 1.5935, 1.6152, 1.6207, 1.5585, 1.3539, 1.3933, 1.3827, 1.3692, 1.4519, 1.5909, 1.7042, 1.6864}
  coeffA = {1706.9, 159.1, 59.5, 35.5, 38.8, 39.7, 39.6, 33.3, 18.1, 21.6, 20.9, 21.0, 25.0, 32.7, 52.1, 13.5},
  */
	public static double[] correlation(short[] data_in) {
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
	
	public static double calcImpedance(short[] inBurst, int burstNumber, double D_mic_loudspeaker, double D_mic_table, double Thickness_sample){
		double impedance;
		double energyIncident;
		double energyIncidentCalculated;
		double energyReflected;
		double noise = 0.0;
		energyIncident = getEnergy(getSubsequent(90, 190, inBurst));
		energyReflected = getEnergy(getSubsequent(300, 400, inBurst));
		//energyIncidentCalculated = (energyIncident*4*PI*(Math.pow(D_mic_loudspeaker,2)))/(4*PI*Math.pow((D_mic_loudspeaker + 2*(D_mic_table-Thickness_sample)),2));
		energyIncidentCalculated = (COEFFA[burstNumber]*energyIncident)/(Math.pow(D_mic_table, ALPHA[burstNumber]))+noise;
		impedance = energyIncidentCalculated/energyReflected;
		return impedance;
	}
	
	public static double calcFrequency(short[] inBurst){
		double freq;
		int[] indices = new int[inBurst.length];
		int k =0;
		int max;
		int[] temp, tempDiff;
		
		
		/* 		 
		 * Check for crossing zero and if it does cross zero, record the index to a new field.
		 */
		
		for (int i = 1; i < inBurst.length; i++) {
			if ((inBurst[i] > 0) && (inBurst[i-1]) < 0) {
				indices[k] = i;
				k++;
			} else if ((inBurst[i] < 0) && (inBurst[i-1] > 0)) {
				indices[k] = i;
				k++;
			}
		}

		temp = new int[k];
		tempDiff = new int[k];
		for (int i = 0; i < k; i++) {
			temp[i] = indices[i];
		}
		
		Log.d("PLR", "Freq - k: " + Integer.toString(k));
		
		if (k > 500) {
			tempDiff = diff(temp); // calculates the difference between all points in the array
			freq = SAMPLING_FREQ/getMean(tempDiff);
			// max = localMax(tempDiff); // finds the greatest difference in the array which is respective to the greatest frequency
			// min = localMin(tempDiff); // finds the smallest difference in the array
			// freq = SAMPLING_FREQ/tempDiff[max]; // calculating the frequency based on the number of samples			
		} else if(k > 1 && k < 500) {
			tempDiff = diff(temp); // calculates the difference between all points in the array
			max = localMax(tempDiff); // finds the greatest difference in the array which is respective to the greatest frequency
			freq = SAMPLING_FREQ/tempDiff[max]; // calculating the frequency based on the number of samples			
		} else {
			// in case only one crossing is detected the most likely frequency is returned
			freq = SAMPLING_FREQ/200;
		}

		return freq;
	}
	
	// This is not full x-correlation.
	public static short[] getNoise(short[] inTemplate, short[] inData ) {		
		double[] corr = null;
		int index;
		short[] noise = null;		
		
		corr = new double[inData.length];
		
		for (int i = 0; i < corr.length-inTemplate.length; i++) {			
			for (int j = 0; j < inTemplate.length; j++) {
				corr[i] += inData[i+j] * inTemplate[j];				
			}
		}
		
		index = localMax(corr);
		noise = getSubsequent(index, index+25000, inData);
		return noise;
	}
	
	// This is not full x-correlation.
	public static int getNoiseStartIndex(short[] inTemplate, short[] inData ) {		
		double[] corr = null;
		int index;
	
		
		corr = new double[inData.length];
		
		for (int i = 0; i < corr.length-inTemplate.length; i++) {			
			for (int j = 0; j < inTemplate.length; j++) {
				corr[i] += inData[i+j] * inTemplate[j];				
			}
		}
		
		index = localMax(corr);
		return index;
		
	}
	
	public static short[] getBurstRegion(short[] inTemplate, short[] inData ) {		
		double[] corr = null;
		int index;
		short[] burstRegion = null;		
		
		corr = new double[inData.length];
		
		for (int i = 0; i < corr.length-inTemplate.length; i++) {			
			for (int j = 0; j < inTemplate.length; j++) {
				corr[i] += inData[i+j] * inTemplate[j];				
			}
		}
		
		index = localMax(corr);
		burstRegion = getSubsequent(index+12000, inData.length-1, inData);
		return burstRegion;
		
	}
	
	public static double[] getBursts(short[] inTemplate, short[] inData ) {		
		double[] corr = null;
		
		corr = new double[inData.length];
		
		for (int i = 0; i < corr.length-inTemplate.length; i++) {			
			for (int j = 0; j < inTemplate.length; j++) {
				corr[i] += inData[i+j] * inTemplate[j];				
			}
		}
				
		return corr;
		
	}	

	// This is not full x-correlation.
	public static short[] getBurst(short[] inTemplate, short[] inData ) {		
		double[] corr = null;
		int index;
		short[] burst = null;		
		
		corr = new double[inData.length];
		
		for (int i = 0; i < corr.length-inTemplate.length; i++) {			
			for (int j = 0; j < inTemplate.length; j++) {
				corr[i] += inData[i+j] * inTemplate[j];				
			}
		}
		
		index = localMax(corr);
		burst = getSubsequent(index, index+600, inData);
		return burst;
		
	}
	/**Computes only half of correlation, we suppose that correlation is symmetric around origin*/
	public static double[] correlation_fast(short[] data_in) {
		  int length;
		  length = data_in.length;
		  double[] data = null;
		  double[] corr = null;		  	
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

		  for (int i = 0; i<length; i++){
		    for(int j = 0; j<=i;j++){		      
		      R += data[length+j]*data_in[length-1-i+j];
		    }
		    corr[i] = R;
		    corr[length-2-i+length] = R;
		    R = 0;
		  }						
		return corr;
	}
	
	public static double[] butterworth(double[] data) {
		int order;
		int length;
		double[] y;
		double[] x;
		double[] b =  {0.0013,    0.0051,    0.0076,    0.0051,    0.0013};
		double[] a =  {1.0000,   -2.8869,    3.2397,   -1.6565,    0.3240};			
		
		order = a.length;		
		length = data.length;
				
		x = new double[length];
		y = new double[length];
		x = data;
		
		if (order!= 5) return null;
		
		for(int i = 0; i<length; i++){
			y[i] = 0;
		}
				
		// from MATLAB a =  {1.0000,   -2.8869,    3.2397,   -1.6565,    0.3240};
		// from MATLAB b =  {0.0013,    0.0051,    0.0076,    0.0051,    0.0013};
		for(int i = order; i<length; i++){
			y[i] = b[0]*x[i] + b[1]*x[i-1] + b[2]*x[i-2] + b[3]*x[i-3] + b[4]*x[i-4]
					 - a[1]*y[i-1] - a[2]*y[i-2] - a[3]*y[i-3] - a[4]*y[i-4];
		}
		
		return y;
	}	
	
	public static double[] butterworth(short[] data) {
		int order;
		int length;
		double[] y;
		short[] x;
		double[] b =  {0.0013,    0.0051,    0.0076,    0.0051,    0.0013};
		double[] a =  {1.0000,   -2.8869,    3.2397,   -1.6565,    0.3240};			
		
		order = a.length;		
		length = data.length;
				
		x = new short[length];
		y = new double[length];
		x = data;
		
		if (order!= 5) return null;
		
		for(int i = 0; i<length; i++){
			y[i] = 0;
		}
				
		// from MATLAB a =  {1.0000,   -2.8869,    3.2397,   -1.6565,    0.3240};
		// from MATLAB b =  {0.0013,    0.0051,    0.0076,    0.0051,    0.0013};
		for(int i = order; i<length; i++){
			y[i] = b[0]*x[i] + b[1]*x[i-1] + b[2]*x[i-2] + b[3]*x[i-3] + b[4]*x[i-4]
					 - a[1]*y[i-1] - a[2]*y[i-2] - a[3]*y[i-3] - a[4]*y[i-4];
		}
		
		return y;
	}
  
  public static double[] abs(double[] x){
    for(int i = 0; i<x.length; i++){
      x[i] = Math.sqrt(x[i]*x[i]); 
    }
    return x;
  }
	
  public static double[] diff(double[] x){
    double[] x_diff;
    x_diff = new double[x.length-1];
	  for(int i = 1;i<x.length; i++){
      x_diff[i-1] = x[i] - x[i-1];
    }
	return x_diff;
  }
  
  public static int[] diff(int[] x){
	    int[] x_diff;
	    x_diff = new int[x.length-1];
		  for(int i = 1;i<x.length; i++){
	      x_diff[i-1] = x[i] - x[i-1];
	    }
		return x_diff;
  }
  
	public static int localMax(double[] data){
		double max;
		int index;
		
		max = data[0];
		index = 0;
		for (int i = 0; i < data.length; i++) {
			if (max<data[i]) {
				max = data[i];
				index = i;
			} 
		}
		return index;
	}
	
	public static int localMax(int[] data){
		int max;
		int index;
		
		max = data[0];
		index = 0;
		for (int i = 0; i < data.length; i++) {
			if (max<data[i]) {
				max = data[i];
				index = i;
			} 
		}
		return index;
	}
	
	public static int localMin(double[] data){
		double min;
		int index;
		
		min = data[0];
		index = 0;
		for (int i = 0; i < data.length; i++) {
			if (min<data[i]) {
				min = data[i];
				index = i;
			} 
		}
		return index;		
	}
	
	public static int localMin(int[] data){
		int min;
		int index;
		
		min = data[0];
		index = 0;
		for (int i = 0; i < data.length; i++) {
			if (min<data[i]) {
				min = data[i];
				index = i;
			} 
		}
		return index;		
	}	
	
	public static double[] getSubsequent(int start, int end, double[] data) {
		double[] output;
		output = new double[end-start+1];
			for (int i = start; i < end+1; i++) {
				output[i-start] = data[i];				
			}
		return output;
	}
	
	public static short[] getSubsequent(int start, int end, short[] data) {
		short[] output;
		output = new short[end-start+1];
			for (int i = start; i < end+1; i++) {
				output[i-start] = data[i];				
			}
		return output;
	}
	
	public static double getEnergy(short[] dataIn){	
		double energy = 0;		
		for (short d:dataIn){
			energy += Math.pow(d, 2);			
		}
		
		energy/=dataIn.length; // Normalizing energy per one sample
		//energy = Math.sqrt(energy); 
		return energy;
	}
	
	/** Calculates mean value of the input data.*/
	public static double getMean(short[] inData) {
		double mean = 0;
		for (short s : inData) {
			mean += s;
		}
		mean /= inData.length;
		return mean;
	}
	
	private static double getMean(int[] inData) {
		double mean = 0;
		for (int s : inData) {
			mean += s;
		}
		mean /= inData.length;
		return mean;		
	}
	
	/** Calculates the standard deviation of the data.*/
	public static double getStd(short[] inData) {
		double std = 0;
		double mean = 0;
		mean = getMean(inData);
		
		for (short s : inData) {
			std += ((s-mean)*(s-mean));
		}		
		std /= (inData.length-1);
		std = Math.sqrt(std);
		return std;
	}
	
	/** getNoise calculates the position of noise and returns it to superior method.*/
	public static short[] xGetNoise(short[] inData) {
		double threshold;
		short[] noise = null;
		int start = 0, end = 0, start2 = 0;
		int[] indices = new int[inData.length];
		int[] indices2 = new int[inData.length];
		int[] indicesClean = null;
		int[] indicesClean2 = null;
		int k = 0, l = 0;
		Double start_m, start2_m;
		
		threshold = getStd(inData);
		for (int i = 0; i < inData.length; i++) {
			if (inData[i] > threshold){
				indices[k] = i;
				k++;
				if (inData[i]>2*threshold){
					indices2[l] = i;
					l++;
				}
			}
		}
		
		indicesClean = new int[k+1];
		indicesClean2 = new int[l+1];
		
		
		for (int i = 0; i < k+1; i++) {
			indicesClean[i] = indices[i];
		}
		
		for (int i = 0; i < l+1; i++) {
			indicesClean2[i] = indices2[i];
		}
		
		start_m = getMean(indicesClean);		
		start2_m = getMean(indicesClean2);
		start = start_m.intValue();
		start2 = start2_m.intValue();
		
		if ((start-start2)>10000) {
			start = start2;
		}
		
		end = start+12500;
		start -= 12500;
		
		if (start<0) {
			start = 0;
		}
		noise = getSubsequent(start, end, inData);
		return noise;
	}
	
	public static short[] xgetBursts(short[] inData) {
		double threshold;
		short[] bursts = null;
		int start = 0, end = 0, start2 = 0;
		int[] indices = new int[inData.length];
		int[] indices2 = new int[inData.length];
		int[] indicesClean = null;
		int[] indicesClean2 = null;
		int k = 0, l = 0;
		Double start_m, start2_m;
		
		threshold = getStd(inData);
		for (int i = 0; i < inData.length; i++) {
			if (inData[i] > threshold){
				indices[k] = i;
				k++;
				if (inData[i]>2*threshold){
					indices2[l] = i;
					l++;
				}
			}
		}
		
		indicesClean = new int[k+1];
		indicesClean2 = new int[l+1];
		
		
		for (int i = 0; i < k+1; i++) {
			indicesClean[i] = indices[i];
		}
		
		for (int i = 0; i < l+1; i++) {
			indicesClean2[i] = indices2[i];
		}
		
		start_m = getMean(indicesClean);		
		start2_m = getMean(indicesClean2);
		start = start_m.intValue();
		start2 = start2_m.intValue();
		
		if ((start-start2)>10000) {
			start = start2;
		}
		
		end = start+12500;
		start -= 12500;

		bursts = getSubsequent(end, inData.length-1, inData);
		return bursts;
	}	


	public static int[] getBurstIndices(short[] inData) {
		int indices[] = new int[20]; // It is expected, that there will not be more than 20 bursts.
		int temp[];
		int k =0;
		double threshold;
					
		threshold = getStd(inData);
		
		for (int i = 0; i < inData.length; i++) {
			
			if (inData[i] > 2*threshold) {
				indices[k] = i; //100 samples are ~2ms with 44100 sampling rate.
				k++;
				if (k==20){
					Log.d("PLR", "Indices overflow");
					break;
				}
					
				i += 3000; // jump over the burst, continues search for other bursts.
			}
		}
		
		// Ensures that number of indices in the matrix is equal to number of bursts.
		temp = new int[k];
		for (int i = 0; i < k; i++) {
			temp[i] = indices[i];
		}
		return temp;		
	}
	
	

}