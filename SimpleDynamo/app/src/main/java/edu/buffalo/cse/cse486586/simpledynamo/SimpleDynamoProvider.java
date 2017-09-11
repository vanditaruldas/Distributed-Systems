package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class SimpleDynamoProvider extends ContentProvider {
	String myPort;
    HashMap<String,String> joinedPorts = new HashMap<String, String>();
    ArrayList<String> nodeID = new ArrayList<String>();
    String succ;
    HashMap<String,HashMap<String,String>> avdData = new HashMap<String, HashMap<String,String>>();
    boolean isRestart = false;
    MatrixCursor returnCursor;
    static final int SERVER_PORT = 10000;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        String keyhash = "";
        try {
            keyhash = genHash(selection);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        for(int i =0;i<5;i++)
        {
            if(((i==0)&&((keyhash.compareTo(nodeID.get(4))>0)||(keyhash.compareTo(nodeID.get(i))<=0)))||((i!=0)&&((keyhash.compareTo(nodeID.get(i))<=0)&&(keyhash.compareTo(nodeID.get(i-1))>0))))
            {
                String port = joinedPorts.get(nodeID.get(i));
                if(port.equals(myPort))
                {
                    HashMap<String, String> temp = avdData.get(port);
                    temp.remove(selection);
                    avdData.put(port, temp);

                    int succind = i + 1;
                    if (succind > 4)
                        succind = 0;

                    String succports = joinedPorts.get(nodeID.get(succind));
                    send("delete:" + port + ":" + selection, succports);

                    succind = succind + 1;
                    if (succind > 4)
                        succind = 0;

                    succports = joinedPorts.get(nodeID.get(succind));
                    send("delete:" + port + ":" + selection, succports);
                }
                break;
            }
        }
		return 0;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
        String keyhash = "";
        try {
            keyhash = genHash(values.getAsString("key"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        for(int i =0;i<5;i++)
        {
            if(((i==0)&&((keyhash.compareTo(nodeID.get(4))>0)||(keyhash.compareTo(nodeID.get(i))<=0)))||((i!=0)&&((keyhash.compareTo(nodeID.get(i))<=0)&&(keyhash.compareTo(nodeID.get(i-1))>0))))
            {
                String port = joinedPorts.get(nodeID.get(i));
                if(port.equals(myPort))
                {
                    HashMap<String, String> temp = avdData.get(port);
                    temp.put(values.getAsString("key"), values.getAsString("value"));
                    avdData.put(port, temp);
                    Log.v("insert",values.getAsString("key"));
                }
                else
                    send("insert:"+port+":"+values.getAsString("key")+":"+values.getAsString("value"),port);

                int succind = i + 1;
                if(succind > 4)
                    succind = 0;

                String succports = joinedPorts.get(nodeID.get(succind));
                if(succports.equals(myPort))
                {
                    HashMap<String,String> temp1 = avdData.get(port);
                    temp1.put(values.getAsString("key"),values.getAsString("value"));
                    avdData.put(port,temp1);
                    Log.v("insert",values.getAsString("key"));
                }
                else
                    send("insert:"+port+":"+values.getAsString("key")+":"+values.getAsString("value"),succports);

                succind = succind + 1;
                if(succind > 4)
                    succind = 0;

                succports = joinedPorts.get(nodeID.get(succind));

                if(succports.equals(myPort))
                {
                    HashMap<String,String> temp1 = avdData.get(port);
                    temp1.put(values.getAsString("key"),values.getAsString("value"));
                    avdData.put(port,temp1);
                    Log.v("insert",values.getAsString("key"));
                }
                else
                    send("insert:"+port+":"+values.getAsString("key")+":"+values.getAsString("value"),succports);

                break;
            }
        }
		return null;
	}

	@Override
	public boolean onCreate() {
		TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        SharedPreferences checkrestart = this.getContext().getSharedPreferences("restart",0);
        if(checkrestart.getBoolean("isRestart",true))
        {
            checkrestart.edit().putBoolean("isRestart",false).commit();
        }
        else
        {
            isRestart = true;
        }

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
        }


		return false;
	}

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try
            {
                nodeID.add(genHash("5554"));
                joinedPorts.put(genHash("5554"),"11108");
                nodeID.add(genHash("5556"));
                joinedPorts.put(genHash("5556"),"11112");
                nodeID.add(genHash("5558"));
                joinedPorts.put(genHash("5558"),"11116");
                nodeID.add(genHash("5560"));
                joinedPorts.put(genHash("5560"),"11120");
                nodeID.add(genHash("5562"));
                joinedPorts.put(genHash("5562"),"11124");
                Collections.sort(nodeID);
                int position = nodeID.indexOf(genHash(Integer.toString((Integer.parseInt(myPort)/2))));
                int succpos = -1;
                if(position == joinedPorts.size()-1)
                    succpos = 0;
                else
                    succpos = position + 1;

                String succid = nodeID.get(succpos);
                succ = joinedPorts.get(succid);
                if(!isRestart)
                {
                    Log.v("Start",myPort);
                    HashMap<String,String> temp = new HashMap<String,String>();
                    avdData.put(myPort,temp);
                    temp = new HashMap<String,String>();
                    int prepos=-1;
                    if(position == 0)
                        prepos = joinedPorts.size()-1;
                    else
                        prepos = position -1;

                    String predid = nodeID.get(prepos);
                    String pred = joinedPorts.get(predid);
                    avdData.put(pred,temp);
                    temp = new HashMap<String,String>();
                    if(prepos == 0)
                        prepos = joinedPorts.size()-1;
                    else
                        prepos = prepos -1;

                    predid = nodeID.get(prepos);
                    pred = joinedPorts.get(predid);
                    avdData.put(pred,temp);
                }
                else
                {
                    Log.v("Recover",myPort);
                    send("recover:"+myPort,succ);
                    int prepos=-1;
                    if(position == 0)
                        prepos = joinedPorts.size()-1;
                    else
                        prepos = position -1;

                    String predid = nodeID.get(prepos);
                    String pred = joinedPorts.get(predid);
                    send("recover:"+pred,pred);
                    if(prepos == 0)
                        prepos = joinedPorts.size()-1;
                    else
                        prepos = prepos -1;

                    predid = nodeID.get(prepos);
                    pred = joinedPorts.get(predid);
                    send("recover:"+pred,pred);
                }
            }
            catch (NoSuchAlgorithmException e)
            {
                e.printStackTrace();
            }

            while(true)
            {
                try
                {
                    Socket server = serverSocket.accept();
                    String outputstring;
                    InputStream inFromServer = server.getInputStream();
                    DataInputStream in = new DataInputStream(inFromServer);
                    outputstring = in.readUTF();
                    String[] split = outputstring.split(":");
                    Log.v("msgtype",split[0]);
                    if(split[0].equals("insert"))
                    {
                        HashMap<String,String> temp = avdData.get(split[1]);
                        if(temp == null)
                        {
                            Log.v("insert","Invalid port:"+split[1]);
                        }
                        else
                        {
                            Log.v("insert",split[2]);
                            temp.put(split[2],split[3]);
                            avdData.put(split[1],temp);
                        }
                        DataOutputStream out = new DataOutputStream(server.getOutputStream());
                        String outString = "PA3-OK:";
                        out.writeUTF(outString);
                        out.close();
                    }
                    if(split[0].equals("query"))
                    {
                        //Log.v("query",split[1]);
                        if(split[1].equals("*"))
                        {
                            HashMap<String,String> temp = avdData.get(myPort);
                            ObjectOutputStream oos = new ObjectOutputStream(server.getOutputStream());
                            //Log.v("query","*");
                            oos.writeObject(temp);
                            oos.flush();
                            //Log.v("query","*");
                            //oos.close();
                        }
                        else
                        {
                            Log.v("query",split[2]);
                            HashMap<String,String> temp = avdData.get(split[1]);
                            DataOutputStream out = new DataOutputStream(server.getOutputStream());
                            String outString = "PA3-OK:"+temp.get(split[2]);
                            out.writeUTF(outString);
                            out.close();
                        }
                    }
                    if(split[0].equals("recover"))
                    {
                        Log.v("msgtype",outputstring);
                        HashMap<String,String> temp = avdData.get(split[1]);
                        if(temp == null)
                        {
                            Log.v("insert","Invalid port:"+split[1]);
                        }
                        else
                        {
                            ObjectOutputStream oos = new ObjectOutputStream(server.getOutputStream());
                            oos.writeObject(temp);
                            oos.flush();
                        }
                    }
                    if(split[0].equals("delete"))
                    {
                        HashMap<String,String> temp1 = avdData.get(split[1]);
                        temp1.remove(split[2]);
                        avdData.put(split[1],temp1);
                    }
                    Log.v("msgtype","end");
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    break;
                }
            }
            return null;
        }
    }

    public String send(String msg,String port)
    {
        String serverRet = "";
        String[] msgsplit =  msg.split(":");
        try
        {
            Socket socket = new Socket();
            Log.v("Send",port);
            socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[] {10, 0, 2, 2}),
                    Integer.parseInt(port)),5000);
            socket.setSoTimeout(1000);
            Log.v("Send",msgsplit[0]+msgsplit[1]);
            OutputStream outgoing = socket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outgoing);
            if(msgsplit[0].equals("recover"))
            {
                //ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                //oos.flush();
                //Log.v("I am","Here");
                out.writeUTF(msg);
                out.flush();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                //Log.v("Send","Query*");
                HashMap<String,String> temp = (HashMap<String,String>)ois.readObject();
                avdData.put(msgsplit[1],temp);
                //oos.close();
                //ois.close();
            }
            else if((msgsplit[0].equals("query"))&&(msgsplit[1].equals("*")))
            {
                //Log.v("I am","Here");
                out.writeUTF(msg);
                out.flush();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                //Log.v("Send","Query*");
                HashMap<String,String> temp = (HashMap<String,String>)ois.readObject();

                for(Map.Entry<String, String> data : temp.entrySet())
                {
                    returnCursor.addRow(new String[]{data.getKey(),data.getValue()});
                }
            }
            else
            {
                //Log.v("I am","Here");
                out.writeUTF(msg);
                while (true)
                {
                    InputStream inFromServer = socket.getInputStream();
                    DataInputStream in = new DataInputStream(inFromServer);
                    serverRet = in.readUTF();
                    String split[] = serverRet.split(":");
                    if (split[0].equals("PA3-OK"))
                        break;
                }
            }
            out.close();
            socket.close();
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "SocketTimeoutException");
        } catch (UnknownHostException e) {
            Log.e(TAG, "ClientTask UnknownHostException");
        } catch (IOException e) {
            Log.e(TAG, "ClientTask socket IOException");
            if((msgsplit[0].equals("query"))&&(msgsplit[1].equals("*")))
            {
                try
                {
                    Log.v("Recovery","Query*");
                    msg = "recover:"+port;
                    int index = nodeID.indexOf(genHash(Integer.toString((Integer.parseInt(port)/2))));
                    if(index == 4)
                        index = 0;
                    else
                        index ++;

                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[] {10, 0, 2, 2}),
                            Integer.parseInt(joinedPorts.get(nodeID.get(index)))),5000);
                    socket.setSoTimeout(1000);

                    OutputStream outgoing = socket.getOutputStream();
                    DataOutputStream out = new DataOutputStream(outgoing);
                    out.writeUTF(msg);
                    out.flush();
                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                    HashMap<String,String> temp = (HashMap<String,String>)ois.readObject();
                    for(Map.Entry<String, String> data : temp.entrySet())
                    {
                        returnCursor.addRow(new String[]{data.getKey(),data.getValue()});
                    }
                }
                catch(Exception ex)
                {
                    Log.e(TAG, "Exception in recover");
                }
            }
            //e.printStackTrace();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ClientTask socket IOException");
            //e.printStackTrace();
        }

        //Log.v("ServerReturn",serverRet);
        return serverRet;
    }

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

        Cursor fetchcursor;

        if(selection.equals("@"))
        {
            MatrixCursor returnCursorLocal = new MatrixCursor(new String[] { "key", "value" });
            for(Map.Entry<String, HashMap<String, String>> maps : avdData.entrySet())
            {
                for(Map.Entry<String, String> data : maps.getValue().entrySet())
                {
                    returnCursorLocal.addRow(new String[]{data.getKey(),data.getValue()});
                }
            }
            Cursor[] cursors = {returnCursorLocal};
            fetchcursor = new MergeCursor(cursors);
        }
        else if(selection.equals("*"))
        {
            returnCursor = new MatrixCursor(new String[] { "key", "value" });
            HashMap<String,String> temp = avdData.get(myPort);
            for(Map.Entry<String, String> data : temp.entrySet())
            {
                returnCursor.addRow(new String[]{data.getKey(),data.getValue()});
            }

            //Log.v("Query*",myPort);
            if(!myPort.equals("11108"))
                send("query:*","11108");
            if(!myPort.equals("11112"))
                send("query:*","11112");
            if(!myPort.equals("11116"))
                send("query:*","11116");
            if(!myPort.equals("11120"))
                send("query:*","11120");
            if(!myPort.equals("11124"))
                send("query:*","11124");

            Cursor[] cursors = {returnCursor};
            fetchcursor = new MergeCursor(cursors);
        }
        else
        {
            MatrixCursor returnCursorLocal = new MatrixCursor(new String[] { "key", "value" });
            String keyhash = "";
            try {
                keyhash = genHash(selection);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            Log.v("SingleQuery:"+selection,Integer.toString(returnCursorLocal.getCount()));
            for(int i =0;i<5;i++)
            {
                if(((i==0)&&((keyhash.compareTo(nodeID.get(4))>0)||(keyhash.compareTo(nodeID.get(i))<=0)))||((i!=0)&&((keyhash.compareTo(nodeID.get(i))<=0)&&(keyhash.compareTo(nodeID.get(i-1))>0))))
                {
                    String port = joinedPorts.get(nodeID.get(i));
                    if(port.equals(myPort))
                    {
                        HashMap<String, String> temp = avdData.get(port);
                        //Log.v("SingleQueryBefore:"+selection,Integer.toString(returnCursorLocal.getCount()));
                        returnCursorLocal.addRow(new String[]{selection,temp.get(selection)});
                        Log.v("SingleQueryMine:"+selection,port);
                    }
                    else
                    {
                        String serverRet = send("query:" + port + ":" + selection, port);
                        String[] split = serverRet.split(":");
                        //Log.v("SingleQueryBefore:"+selection,Integer.toString(returnCursorLocal.getCount()));
                        if(split.length == 1)
                        {
                            int next = i;
                            if(next == 4)
                                next = 0;
                            else
                                next++;
                            serverRet = send("query:" + port + ":" + selection, joinedPorts.get(nodeID.get(next)));
                            split = serverRet.split(":");
                        }
                        returnCursorLocal.addRow(new String[]{selection,split[1]});
                        Log.v("SingleQueryOther:"+selection,port);
                    }

                    break;
                }
            }
            Cursor[] cursors = {returnCursorLocal};
            fetchcursor = new MergeCursor(cursors);
        }
		return fetchcursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
