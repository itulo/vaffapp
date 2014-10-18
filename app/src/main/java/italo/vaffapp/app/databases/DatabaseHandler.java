package italo.vaffapp.app.databases;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteException;
import android.database.Cursor;
import android.content.Context;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.sql.SQLException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

/**
 * Created by iarmenti on 4/26/14.
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 2;

    private static final String DATABASE_NAME = "vaffapp.db";
    private static final String TABLE_INSULTS = "insults";
    private static final String TABLE_REGIONS = "regions";
    private static final String TABLE_VERSION = "version";
    // my db version - increase anytime db content changes
    // if different from "version" table, it copies again the db from the assets folder
    // and writes in the same table the new version number
    private static int DB_VER = 11;

    // Insults Table Columns names
    private static final String KEY_ID = "rowid";
    private static final String KEY_INSULT = "insult";
    private static final String KEY_DESC = "desc";

    private SQLiteDatabase myDataBase;
    private final Context myContext;

    private static final String TAG = "DatabaseHandler";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        myContext = context;
    }

    /**
     * Creates a empty database on the system and rewrites it with your own database.
     **/
    public void createDataBase() throws IOException {

        boolean dbExist = checkDataBase();

        if(dbExist) {
            try {
                if (!checkVersion()) {
                    Log.w(TAG,"version is different");
                    copyDbProcess();

                    writeVersion();
                }
            } catch (SQLiteException e) {
                Log.w(TAG, "version table doesn't exist probably... copying db");
                copyDbProcess();
                writeVersion();
            }
        }
        else {
            copyDbProcess();
        }
    }

    private void copyDbProcess() {
        //By calling this method and empty database will be created into the default system path
        //of your application so we are gonna be able to overwrite that database with our database.
        this.getReadableDatabase();
        try {
            copyDataBase();
        } catch (IOException e) {
            Log.e(TAG, "Error copying db");
        }
    }

    // check if DB_VER is the same as in "version" table
    private boolean checkVersion() {
        return getVersion() == DB_VER;
    }

    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBase(){
        SQLiteDatabase checkDB = null;
        try{
            String myPath = myContext.getDatabasePath(DATABASE_NAME).toString();
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
        }catch(SQLiteException e){
            Log.i(TAG, "DB doesn't exist yet");
        }
        if(checkDB != null){
            checkDB.close();
        }
        return checkDB != null;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transferring bytestream.
     **/
    private void copyDataBase() throws IOException{
        //Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(DATABASE_NAME);
        // Path to the just created empty db
        String outFileName = myContext.getDatabasePath(DATABASE_NAME).toString();

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ( (length = myInput.read(buffer)) > 0 ){
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public void openDataBase() {
        try {
            createDataBase();
        } catch(IOException e) {
            Log.e(TAG, "createDataBase failed");
        }

        String myPath = myContext.getDatabasePath(DATABASE_NAME).toString();
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    }

    public synchronized void close() {
        if(myDataBase != null)
            myDataBase.close();
        super.close();
    }

    /* CRUD METHODS */
    // Adding new insult - not needed
    //public void addInsult(Insult insult) {}

    // Getting single insult - maybe
    //public Contact getContact(int rowid) {}

    // Getting All Insults
    public ArrayList<Insult> getAllInsults() {
        ArrayList<Insult> insultList = new ArrayList<Insult>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_INSULTS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Insult insult = new Insult();
                insult.setInsult(cursor.getString(0)); // 0 is insult
                insult.setDesc(cursor.getString(1)); // 1 is desc
                insult.setEnglish(cursor.getString(2)); //2 is english
                insult.setRegionId(cursor.getInt(3)); // 3 is region id
                insultList.add(insult);
            } while (cursor.moveToNext());
        }

        return insultList;
    }

    // get all regions
    public ArrayList<Region> getAllRegions() {
        ArrayList<Region> regionList = new ArrayList<Region>();
        String selectQuery = "SELECT * FROM " + TABLE_REGIONS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Region region = new Region();
                region.setName(cursor.getString(1)); // 0 is insult
                // Adding region to list
                regionList.add(region);
            } while (cursor.moveToNext());
        }

        return regionList;
    }

    // get version number
    public int getVersion() {
        String selectQuery = "SELECT * FROM " + TABLE_VERSION;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        cursor.moveToFirst();

        return cursor.getInt(0);
    }

    public void writeVersion() {
        String query = "UPDATE " + TABLE_VERSION + " SET ver='" + DB_VER + "'";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(query);
    }

    // Updating single contact - not needed
    //public int updateContact(Contact contact) {}

    // Deleting single contact - not needed
    //public void deleteContact(Contact contact) {}
}
