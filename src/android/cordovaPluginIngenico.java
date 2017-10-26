package cordova.plugin.ingenico;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;

import com.ingenico.rba_sdk.*;
import com.ingenico.rba_sdk.RBA_API.LOG_LEVEL;
import com.ingenico.rba_sdk.RBA_API.LOG_OUTPUT_FORMAT_OPTIONS;
import com.ingenico.rbasdk_android_adapter.*;

/**
 * This class echoes a string called from JavaScript.
 */
public class cordovaPluginIngenico extends CordovaPlugin {

    private Context context;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        context = this.cordova.getActivity().getApplicationContext();
        if (action.equals("connect")) {
            this.connect(args.getString(0), args.getString(1),callbackContext);
            return true;
        }
        return false;
    }

    private void connect(String ip_address,String port, CallbackContext callbackContext) {
        if(ip_address.trim() != null && ip_address.trim().length() > 0 && port.trim() != null && port.trim().length() > 0 ){
            if(this.RBA_SDK_Initialize()){
                InputStream clientCertKeyFile = null;
                InputStream caCertFile        = null;

                Comm_Settings comSettings = new Comm_Settings();
                RBASDKAdapter.SetApplicationContext(context);
                comSettings.Interface_id      = Comm_Settings_Constants.SSL_TCPIP_INTERFACE;
                comSettings.IP_Address   = ip_address;
                comSettings.Port_Num     = port;
                System.out.println(comSettings.IP_Address);
                System.out.println(comSettings.Port_Num);

                try{ caCertFile   		= context.getAssets().open("www/assets/certy/CA_SERVER_CERT.PEM");
                System.out.println(caCertFile);
                System.out.println("get caCertFile assets");
                }
                catch  (IOException e) {
                  System.out.println("IO exception:"+e.toString());
                   }

                try{ clientCertKeyFile   = context.getAssets().open("www/assets/certy/CLIENT.bks");
                System.out.println(clientCertKeyFile);
                System.out.println("get clientCertKeyFile assets");}
                catch  (IOException e) {
                  System.out.println("IO exception:"+e.toString());
                  try{ caCertFile.close();}catch(IOException ee){
                    System.out.println("IO exception:"+ ee.toString());
                  }
                  return;
                }

                char keystorepass[]	= "Ingenico".toCharArray();
                RBASDKAdapter.SetCaCerticicate(caCertFile);
                RBASDKAdapter.SetLocalCerticicate(clientCertKeyFile,keystorepass);

                ERROR_ID ret = RBA_API.Connect(comSettings);
                System.out.println(ret);
                if (ret == ERROR_ID.RESULT_SUCCESS){
                    callbackContext.success("connected");
                }else{
                    callbackContext.success("initialized");
                }
            } else {
                callbackContext.error("Expected one non-empty string argument.");
            }
        } else {
            callbackContext.error("Ip Address and port is missing");
        }
    }

    private boolean RBA_SDK_Initialize()
    {
    	boolean ret 			= false;
        System.out.println("RBA_SDK Initializing ... ");

        // Initialize call back for RBA_SDK log
        RBA_API.SetDefaultLogLevel(LOG_LEVEL.LTL_TRACE);
        RBA_API.SetLogCallBack(new LogTrace());
        RBA_API.SetTraceOutputFormatOption(LOG_OUTPUT_FORMAT_OPTIONS.LOFO_NO_INSTANCE_ID);

        if( RBA_API.Initialize() == ERROR_ID.RESULT_SUCCESS )
        {
            System.out.println("Initialized");

            // // Initialize call back for RBA messages
            // RBA_API.SetMessageCallBack(new RBA_SDK_EventCallBackHandler());

            ret = true;
        }
        else
        	System.out.println("Initialized Failed");

        return ret;
    }

    private class LogTrace implements LogTraceInterface
    {
        public void Log(String  logLine)
        {
            System.out.println(logLine);
        }
    }
}
