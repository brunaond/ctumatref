package cz.cvut.fel.wavrecordtest;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ListFiles extends ListActivity {
	static String logFiles[];
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		String file_wav;
		file_wav = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
		
		File f = new File(file_wav);        		
		File file[] = f.listFiles();
		String fileNames[] = new String[file.length];
				
		int k = 0;
		for (int i=0; i < file.length; i++) {
			String name;
			name = file[i].getName();
			if (name.matches("^rec.*wav$")){
				fileNames[k] = name;
				k++;				
			}
		}
		
		
		
		Log.d("PLR", Integer.toString(k));
		
		 logFiles = new String[k];
					
		for(int i=0; i < k; i++) {
			logFiles[i] = fileNames[i];
		}
		
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, 
				logFiles));
		

		
		Log.d("PLR", "New Intent List.");
		Log.d("Files", "Size: "+ file.length);
		
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		Intent intent;
		if(position == 0){
			intent = new Intent(this, WavRecordTest.class);
			startActivity(intent);
		} else if (position == 1) {
			intent = new Intent(this, ListFiles.class);
			startActivity(intent);			
		} else if (position == 2) {

		}
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.list_files, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
