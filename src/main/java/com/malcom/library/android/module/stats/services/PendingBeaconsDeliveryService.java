package com.malcom.library.android.module.stats.services;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.malcom.library.android.module.core.MCMCoreAdapter;
import com.malcom.library.android.module.stats.MCMStats;
import com.malcom.library.android.utils.HttpDateUtils;
import com.malcom.library.android.utils.MalcomHttpOperations;
import com.malcom.library.android.utils.ToolBox;
import com.malcom.library.android.utils.ToolBox.HTTP_METHOD;
import com.malcom.library.android.utils.encoding.DigestUtils;
import com.malcom.library.android.utils.encoding.base64.Base64;

/**
 * Service that delivers the pending beacons to malcom server.
 * 
 * By using a service for this, we avoid the system to destroys 
 * the application if more resources are needed. Operation of 
 * this kind (deliveries) should always be done by using a service.
 * 
 * We use the IntentService because the advantages explained here: 
 * http://developer.android.com/guide/components/services.html
 * 
 * @author Malcom Ventures S.L
 * @since  2012
 *
 */
public class PendingBeaconsDeliveryService extends IntentService {
	
	
	public PendingBeaconsDeliveryService() {
		super("PendingBeaconsDeliveryService");
	}
	
	public PendingBeaconsDeliveryService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		String[] pendingBeacons = listCachedBeacons();
		if(pendingBeacons!=null && pendingBeacons.length>0){
			if(ToolBox.network_haveNetworkConnection(getApplicationContext())){
				sendCachedBeacons(pendingBeacons);
			}else{
				Log.i(MCMStats.TAG,"PendingBeaconsDeliveryService: no network connection, skipping pending beacon.");
			}
		}
	}

	
	
	// AUXILIAR FUNCTIONS ----------------------------------------------------------------------------------------------
	
	private String[] listCachedBeacons(){
		String filePath = getApplicationContext().getFilesDir().getAbsolutePath();//returns current directory.
		File appInternalDir = new File(filePath);
		String[] pendingBeacons = appInternalDir.list(new FilenameFilter(){
			public boolean accept(File arg0, String name) {
				return name.startsWith(MCMStats.CACHED_BEACON_FILE_PREFIX);			
			}});
		
		return pendingBeacons;
	}
	
	private void sendCachedBeacons(String[] pendingBeacons){
		Log.i(MCMStats.TAG, "PendingBeaconsDeliveryService: Pending beacons to send: " + pendingBeacons.length);
		for(String b:pendingBeacons){			
			try {
				byte[] beaconBytes = ToolBox.storage_readDataFromInternalStorage(getApplicationContext(), b);
				if(beaconBytes!=null && beaconBytes.length>0){
					sendToMalcom(new String(beaconBytes));
				}
				ToolBox.storage_deleteDataFromInternalStorage(getApplicationContext(), b);
			} catch (Exception e) {
				Log.e(MCMStats.TAG,"PendingBeaconsDeliveryService: Error sending pending beacon ("+e.getMessage()+")",e);
			}
		}
	}
	
	private void sendToMalcom(String beaconData) throws Exception{
		
		MalcomHttpOperations.sendRequestToMalcom(MCMCoreAdapter.getInstance().coreGetProperty(
				MCMCoreAdapter.PROPERTIES_MALCOM_BASEURL) + "v1/beacon",
				"/v1/beacon", 
				beaconData,
				MCMCoreAdapter.getInstance().coreGetProperty(MCMCoreAdapter.PROPERTIES_MALCOM_APPID),
				MCMCoreAdapter.getInstance().coreGetProperty(MCMCoreAdapter.PROPERTIES_MALCOM_APPSECRETKEY), 
				HTTP_METHOD.POST);
	}
	
}
