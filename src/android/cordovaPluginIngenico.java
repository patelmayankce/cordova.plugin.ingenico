package cordova.plugin.ingenico;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ingenico.rba_sdk.*;
import com.ingenico.rba_sdk.RBA_API.LOG_LEVEL;
import com.ingenico.rba_sdk.RBA_API.LOG_OUTPUT_FORMAT_OPTIONS;
import com.ingenico.rbasdk_android_adapter.*;

/**
 * This class echoes a string called from JavaScript.
 */
public class cordovaPluginIngenico extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("connect")) {            
            this.connect(args.getString(0), args.getString(1),callbackContext);
            return true;
        }
        return false;
    }

    private void connect(String ip_address,String port, CallbackContext callbackContext) {

        if(this.RBA_SDK_Initialize()){
            Comm_Settings comSettings = new Comm_Settings();
            comSettings.Interface_id      = Comm_Settings_Constants.SSL_TCPIP_INTERFACE;
            comSettings.IP_Address   = "192.168.0.101";
            comSettings.Port_Num     = "12000";
            ERROR_ID ret = RBA_API.Connect(comSettings);
            if (ret == ERROR_ID.RESULT_SUCCESS){
                callbackContext.success("connected");
            }else{
                callbackContext.success("initialized");                
            }
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
        // if (message != null && message.length() > 0) {
        //     callbackContext.success(message);
        // } else {
        //     callbackContext.error("Expected one non-empty string argument.");
        // }
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
