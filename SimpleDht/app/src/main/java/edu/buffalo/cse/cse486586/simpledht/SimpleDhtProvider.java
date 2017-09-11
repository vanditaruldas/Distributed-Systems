package edu.buffalo.cse.cse486586.simpledht;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.net.URI;
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
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static android.content.ContentValues.TAG;

class DBHandler extends SQLiteOpenHelper
{
    public DBHandler(Context context)
    {
        super(context, "SimpleDHT", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String CREATE_TABLE = "CREATE TABLE SIMPLEDHT( key TEXT UNIQUE NOT NULL, value TEXT NOT NULL)";
        db.execSQL(CREATE_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS SIMPLEDHT");
        onCreate(db);
    }
}

public class SimpleDhtProvider extends ContentProvider {
    DBHandler dbHandler;
    String myPort;
    String predPort;
    String succPort;
    String myNodeID;
    String predNodeID;
    String succNodeID;
    static final int SERVER_PORT = 10000;
    Map<String,String> joinedPorts = new HashMap<String, String>();
    ArrayList<String> nodeID = new ArrayList<String>();
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if((selection.equals("*"))||(selection.equals("@")))
        {
            SQLiteDatabase db = dbHandler.getWritableDatabase();
            db.delete("SIMPLEDHT", null, null);
            db.close();
            if ((selection.equals("*")) && ((!succPort.isEmpty()) && (!myPort.equals(succPort))))
            {
                String msg = "";
                if(selectionArgs == null)
                    msg = "delete:" + myPort+":"+selection;
                else
                    msg = selectionArgs[0];
                send(msg, succPort);
            }
        }
        else
        {
            SQLiteDatabase db = dbHandler.getWritableDatabase();
            int rows = db.delete("SIMPLEDHT", "key = ?",new String[] { selection });
            db.close();
            if(rows == 0)
            {
                String msg = "";
                if(selectionArgs == null)
                    msg = "delete:" + myPort+":"+selection;
                else
                    msg = selectionArgs[0];
                send(msg, succPort);
            }
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String keyhash = "";
        try {
            keyhash = genHash(values.getAsString("key"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //Log.v("insert","start");
        if(((keyhash.compareTo(myNodeID)<=0)&&(keyhash.compareTo(predNodeID)>0))||((myNodeID.compareTo(predNodeID)<0)&&((keyhash.compareTo(predNodeID)>0)||(keyhash.compareTo(myNodeID)<=0)))||(myPort.equals(succPort))||(succPort.isEmpty()))
        {
            SQLiteDatabase db = dbHandler.getWritableDatabase();
            db.replace("SIMPLEDHT", null, values);
            db.close();
            Log.v("insertvalues", values.toString());
            //Log.v("insert",keyhash+":"+myNodeID+":"+predNodeID);
        }
        else
            send("insert:"+myPort+":"+values.getAsString("key")+":"+values.getAsString("value")+":"+uri.toString(),succPort);

        //Log.v("insert","end");
        return null;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.v("query",selection);
        Cursor fetchcursor = null;
        if((selection.equals("*"))||(selection.equals("@")))
        {
            SQLiteDatabase db = dbHandler.getReadableDatabase();
            String fetch = "SELECT * FROM SIMPLEDHT";
            fetchcursor = db.rawQuery(fetch, null);
            if ((selection.equals("*")) && ((!succPort.isEmpty()) && (!myPort.equals(succPort)))) {
                String msg = "";
                if(selectionArgs == null)
                    msg = "query:" + myPort+":"+selection;
                else
                    msg = selectionArgs[0];
                String serverRet = send(msg, succPort);
                String[] split = serverRet.split(":");
                MatrixCursor obj = new MatrixCursor(new String[] { "key", "value" });
                if ((split.length > 1) && (!split[1].isEmpty()))
                {
                    String data = split[1];
                    String[] rows = data.split("/");
                    Log.v("ROWS",data);
                    Log.v("ROWS",Integer.toString(data.indexOf("/")));
                    for (int i =0;i<rows.length;i++)
                    {
                        Log.v("ROW",rows[i]);
                        String[] columns = rows[i].split(",");
                        obj.addRow(new String[] { columns[0], columns[1]});
                    }

                    Cursor[] cursors = {fetchcursor, obj};
                    fetchcursor = new MergeCursor(cursors);
                }
            }
        }
        else
        {
            SQLiteDatabase db = dbHandler.getReadableDatabase();
            String fetch = "SELECT * FROM SIMPLEDHT WHERE key =\""+ selection +"\"";
            fetchcursor = db.rawQuery(fetch, null);
            if(fetchcursor.getCount() == 0)
            {
                Log.v("query", "forward");
                String msg = "";
                if(selectionArgs == null)
                    msg = "query:" + myPort+":"+selection;
                else
                    msg = selectionArgs[0];
                String serverRet = send(msg, succPort);
                String[] split = serverRet.split(":");
                MatrixCursor obj = new MatrixCursor(new String[] { "key", "value" });
                Log.v("SPLIT",serverRet);
                if ((split.length > 1) && (!split[1].isEmpty()))
                {
                    String data = split[1];
                    String[] rows = data.split("/");
                    Log.v("ROWS",data);
                    Log.v("ROWS",Integer.toString(data.indexOf("/")));
                    for (int i =0;i<rows.length;i++)
                    {
                        Log.v("ROW",rows[i]);
                        String[] columns = rows[i].split(",");
                        obj.addRow(new String[] { columns[0], columns[1]});
                    }

                    Cursor[] cursors = {obj};
                    fetchcursor = new MergeCursor(cursors);
                }
            }
        }

        Log.v("Query",Integer.toString(fetchcursor.getCount()));
        return fetchcursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }

        Log.v("KEY",input+":"+formatter.toString());
        return formatter.toString();
    }

    @Override
    public boolean onCreate() {
        dbHandler = new DBHandler(this.getContext());
        TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        try {
            myNodeID = genHash(Integer.toString((Integer.parseInt(myPort)/2)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
        }

        return false;
    }

    public String send(String msg,String port)
    {
        String serverRet = "";
        try
        {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[] {10, 0, 2, 2}),
                    Integer.parseInt(port)));
            OutputStream outgoing = socket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outgoing);
            out.writeUTF(msg);
            while (true) {
                InputStream inFromServer = socket.getInputStream();
                DataInputStream in = new DataInputStream(inFromServer);
                serverRet = in.readUTF();
                String split[] = serverRet.split(":");
                if (split[0].equals("PA3-OK"))
                    break;
            }
            socket.close();
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "SocketTimeoutException");
        } catch (UnknownHostException e) {
            Log.e(TAG, "ClientTask UnknownHostException");
        } catch (IOException e) {
            Log.e(TAG, "ClientTask socket IOException");
            //e.printStackTrace();
        }
        //Log.v("ServerReturn",serverRet);
        return serverRet;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Log.v("Server","Start");
            predPort = myPort;
            succPort = myPort;
            try {
                predNodeID = genHash(Integer.toString((Integer.parseInt(myPort)/2)));
                succNodeID = genHash(Integer.toString((Integer.parseInt(myPort)/2)));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            if(!myPort.equals("11108"))
            {
                String msgToSend = "join:"+myPort;
                String serverReturn = send(msgToSend,"11108");
                String[] split = serverReturn.split(":");
                if(split.length > 1)
                {
                    predPort = split[1];
                    succPort = split[2];
                    try {
                        predNodeID = genHash(Integer.toString((Integer.parseInt(split[1])/2)));
                        succNodeID = genHash(Integer.toString((Integer.parseInt(split[2])/2)));
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            }
            else
            {
                joinedPorts.put(myNodeID,myPort);
                nodeID.add(myNodeID);
            }

            while(true)
            {
                try
                {
                    Socket server = serverSocket.accept();
                    String outputstring;
                    DataOutputStream out = new DataOutputStream(server.getOutputStream());
                    String outString = "PA3-OK:";
                    InputStream inFromServer = server.getInputStream();
                    DataInputStream in = new DataInputStream(inFromServer);
                    outputstring = in.readUTF();
                    String[] split = outputstring.split(":");
                    Log.v("msgtype",split[0]);
                    if(split[0].equals("join"))
                    {
                        String nodetemp="";
                        try {
                            nodetemp = genHash(Integer.toString((Integer.parseInt(split[1])/2)));
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                        nodeID.add(nodetemp);
                        joinedPorts.put(nodetemp,split[1]);
                        Collections.sort(nodeID);
                        int position = nodeID.indexOf(nodetemp);
                        int prepos=-1;
                        if(position == 0)
                            prepos = joinedPorts.size()-1;
                        else
                            prepos = position -1;

                        String predid = nodeID.get(prepos);
                        String pred = joinedPorts.get(predid);
                        outString = outString+pred+":";
                        if(pred.equals(myPort))
                        {
                            succPort = split[1];
                            succNodeID = genHash(Integer.toString((Integer.parseInt(split[1])/2)));
                        }
                        else
                        {
                            send("succ:"+split[1],pred);
                        }
                        int succpos = -1;
                        if(position == joinedPorts.size()-1)
                            succpos = 0;
                        else
                            succpos = position + 1;

                        String succid = nodeID.get(succpos);
                        String succ = joinedPorts.get(succid);
                        outString = outString+succ;
                        if(succ.equals(myPort))
                        {
                            predPort = split[1];
                            predNodeID = genHash(Integer.toString((Integer.parseInt(split[1])/2)));
                        }
                        else
                        {
                            send("pred:"+split[1],succ);
                        }
                    }
                    if(split[0].equals("pred"))
                    {
                        predPort = split[1];
                        predNodeID = genHash(Integer.toString((Integer.parseInt(split[1])/2)));
                    }
                    if(split[0].equals("succ"))
                    {
                        succPort = split[1];
                        succNodeID = genHash(Integer.toString((Integer.parseInt(split[1])/2)));
                    }
                    if(split[0].equals("insert"))
                    {
                        ContentValues cv = new ContentValues();
                        cv.put("key", split[2]);
                        cv.put("value", split[3]);
                        Uri uri = Uri.parse(split[4]);
                        String keyhash = "";
                        try {
                            keyhash = genHash(cv.getAsString("key"));
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                        //Log.v("insert",succPort);
                        if (((keyhash.compareTo(myNodeID) <= 0) && (keyhash.compareTo(predNodeID) > 0)) || ((myNodeID.compareTo(predNodeID) < 0) && ((keyhash.compareTo(predNodeID) > 0) || (keyhash.compareTo(myNodeID) <= 0)))) {
                            SQLiteDatabase db = dbHandler.getWritableDatabase();
                            db.replace("SIMPLEDHT", null, cv);
                            db.close();
                            Log.v("insertvalues", cv.toString());
                            //Log.v("insert",keyhash+":"+myNodeID+":"+predNodeID);
                        } else
                            send(outputstring, succPort);
                    }

                    if(split[0].equals("delete"))
                    {
                        if(!split[1].equals(myPort))
                            delete(null,split[2], new String[]{outputstring});
                    }
                    if(split[0].equals("query"))
                    {
                        //Log.v("query", split[1]);
                        if(!split[1].equals(myPort))
                        {
                            //Log.v("query", "forwarded");
                            String temp = "";
                            Cursor obj = query(null, null, split[2], new String[]{outputstring}, null);
                            if (obj != null) {
                                if (obj.moveToFirst()) {
                                    do {
                                        String key = obj.getString(obj.getColumnIndex("key"));
                                        String value = obj.getString(obj.getColumnIndex("value"));
                                        temp = temp +key+","+value+ "/";
                                        Log.v("temp",temp);
                                    } while (obj.moveToNext());
                                    outString = outString + temp;
                                }
                            }
                        }
                    }
                    Log.v("myPort",myPort);
                    Log.v("predPort",predPort);
                    Log.v("succPort",succPort);
                    out.writeUTF(outString);
                    server.close();
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
}
