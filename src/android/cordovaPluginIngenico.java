package cordova.plugin.ingenico;

// import java.io.IOException;
// import java.io.InputStream;
// import java.io.UnsupportedEncodingException;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
// import android.content.Context;

import com.ingenico.rba_sdk.*;
import com.ingenico.rba_sdk.RBA_API.LOG_LEVEL;
import com.ingenico.rba_sdk.RBA_API.LOG_OUTPUT_FORMAT_OPTIONS;
// import com.ingenico.rbasdk_android_adapter.*;
import com.ingenico.iConnectEFT.*;

/**
 * This class echoes a string called from JavaScript.
 */
public class cordovaPluginIngenico extends CordovaPlugin {

    // private Context context;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        // context = this.cordova.getActivity().getApplicationContext();
        if (action.equals("connect")) {
            this.connect(args.getString(0), args.getString(1),callbackContext);
            return true;
        }
        return false;
    }

    private void connect(String ip_address,String port, CallbackContext callbackContext) {
        if(ip_address.trim() != null && ip_address.trim().length() > 0 && port.trim() != null && port.trim().length() > 0 ){
            if(this.RBA_SDK_Initialize()){
                // InputStream clientCertKeyFile = null;
                // InputStream caCertFile        = null;

                // System.out.println("initialize iConnectEFT");
                // try{
                //   iConnectEFT eft = new iConnectEFT("mayank");
                //   eft.ConnectTcp(ip_address,443);
                //   System.out.println("Connection establised");
                // }catch(RbaSdkException e){
                //   System.out.println("RbaSdkException:"+ e.toString());
                // }

                // System.out.println(eft);

                Comm_Timeout commTimeout = new Comm_Timeout();
                commTimeout.connectTimeOut = 2000;
                commTimeout.receiveTimeOut = 2000;
                commTimeout.sendTimeOut    = 2000;
                RBA_API.SetCommTimeouts(commTimeout);


                Comm_Settings comSettings = new Comm_Settings();
                // RBASDKAdapter.SetApplicationContext(context);
                comSettings.Interface_id      = Comm_Settings_Constants.TCPIP_INTERFACE;
                comSettings.IP_Address   = ip_address;
                comSettings.Port_Num     = port;
                System.out.println(comSettings.IP_Address);
                System.out.println(comSettings.Port_Num);
/**
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
 */
                ERROR_ID ret = RBA_API.Connect(comSettings);
                System.out.println(ret);
                if (ret == ERROR_ID.RESULT_SUCCESS){
                  if( RBA_API.GetConnectionStatus() == RBA_API.ConnectionStatus.CONNECTED){
                    this.DoCardRequest();
                    callbackContext.success("connected");
                  }else{
                    callbackContext.success("not connected");
                  }
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

    private void DoCardRequest()
    {
        if( this.Online() && this.Offline() && this.CardReadRequest()) {
            System.out.println("");
            System.out.println("SWIPE A CARD ON TELIUM DEVICE");
            System.out.println("");
        }
    }

    private boolean Online()
    {
        System.out.println("Online Message Sent...");

        //Set Parameters
        RBA_API.SetParam(PARAMETER_ID.P01_REQ_APPID,   "0000");
        RBA_API.SetParam(PARAMETER_ID.P01_REQ_PARAMID, "0000");

        //Process Message
        if (RBA_API.ProcessMessage(MESSAGE_ID.M01_ONLINE) != ERROR_ID.RESULT_SUCCESS) {
            System.out.println("Online Message Failed");
            return false;
        }

        System.out.println("Online Message Successful");

        // Get Parameters
        String appId   = RBA_API.GetParam(PARAMETER_ID.P01_RES_APPID);
        String paramId = RBA_API.GetParam(PARAMETER_ID.P01_RES_PARAMID);

        System.out.println("Online :: App ID = " + appId);
        System.out.println("Online :: Param ID = " + paramId);

        return true;
    }

    private boolean Offline()
    {
        System.out.println("Offline Message Sent...");
        // Set Parameters
        RBA_API.SetParam(PARAMETER_ID.P00_REQ_REASON_CODE, ("0000"));
        // Process Message
        if (RBA_API.ProcessMessage(MESSAGE_ID.M00_OFFLINE) != ERROR_ID.RESULT_SUCCESS) {
            System.out.println("Offline Failed");
            return false;
        }
        System.out.println("Offline Successful");
        return true;
    }

    private boolean Status()
    {
        System.out.println("Status Message Sent...");
        // Process Message
        if (RBA_API.ProcessMessage(MESSAGE_ID.M11_STATUS) != ERROR_ID.RESULT_SUCCESS) {
            System.out.println("Status Failed");
            return false;
        }
        System.out.println("Status Successful");
        return true;
    }

    private boolean CardReadRequest()
    {
        RBA_API.SetParam(PARAMETER_ID.P23_REQ_PROMPT_INDEX, "Hello, Swipe a card");
        //RBA_API.SetParam(PARAMETER_ID.P23_REQ_FORM_NAME,    "FORM1.K3Z");
        //RBA_API.SetParam(PARAMETER_ID.P23_REQ_OPTIONS,      "MSC");

        if( RBA_API.ProcessMessage(MESSAGE_ID.M23_CARD_READ) != ERROR_ID.RESULT_SUCCESS ) {
            System.out.println("Card Read Failed");
            return false;
        }
        System.out.println("Card Read Successful");
        return true;
    }


    private class LogTrace implements LogTraceInterface
    {
        public void Log(String  logLine)
        {
            System.out.println(logLine);
        }
    }
}
