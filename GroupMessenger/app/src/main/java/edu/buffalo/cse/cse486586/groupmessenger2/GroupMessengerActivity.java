package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */

class MultiCastMsg implements Comparable<MultiCastMsg>
{
    private String timeStmp;
    private String msg;
    private int maxKey;
    private String msgtype;
    private String sourcePort;
    private String myPort;
    private int ackRev;

    public int getAckRev() {
        return ackRev;
    }

    public void setAckRev(int ackRev) {
        this.ackRev = ackRev;
    }

    public MultiCastMsg(String incomingMsg) {
        String[] msgsplit = incomingMsg.split(":");
        this.timeStmp = msgsplit[0];
        this.msg = msgsplit[3];
        this.msgtype = msgsplit[1];
        this.maxKey = Integer.parseInt(msgsplit[5]);
        this.myPort = msgsplit[4];
        this.sourcePort = msgsplit[2];
        this.ackRev = 1;
    }

    public String getMsgtype() {
        return msgtype;
    }

    public void setMsgtype(String msgtype) {
        this.msgtype = msgtype;
    }

    public String getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(String sourcePort) {
        this.sourcePort = sourcePort;
    }

    public String getMyPort() {
        return myPort;
    }

    public void setMyPort(String myPort) {
        this.myPort = myPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MultiCastMsg that = (MultiCastMsg) o;

        boolean ret = false;

        return timeStmp.equals(that.timeStmp);

    }

    @Override
    public int compareTo(MultiCastMsg other) {
        int ret = 0;

        if(this.getMaxKey() == other.getMaxKey())
        {
            ret =this.getSourcePort().compareTo(other.getSourcePort());
        }
        else
        {
            if(this.getMaxKey() < other.getMaxKey())
                ret = -1;
            else
                ret = 1;
        }

        return ret;
    }

    public String getTimeStmp() {

        return timeStmp;
    }

    public void setTimeStmp(String timeStmp) {
        this.timeStmp = timeStmp;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getMaxKey() {
        return maxKey;
    }

    public void setMaxKey(int ackRev) {
        this.maxKey = ackRev;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        for (int i = 0; i < timeStmp.length(); i++) {
            hash = hash*31 + timeStmp.charAt(i);
        }
        return hash;
    }
}

public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    List<Integer> ports = new ArrayList<Integer>();
    //Integer[] ports = new Integer[] {Integer.parseInt(REMOTE_PORT0),Integer.parseInt(REMOTE_PORT1),
            //Integer.parseInt(REMOTE_PORT2),Integer.parseInt(REMOTE_PORT3),Integer.parseInt(REMOTE_PORT4)};
    private Uri mUri;
    private ContentValues mContentValue;
    private int seqno = -1;
    private int dbKey = 0;
    private ContentResolver mContentResolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        ports.add(Integer.parseInt(REMOTE_PORT0));
        ports.add(Integer.parseInt(REMOTE_PORT1));
        ports.add(Integer.parseInt(REMOTE_PORT2));
        ports.add(Integer.parseInt(REMOTE_PORT3));
        ports.add(Integer.parseInt(REMOTE_PORT4));

        mContentResolver = getContentResolver();
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
        uriBuilder.scheme("content");
        mUri = uriBuilder.build();

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);
        Log.v("Client",myPort);
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = "msg:"+myPort+":"+ editText.getText().toString() + "\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket server;
            PriorityQueue<MultiCastMsg> queue = new PriorityQueue<MultiCastMsg>();
            String failedPort = "00000";

            while(true)
            {
                try
                {
                    try
                    {
                        serverSocket.setSoTimeout(4000);
                        server = serverSocket.accept();
                    }
                    catch(Exception e)
                    {
                        if(!queue.isEmpty()) {
                            MultiCastMsg peekMsg = queue.peek();

                            do {
                                Log.v("Done", peekMsg.getMaxKey() + ":" + peekMsg.getSourcePort());
                                if (peekMsg.getAckRev() == 5) {
                                    mContentValue = new ContentValues();
                                    mContentValue.put("key", Integer.toString(dbKey));
                                    dbKey++;
                                    mContentValue.put("value", peekMsg.getMsg());
                                    mContentResolver.insert(mUri, mContentValue);
                                    this.publishProgress(peekMsg.getMsg());
                                }
                                queue.remove(peekMsg);
                                peekMsg = queue.peek();
                            } while (!queue.isEmpty());
                        }
                        continue;
                    }
                    Log.v("Server","Start");
                    String outputstring;
                    DataOutputStream out = new DataOutputStream(server.getOutputStream());
                    InputStream inFromServer = server.getInputStream();
                    DataInputStream in = new DataInputStream(inFromServer);
                    outputstring = in.readUTF();
                    MultiCastMsg incMsg = new MultiCastMsg(outputstring);
                    String close="";

                    if(incMsg.getMsgtype().equals("msg"))
                    {
                        seqno=seqno + 1;
                        incMsg.setMaxKey(seqno);
                        close = "PA2B-OK"+":"+String.valueOf(seqno);
                        out.writeUTF(close);
                        queue.add(incMsg);
                        Log.v("Msg", incMsg.getMaxKey() + ":" + incMsg.getSourcePort()+ ":" + incMsg.getMsg());
                    }

                    if(incMsg.getMsgtype().equals("fail"))
                    {
                        close = "PA2B-OK";
                        out.writeUTF(close);
                        failedPort = incMsg.getMyPort();
                        Iterator it = queue.iterator();
                        while (it.hasNext())
                        {
                            MultiCastMsg temp = (MultiCastMsg)it.next();
                            Log.v("Queue", temp.getMaxKey() + ":" + temp.getSourcePort() + ":" + temp.getMsg());
                            if(temp.getSourcePort().equals(failedPort))
                            {
                                queue.remove(temp);
                            }
                        }
                        Log.v("Fail", failedPort);
                    }

                    if(incMsg.getMsgtype().equals("final"))
                    {
                        close = "PA2B-OK";
                        out.writeUTF(close);
                        queue.remove(incMsg);
                        incMsg.setAckRev(5);
                        if(seqno < incMsg.getMaxKey())
                            seqno = incMsg.getMaxKey();
                        queue.add(incMsg);
                        Log.v("Final", incMsg.getMaxKey() + ":" + incMsg.getSourcePort()+ ":" + incMsg.getAckRev()+ ":" + incMsg.getMsg());

                        /*Iterator it = queue.iterator();

                        System.out.println ( "Priority queue values are: ");

                        while (it.hasNext()){
                            MultiCastMsg temp = (MultiCastMsg)it.next();
                            Log.v("Queue", temp.getMaxKey() + ":" + temp.getSourcePort()+ ":" + temp.getMsg());
                        }*/

                        MultiCastMsg peekMsg = queue.peek();
                        //Log.v("ServerFinal", peekMsg.getMaxKey() + ":" + peekMsg.getSourcePort()+ ":" + peekMsg.getAckRev()+ ":" + peekMsg.getMsg());
                        /*while((!queue.isEmpty())&&(peekMsg.getSourcePort().equals(failedPort)))
                        {
                            //Log.v("ServerFinal","Remove");
                                    queue.remove(peekMsg);
                            peekMsg = queue.peek();
                        }*/

                        Log.v("Peek", peekMsg.getMaxKey() + ":" + peekMsg.getSourcePort()+ ":" + peekMsg.getMsg());
                        while((!queue.isEmpty())&&(peekMsg.getAckRev() == 5))
                        {
                            Log.v("ServerFinal", peekMsg.getMaxKey() + ":" + peekMsg.getSourcePort()+ ":" + peekMsg.getMsg());
                            mContentValue = new ContentValues();
                            mContentValue.put("key", Integer.toString(dbKey));
                            dbKey++;
                            mContentValue.put("value", peekMsg.getMsg());
                            mContentResolver.insert(mUri, mContentValue);
                            this.publishProgress(peekMsg.getMsg());
                            queue.remove(peekMsg);
                            peekMsg = queue.peek();
                            /*while ((!queue.isEmpty())&&(peekMsg.getSourcePort().equals(failedPort)))
                            {
                                //Log.v("ServerFinal","Remove");
                                queue.remove(peekMsg);
                                peekMsg = queue.peek();
                            }*/
                            //Log.v("ServerNext", peekMsg.getMaxKey() + ":" + peekMsg.getSourcePort()+ ":" + peekMsg.getMsg());
                        }

                    }
                    server.close();
                    Log.v("Server","Close");
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    break;
                }
            }

            return null;
        }

        protected void onProgressUpdate(String...strings) {
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");

        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        protected void handlefailure(String sourcePort,String failedPort,String msg)
        {
            //ports.remove((Integer) Integer.parseInt(failedPort));
            String msgsplit[] = msg.split(":");
            String handleFail = msgsplit[0]+":"+"fail:"+msgsplit[2]+":"+msgsplit[3]+":"+failedPort+":0";
            try{
            Socket socketfin = new Socket();
            socketfin.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[] {10, 0, 2, 2}),
                    Integer.parseInt(sourcePort)),1500);
            socketfin.setSoTimeout(500);
            OutputStream outgoing = socketfin.getOutputStream();
            DataOutputStream out = new DataOutputStream(outgoing);
            out.writeUTF(handleFail);
            while (true) {
                InputStream inFromServer = socketfin.getInputStream();
                DataInputStream in = new DataInputStream(inFromServer);
                String comp = in.readUTF();
                if (comp.equals("PA2B-OK")) {
                    break;
                }
            }
            socketfin.close();
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "SocketTimeoutException:"+sourcePort);
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
        }

        @Override
        protected Void doInBackground(String... msgs) {

            String myPort = msgs[1];
            int maxKey = 0;
            //Log.v("Client","Start");
            msgs[0] = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())+ ":" + msgs[0];
            for(int i=0;i<ports.size();i++) {
                try {
                    Log.v("Client","Start");
                    String msgToSend = msgs[0]+ ":"+String.valueOf(ports.get(i))+":0" ;
                    Socket socketfin = new Socket();
                    socketfin.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[] {10, 0, 2, 2}),
                            ports.get(i)),2000);
                    socketfin.setSoTimeout(1000);
                    OutputStream outgoing = socketfin.getOutputStream();
                    DataOutputStream out = new DataOutputStream(outgoing);
                    out.writeUTF(msgToSend);
                    Log.v("Client","Write");
                    while (true) {
                        InputStream inFromServer = socketfin.getInputStream();
                        DataInputStream in = new DataInputStream(inFromServer);
                        String comp = in.readUTF();
                        String[] temp = comp.split(":");
                        //Log.v("Client",String.valueOf(ports[i])+":"+temp[1]);
                        if (temp[0].equals("PA2B-OK")) {
                            if (maxKey < Integer.parseInt(temp[1])) {
                                maxKey = Integer.parseInt(temp[1]);
                            }
                            break;
                        }
                    }
                    Log.v("Client","Close");
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "SocketTimeoutException:"+String.valueOf(ports.get(i)));
                    handlefailure(myPort,String.valueOf(ports.get(i)),msgs[0]);
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                    handlefailure(myPort,String.valueOf(ports.get(i)),msgs[0]);
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                    handlefailure(myPort,String.valueOf(ports.get(i)),msgs[0]);
                }
            }

            //Log.v("Client","Close");

            String msgsplit[] = msgs[0].split(":");
            String finalPrioirty = msgsplit[0]+":"+"final:"+msgsplit[2]+":"+msgsplit[3];

            try {
            Thread.sleep(250, 0);
            } catch (Exception ex) {
                Log.e(TAG, "ClientTask Exception");
            }

            for(int i=ports.size()-1;i>=0;i--) {
                try {
                    Log.v("Client","Start");
                    String msgToSend = finalPrioirty+ ":"+String.valueOf(ports.get(i))+":"+String.valueOf(maxKey) ;
                    Socket socketfin = new Socket();
                    socketfin.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[] {10, 0, 2, 2}),
                            ports.get(i)),5000);
                    socketfin.setSoTimeout(1000);
                    OutputStream outgoing = socketfin.getOutputStream();
                    DataOutputStream out = new DataOutputStream(outgoing);
                    out.writeUTF(msgToSend);
                    Log.v("Client","Write");
                    while (true) {
                        InputStream inFromServer = socketfin.getInputStream();
                        DataInputStream in = new DataInputStream(inFromServer);
                        String comp = in.readUTF();
                        if (comp.equals("PA2B-OK")) {
                            break;
                        }
                    }
                    socketfin.close();
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "SocketTimeoutException:"+String.valueOf(ports.get(i)));
                    handlefailure(myPort,String.valueOf(ports.get(i)),msgs[0]);
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                    handlefailure(myPort,String.valueOf(ports.get(i)),msgs[0]);
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                    handlefailure(myPort,String.valueOf(ports.get(i)),msgs[0]);
                    //e.printStackTrace();
                }

                Log.v("Client","Close");
            }

            return null;
        }
    }
}