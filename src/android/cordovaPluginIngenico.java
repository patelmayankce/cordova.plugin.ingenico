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

import java.util.Map;

/**
 * This class echoes a string called from JavaScript.
 */
public class cordovaPluginIngenico extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("connect")) {
            this.connect(args.getString(0), args.getString(1), args.getInt(2), args.getString(3), callbackContext);
            return true;
        } else if (action.equals("void")) {
            this.voidTransation(args.getString(0), args.getString(1), args.getString(2), callbackContext);
            return true;
        } else if (action.equals("refund")) {
            this.refundTransation(args.getString(0), args.getString(1), args.getInt(2), args.getString(3), callbackContext);
            return true;
        } else if (action.equals("addTip")) {
            this.addTip(args.getString(0), args.getString(1), args.getString(2), callbackContext);
            return true;
        }
        return false;
    }

    private void connect(String ip_address, String port, Integer amount, String invoiceNo, CallbackContext callbackContext) throws JSONException {
        System.out.println("Ingenico Connecting......");
        JSONObject JSONResponse = new JSONObject();

        if (ip_address.trim() != null && ip_address.trim().length() > 0 && port.trim() != null && port.trim().length() > 0
                && amount != null && invoiceNo.trim() != null && invoiceNo.trim().length() > 0) {
            RequestType.Sale saleReq = new RequestType.Sale(amount);
            saleReq.setClerkId(1);
            saleReq.setEcrTenderType(new iConnectTsiTypes.EcrTenderType.Credit());
            saleReq.setInvoiceNo(invoiceNo);
            // this.processSaleRequest(saleReq,ip_address,port);

            TsiStatus ret = new TsiStatus();

            System.out.println("iConnect-TSI version " + TransactionManager.getVersion());
            System.out.println("iConnect version " + Utility.iConnectVersion());
            IConnectDevice device = null;

            try {
                device = new IConnectTcpDevice(ip_address, port);
                //pass "this" as an class implementing IConnectDevice.Logger
                // device.enableTsiTraces(true,this);

                //By contract, first parameter is a sale request object
                RequestType.Sale req = saleReq;

                TransactionManager transactionManager = new TransactionManager(device);

                System.out.println("Connecting...  ");
                device.connect();
                System.out.println("Connected");

                System.out.println("Sale request with amount of" + req.getAmount());
                System.out.println("Sending request ...  ");

                transactionManager.sendRequest(req);

                boolean multiTransaction = false;
                String refNo = null;

                do {
                    System.out.println("Waiting for response...  ");
                    ResponseType.Raw resp = transactionManager.receiveResponse();

                    iConnectTsiTypes.TransactionStatus status = resp.getStatus();
                    multiTransaction = resp.isMultiTransactionFlag();
                    System.out.println("Status: " + Utility.TransactionStatusToString(status.getTransactionStatus()));
                    //Handle receipt printing
                    if (resp.getStatus().getTransactionStatus() == iConnectTsiTypes.TransactionStatus.ReceiptInformation) {

                        multiTransaction = printReceipt(resp, transactionManager);

                    } else if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.Approved) {
                        ResponseType.Sale saleResp = new ResponseType.Sale().validate(resp);

                        if (saleResp == null) {
                            System.out
                                    .println("ERROR: Cannot construct sale response object from raw. Reported type: " + resp.getType());
                            JSONResponse.put("error",
                                    "ERROR: Cannot construct sale response object from raw. Reported type: " + resp.getType());
                            break;
                        } else {
                            System.out.println("Status: " + saleResp.getStatus());

                            System.out.println("Date: " + Integer.toString(saleResp.getTransactionDate()));
                            System.out.println("Time: " + Integer.toString(saleResp.getTransactionTime()));
                            System.out.println("Card Type: " + saleResp.getCustomerCardType());
                            System.out.println("Customer PAN " + saleResp.getCustomerAccountNo());
                            System.out.println("Reference Number: " + saleResp.getReferenceNo());
                            System.out.println("Terminal ID: " + saleResp.getTerminalId());
                            System.out.println("Total Amount: " + Integer.toString(saleResp.getTotalAmount()));
                            refNo = saleResp.getReferenceNo();
                            JSONResponse.put("status", saleResp.getStatus());
                            JSONResponse.put("data", Integer.toString(saleResp.getTransactionDate()));
                            JSONResponse.put("time", Integer.toString(saleResp.getTransactionTime()));
                            JSONResponse.put("cardType", saleResp.getCustomerCardType());
                            JSONResponse.put("customerPan", saleResp.getCustomerAccountNo());
                            JSONResponse.put("referenceNumber", saleResp.getReferenceNo());
                            JSONResponse.put("invoiceNumber", saleResp.getInvoiceNo());
                            JSONResponse.put("terminalId", saleResp.getTerminalId());
                            JSONResponse.put("totalAmount", Integer.toString(saleResp.getTotalAmount()));
                        }
                    } else if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.CancelledByUser
                            || status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.TimeoutOnUserInput) {
                        System.out.println("Cancelled or timed out");
                        JSONResponse.put("error", "Cancelled or timed out");
                        break;
                    }

                } while (multiTransaction);

                //Reference number is not null, which means we can use it to send a Void request

                System.out.println("Disconnecting ...  ");
                device.disconnect();
                System.out.println("OK");

            } catch (TsiException e) {
                ret = new TsiStatus(e);
                System.out.println(e.getMessage());
                JSONResponse.put("error", e.getMessage());
            } finally {
                device.dispose();
            }
        } else {
            JSONResponse.put("error", "Ip Address / port / amount is missing");
        }

        try {
            String status = JSONResponse.getString("error");
            callbackContext.error(JSONResponse.toString());
        } catch (JSONException e) {
            callbackContext.success(JSONResponse.toString());
        }
    }

    private void voidTransation(String ip_address, String port, String refNo, CallbackContext callbackContext)
            throws JSONException {
        JSONObject JSONResponse = new JSONObject();
        if (ip_address.trim() != null && ip_address.trim().length() > 0 && port.trim() != null && port.trim().length() > 0
                && refNo.trim() != null && refNo.trim().length() > 0) {

            TsiStatus ret = new TsiStatus();
            System.out.println("iConnect-TSI version " + TransactionManager.getVersion());
            System.out.println("iConnect version " + Utility.iConnectVersion());
            IConnectDevice device = null;
            try {
                device = new IConnectTcpDevice(ip_address, port);
                //pass "this" as an class implementing IConnectDevice.Logger
                // device.enableTsiTraces(true,this);
                TransactionManager transactionManager = new TransactionManager(device);

                boolean multiTransaction = false;

                //Reference number is not null, which means we can use it to send a Void request

                //reconnect to a terminal
                device.connect();
                //create and fill a void request object
                RequestType.VoidRequest voidReq = new RequestType.VoidRequest();
                voidReq.setReferenceNo(refNo);

                System.out.println("Void request with reference number " + refNo + " is being sent...");
                transactionManager.sendRequest(voidReq);

                do {

                    ResponseType.Raw raw = transactionManager.receiveResponse();
                    iConnectTsiTypes.TransactionStatus status = raw.getStatus();

                    //the same outcome as from using status.toString()
                    System.out.println("Status: " + Utility.TransactionStatusToString(status.getTransactionStatus()));

                    multiTransaction = raw.isMultiTransactionFlag();

                    if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.ReceiptInformation) {
                        multiTransaction = printReceipt(raw, transactionManager);
                    } else if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.Approved) {
                        ResponseType.VoidResponse voidResp = new ResponseType.VoidResponse().validate(raw);
                        if (voidResp == null) {
                            System.out
                                    .println("ERROR: Cannot construct void response object from raw. Reported type: " + raw.getType());
                            JSONResponse.put("error",
                                    "ERROR: Cannot construct void response object from raw. Reported type: " + raw.getType());
                            break;
                        }
                        System.out.println("Successfully voided");
                        JSONResponse.put("status", voidResp.getStatus());
                    }
                } while (multiTransaction);

                System.out.println("Disconnecting ...  ");
                device.disconnect();
                System.out.println("OK");

            } catch (TsiException e) {
                ret = new TsiStatus(e);
                System.out.println(e.getMessage());
                JSONResponse.put("error", e.getMessage());
            } finally {
                device.dispose();
            }
        } else {
            JSONResponse.put("error", "Ip Address / port / refNum is missing");
        }

        try {
            String status = JSONResponse.getString("error");
            callbackContext.error(JSONResponse.toString());
        } catch (JSONException e) {
            callbackContext.success(JSONResponse.toString());
        }

    }
    
    private void refundTransation(String ip_address, String port, Integer amount, String invoiceNo, CallbackContext callbackContext) throws JSONException {
        System.out.println("Ingenico Connecting......");
        JSONObject JSONResponse = new JSONObject();

        if (ip_address.trim() != null && ip_address.trim().length() > 0 && port.trim() != null && port.trim().length() > 0
                && amount != null && invoiceNo.trim() != null && invoiceNo.trim().length() > 0) {
            RequestType.Refund refundReq = new RequestType.Refund(amount);
            refundReq.setClerkId(1);
            refundReq.setEcrTenderType(new iConnectTsiTypes.EcrTenderType.Credit());
            refundReq.setInvoiceNo(invoiceNo);

            TsiStatus ret = new TsiStatus();

            System.out.println("iConnect-TSI version " + TransactionManager.getVersion());
            System.out.println("iConnect version " + Utility.iConnectVersion());
            IConnectDevice device = null;

            try {
                device = new IConnectTcpDevice(ip_address, port);
                //pass "this" as an class implementing IConnectDevice.Logger
                // device.enableTsiTraces(true,this);

                //By contract, first parameter is a refund request object
                RequestType.Refund req = refundReq;

                TransactionManager transactionManager = new TransactionManager(device);

                System.out.println("Connecting...  ");
                device.connect();
                System.out.println("Connected");

                System.out.println("refund request with amount of" + req.getAmount());
                System.out.println("Sending request ...  ");

                transactionManager.sendRequest(req);

                boolean multiTransaction = false;
                String refNo = null;

                do {
                    System.out.println("Waiting for response...  ");
                    ResponseType.Raw resp = transactionManager.receiveResponse();

                    iConnectTsiTypes.TransactionStatus status = resp.getStatus();
                    multiTransaction = resp.isMultiTransactionFlag();
                    System.out.println("Status: " + Utility.TransactionStatusToString(status.getTransactionStatus()));
                    //Handle receipt printing
                    if (resp.getStatus().getTransactionStatus() == iConnectTsiTypes.TransactionStatus.ReceiptInformation) {

                        multiTransaction = printReceipt(resp, transactionManager);

                    } else if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.Approved) {
                        ResponseType.Refund refundResp = new ResponseType.Refund().validate(resp);

                        if (refundResp == null) {
                            System.out
                                    .println("ERROR: Cannot construct refund response object from raw. Reported type: " + resp.getType());
                            JSONResponse.put("error",
                                    "ERROR: Cannot construct refund response object from raw. Reported type: " + resp.getType());
                            break;
                        } else {
                            System.out.println("Status: " + refundResp.getStatus());

                            System.out.println("Date: " + Integer.toString(refundResp.getTransactionDate()));
                            System.out.println("Time: " + Integer.toString(refundResp.getTransactionTime()));
                            System.out.println("Card Type: " + refundResp.getCustomerCardType());
                            System.out.println("Customer PAN " + refundResp.getCustomerAccountNo());
                            System.out.println("Reference Number: " + refundResp.getReferenceNo());
                            System.out.println("Terminal ID: " + refundResp.getTerminalId());
                            System.out.println("Total Amount: " + Integer.toString(refundResp.getTotalAmount()));
                            refNo = refundResp.getReferenceNo();
                            JSONResponse.put("status", refundResp.getStatus());
                            JSONResponse.put("data", Integer.toString(refundResp.getTransactionDate()));
                            JSONResponse.put("time", Integer.toString(refundResp.getTransactionTime()));
                            JSONResponse.put("cardType", refundResp.getCustomerCardType());
                            JSONResponse.put("customerPan", refundResp.getCustomerAccountNo());
                            JSONResponse.put("referenceNumber", refundResp.getReferenceNo());
                            JSONResponse.put("invoiceNumber", refundResp.getInvoiceNo());
                            JSONResponse.put("terminalId", refundResp.getTerminalId());
                            JSONResponse.put("totalAmount", Integer.toString(refundResp.getTotalAmount()));
                        }
                    } else if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.CancelledByUser
                            || status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.TimeoutOnUserInput) {
                        System.out.println("Cancelled or timed out");
                        JSONResponse.put("error", "Cancelled or timed out");
                        break;
                    }

                } while (multiTransaction);

                //Reference number is not null, which means we can use it to send a Void request

                System.out.println("Disconnecting ...  ");
                device.disconnect();
                System.out.println("OK");

            } catch (TsiException e) {
                ret = new TsiStatus(e);
                System.out.println(e.getMessage());
                JSONResponse.put("error", e.getMessage());
            } finally {
                device.dispose();
            }
        } else {
            JSONResponse.put("error", "Ip Address / port / amount is missing");
        }

        try {
            String status = JSONResponse.getString("error");
            callbackContext.error(JSONResponse.toString());
        } catch (JSONException e) {
            callbackContext.success(JSONResponse.toString());
        }
    }

    private void addTip(String ip_address, String port, String invoiceNo, CallbackContext callbackContext)
            throws JSONException {
        JSONObject JSONResponse = new JSONObject();
        if (ip_address.trim() != null && ip_address.trim().length() > 0 && port.trim() != null && port.trim().length() > 0
                && invoiceNo.trim() != null && invoiceNo.trim().length() > 0) {

            TsiStatus ret = new TsiStatus();
            System.out.println("iConnect-TSI version " + TransactionManager.getVersion());
            System.out.println("iConnect version " + Utility.iConnectVersion());
            IConnectDevice device = null;
            try {
                device = new IConnectTcpDevice(ip_address, port);
                //pass "this" as an class implementing IConnectDevice.Logger
                // device.enableTsiTraces(true,this);
                TransactionManager transactionManager = new TransactionManager(device);

                boolean multiTransaction = false;

                //Reference number is not null, which means we can use it to send a add tip request

                //reconnect to a terminal
                device.connect();
                //create and fill a add tip request object
                RequestType.AddTip AddTipReq = new RequestType.AddTip();
                AddTipReq.setInvoiceNo(invoiceNo);

                System.out.println("add tip request with reference number " + invoiceNo + " is being sent...");
                transactionManager.sendRequest(AddTipReq);

                do {

                    ResponseType.Raw raw = transactionManager.receiveResponse();
                    iConnectTsiTypes.TransactionStatus status = raw.getStatus();

                    //the same outcome as from using status.toString()
                    System.out.println("Status: " + Utility.TransactionStatusToString(status.getTransactionStatus()));

                    multiTransaction = raw.isMultiTransactionFlag();

                    if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.ReceiptInformation) {
                        multiTransaction = printReceipt(raw, transactionManager);
                    } else if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.Approved) {
                        ResponseType.AddTip addTipResp = new ResponseType.AddTip().validate(raw);
                        if (addTipResp == null) {
                            System.out
                                    .println("ERROR: Cannot construct add tip response object from raw. Reported type: " + raw.getType());
                            JSONResponse.put("error",
                                    "ERROR: Cannot construct add tip response object from raw. Reported type: " + raw.getType());
                            break;
                        }
                        System.out.println("Successfully add tip");
                        JSONResponse.put("status", addTipResp.getStatus());
                    }
                } while (multiTransaction);

                System.out.println("Disconnecting ...  ");
                device.disconnect();
                System.out.println("OK");

            } catch (TsiException e) {
                ret = new TsiStatus(e);
                System.out.println(e.getMessage());
                JSONResponse.put("error", e.getMessage());
            } finally {
                device.dispose();
            }
        } else {
            JSONResponse.put("error", "Ip Address / port / refNum is missing");
        }

        try {
            String status = JSONResponse.getString("error");
            callbackContext.error(JSONResponse.toString());
        } catch (JSONException e) {
            callbackContext.success(JSONResponse.toString());
        }

    }

  /*
  private void processSaleRequest(RequestType.Sale saleReq,String ip_address,String port){
      TsiStatus ret = new TsiStatus();

      System.out.println("iConnect-TSI version " + TransactionManager.getVersion());
      System.out.println("iConnect version " + Utility.iConnectVersion());
      IConnectDevice device = null;

      try {
          device = new IConnectTcpDevice(ip_address, port);
          //pass "this" as an class implementing IConnectDevice.Logger
          // device.enableTsiTraces(true,this);

          //By contract, first parameter is a sale request object
          RequestType.Sale req = saleReq;

          TransactionManager transactionManager = new TransactionManager(device);

          System.out.println("Connecting...  ");
          device.connect();
          System.out.println("Connected");

          System.out.println("Sale request with amount of" + req.getAmount());
          System.out.println("Sending request ...  ");

          transactionManager.sendRequest(req);

          boolean multiTransaction = false;
          String refNo = null;


          do {
              System.out.println("Waiting for response...  ");
              ResponseType.Raw resp = transactionManager.receiveResponse();


              iConnectTsiTypes.TransactionStatus status = resp.getStatus();
              multiTransaction = resp.isMultiTransactionFlag();

              //Handle receipt printing
              if (resp.getStatus().getTransactionStatus() == iConnectTsiTypes.TransactionStatus.ReceiptInformation) {

                  multiTransaction = printReceipt(resp, transactionManager);

              } else if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.Approved) {
                  ResponseType.Sale saleResp = new ResponseType.Sale().validate(resp);

                  if (saleResp == null) {
                      System.out.println("ERROR: Cannot construct sale response object from raw. Reported type: " + resp.getType());
                      break;
                  } else {
                      System.out.println("Status: " + saleResp.getStatus());


                      System.out.println("Date: " + Integer.toString(saleResp.getTransactionDate()));
                      System.out.println("Time: " + Integer.toString(saleResp.getTransactionTime()));
                      System.out.println("Card Type: " + saleResp.getCustomerCardType());
                      System.out.println("Customer PAN " + saleResp.getCustomerAccountNo());
                      System.out.println("Reference Number: " + saleResp.getReferenceNo());
                      System.out.println("Terminal ID: " + saleResp.getTerminalId());
                      System.out.println("Total Amount: " + Integer.toString(saleResp.getTotalAmount()));
                      refNo = saleResp.getReferenceNo();
                  }
              } else if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.CancelledByUser ||
                      status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.TimeoutOnUserInput) {
                  System.out.println("Cancelled or timed out");
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

              System.out.println("Void request with reference number " + refNo + " is being sent...");
              transactionManager.sendRequest(voidReq);

              do {

                  ResponseType.Raw raw = transactionManager.receiveResponse();
                  iConnectTsiTypes.TransactionStatus status = raw.getStatus();

                  //the same outcome as from using status.toString()
                  System.out.println("Status: " + Utility.TransactionStatusToString(status.getTransactionStatus()));

                  multiTransaction = raw.isMultiTransactionFlag();

                  if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.ReceiptInformation) {
                      multiTransaction = printReceipt(raw, transactionManager);
                  } else if (status.getTransactionStatus() == iConnectTsiTypes.TransactionStatus.Approved) {
                      ResponseType.VoidResponse voidResp = new ResponseType.VoidResponse().validate(raw);
                      if (voidResp == null) {
                          System.out.println("ERROR: Cannot construct void response object from raw. Reported type: " + raw.getType());
                          break;
                      }
                      System.out.println("Successfully voided");
                  }
              } while (multiTransaction);
          }

          System.out.println("Disconnecting ...  ");
          device.disconnect();
          System.out.println("OK");

      } catch (TsiException e) {
          ret = new TsiStatus(e);
          System.out.println(e.getMessage());
      } finally {
          device.dispose();
      }

  }
  */

    private boolean printReceipt(ResponseType.Raw resp, TransactionManager transactionManager) {

        System.out.println("Receipt Information");
        for (Map.Entry<Integer, String> tag : resp.tags().entrySet()) {
            System.out.println("Tag " + Integer.toString(tag.getKey()) + ": " + tag.getValue());
        }

        RequestType.PrintingStatus req = new RequestType.PrintingStatus(
                new iConnectTsiTypes.EcrPrintingStatus(iConnectTsiTypes.EcrPrintingStatus.Ok));
        try {
            transactionManager.sendRequest(req);
        } catch (TsiException e) {
            System.out.println("EXCEPTION : " + e.getMessage());
            return false;
        }
        return true;
    }

}
