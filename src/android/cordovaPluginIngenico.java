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

import com.ingenico.framework.iconnecttsi.*;

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
        if(ip_address.trim() != null && ip_address.trim().length() > 0 && port.trim() != null && port.trim().length() > 0 ){
            RequestType.Sale saleReq = new RequestType.Sale(1000);
            saleReq.setClerkId(1);
            saleReq.setEcrTenderType(new iConnectTsiTypes.EcrTenderType.Credit());

            this.processSaleRequest(saleReq);

            callbackContext.success("Ip Address and port is missing");
        } else {
            callbackContext.error("Ip Address and port is missing");
        }
    }

    private void processSaleRequest(RequestType.Sale... params){
        TsiStatus ret = new TsiStatus();

        System.out.println("iConnect-TSI version " + TransactionManager.getVersion() + eol);
        System.out.println("iConnect version " + Utility.iConnectVersion() + eol);

        try {
            device = new IConnectTcpDevice(ip_address, port);
            //pass "this" as an class implementing IConnectDevice.Logger
            device.enableTsiTraces(true);

            //By contract, first parameter is a sale request object
            RequestType.Sale req = params[0];

            TransactionManager transactionManager = new TransactionManager(device);

            System.out.println("Connecting...  " + eol);
            device.connect();
            System.out.println("Connected" + eol);

            System.out.println("Sale request with amount of" + req.getAmount() + eol);
            System.out.println("Sending request ...  " + eol);

            transactionManager.sendRequest(req);

            boolean multiTransaction = false;
            String refNo = null;


            do {
                System.out.println("Waiting for response...  " + eol);
                ResponseType.Raw resp = transactionManager.receiveResponse();


                iConnectTsiTypes.TransactionStatus status = resp.getStatus();
                multiTransaction = resp.isMultiTransactionFlag();

                //Handle receipt printing
                if (resp.getStatus().getTransactionStatus() == iConnectTsiTypes.TransactionStatus.ReceiptInformation) {

                    multiTransaction = printReceipt(resp, transactionManager);

                } else if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.Approved) {
                    ResponseType.Sale saleResp = new ResponseType.Sale().validate(resp);

                    if (saleResp == null) {
                        System.out.println("ERROR: Cannot construct sale response object from raw. Reported type: " + resp.getType() + eol);
                        break;
                    } else {
                        System.out.println("Status: " + saleResp.getStatus() + eol);


                        System.out.println("Date: " + Integer.toString(saleResp.getTransactionDate()) + eol);
                        System.out.println("Time: " + Integer.toString(saleResp.getTransactionTime()) + eol);
                        System.out.println("Card Type: " + saleResp.getCustomerCardType() + eol);
                        System.out.println("Customer PAN " + saleResp.getCustomerAccountNo() + eol);
                        System.out.println("Reference Number: " + saleResp.getReferenceNo() + eol);
                        System.out.println("Terminal ID: " + saleResp.getTerminalId() + eol);
                        System.out.println("Total Amount: " + Integer.toString(saleResp.getTotalAmount()) + eol);
                        refNo = saleResp.getReferenceNo();
                    }
                } else if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.CancelledByUser ||
                        status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.TimeoutOnUserInput) {
                    System.out.println("Cancelled or timed out" + eol);
                    break;
                }

            } while (multiTransaction);

            //Reference number is not null, which means we can use it to send a Void request
            if (refNo != null) {
                //reconnect to a terminal
                device.disconnect();
                device.connect();
                //create and fill a void request object
                RequestType.VoidRequest voidReq = new RequestType.VoidRequest();
                voidReq.setReferenceNo(refNo);

                System.out.println("Void request with reference number " + refNo + " is being sent..." + eol);
                transactionManager.sendRequest(voidReq);

                do {

                    ResponseType.Raw raw = transactionManager.receiveResponse();
                    iConnectTsiTypes.TransactionStatus status = raw.getStatus();

                    //the same outcome as from using status.toString()
                    System.out.println("Status: " + Utility.TransactionStatusToString(status.getTransactionStatus()) + eol);

                    multiTransaction = raw.isMultiTransactionFlag();

                    if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.ReceiptInformation) {
                        multiTransaction = printReceipt(raw, transactionManager);
                    } else if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.Approved) {
                        ResponseType.VoidResponse voidResp = new ResponseType.VoidResponse().validate(raw);
                        if (voidResp == null) {
                            System.out.println("ERROR: Cannot construct void response object from raw. Reported type: " + raw.getType() + eol);
                            break;
                        }
                        System.out.println("Successfully voided" + eol);
                    }
                } while (multiTransaction);
            }

            System.out.println("Disconnecting ...  ");
            device.disconnect();
            System.out.println("OK" + eol);

        } catch (TsiException e) {
            ret = new TsiStatus(e);
            System.out.println(e.getMessage() + eol);
        } finally {
            device.dispose();
        }

    }

}
