package cordova.plugin.ingenico;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ingenico.rba_sdk.*;
import com.ingenico.rba_sdk.RBA_API.LOG_LEVEL;
import com.ingenico.rba_sdk.RBA_API.LOG_OUTPUT_FORMAT_OPTIONS;

/**
 * This class echoes a string called from JavaScript.
 */
public class cordovaPluginIngenico extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        RBA_API.SetDefaultLogLevel(LOG_LEVEL.LTL_TRACE);
        RBA_API.SetTraceOutputFormatOption(LOG_OUTPUT_FORMAT_OPTIONS.LOFO_NO_INSTANCE_ID);
        RBA_API.SetTraceOutputFormatOption(LOG_OUTPUT_FORMAT_OPTIONS.LOFO_NO_THREAD_ID);
        RBA_API.SetLogCallBack(new LogTrace());
        RBA_API.Initialize();
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private class LogTrace implements LogTraceInterface
    {
        public void Log(String  logLine)
        {
            System.out.println(logLine);
        }
    }
}
