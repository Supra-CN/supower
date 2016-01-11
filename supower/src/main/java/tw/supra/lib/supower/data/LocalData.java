package tw.supra.lib.supower.data;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.text.TextUtils;

/**
 * 数据库基类；
 *
 * @author supra
 */
public abstract class LocalData {

    public static final String ENABLE_DB_FOREIGN_KEY = "PRAGMA foreign_keys=ON;";
    private static final String LOG_TAG = LocalData.class.getSimpleName();
    private static final String DB_SUB_FIX = ".db";
    private static final String PRE_FIX_ADJUST = "supra";

    public final String PRE_FIX;
    public final int VERSION;
    public final String NAME;
    public final String DATA_NAME;
    public final Context CONTEXT;
    private final SQLiteOpenHelper DB_HELPER;

    private SQLiteDatabase mTmpDbOnInitializing;

    protected LocalData(Context context, String name, int version) {
        this(context, null, name, version);
    }

    protected LocalData(Context context, String prefix, String name, int version) {
        checkName(name);
        checkVersion(version);
        checkContext(context);
        NAME = name;
        VERSION = version;
        CONTEXT = context.getApplicationContext();
        PRE_FIX = TextUtils.isEmpty(prefix) ? PRE_FIX_ADJUST : prefix;
        DATA_NAME = PRE_FIX + "_" + NAME;
        String dbName = (DATA_NAME.endsWith(DB_SUB_FIX) ? DATA_NAME : DATA_NAME + DB_SUB_FIX);
        DB_HELPER = new SQLiteOpenHelper(CONTEXT, dbName, null, VERSION) {

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onConfigure(SQLiteDatabase db) {
                super.onConfigure(db);
                mTmpDbOnInitializing = db;
                // setForeignKeyConstraintsEnabled(db, true);
                onDbConfigure(db);
                mTmpDbOnInitializing = null;
            }

            @Override
            public void onCreate(SQLiteDatabase db) {
                mTmpDbOnInitializing = db;
                // setForeignKeyConstraintsEnabled(db, true);
                onDbCreate(db);
                mTmpDbOnInitializing = null;
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                mTmpDbOnInitializing = db;
                onDbUpgrade(db, oldVersion, newVersion);
                mTmpDbOnInitializing = null;
            }

            @Override
            public void onOpen(SQLiteDatabase db) {
                super.onOpen(db);
                mTmpDbOnInitializing = db;
                // if (!db.isReadOnly()) {
                // // Enable foreign key constraints
                // db.execSQL(DataDef.ENABLE_DB_FOREIGN_KEY);
                // }
                onDbOpen(db);
                mTmpDbOnInitializing = null;
            }
        };
    }

    private static void checkName(String name) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("the arg 'name' can not be empty");
        }
    }

    private static void checkVersion(int version) {
        if (version < 0) {
            throw new IllegalArgumentException("the arg 'version' must be >= 0");
        }
    }

    private static void checkContext(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("the arg 'context' can not be empty");
        }
    }

    public abstract void onDbConfigure(SQLiteDatabase db);

    public abstract void onDbOpen(SQLiteDatabase db);

    public abstract void onDbCreate(SQLiteDatabase db);

    public abstract void onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

    public void close() {
        DB_HELPER.close();
    }

    /**
     * 在SQLiteOpenHelper初始化期间，返回初始化时的临时数据库，避免初始化时死锁；
     *
     * @return 本地数据的db对象
     */
    public SQLiteDatabase getDb() {
        return mTmpDbOnInitializing == null ? DB_HELPER.getWritableDatabase() : mTmpDbOnInitializing;
    }

    public SmartPreferences getPreferences(){
        return SmartPreferences.get(CONTEXT, DATA_NAME, Context.MODE_PRIVATE);
    }

    private void setForeignKeyConstraintsEnabled(SQLiteDatabase db, boolean enable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            setForeignKeyConstraintsEnabledBySql(db, enable);
        } else {
            setForeignKeyConstraintsEnabledByNative(db, enable);
        }
    }

    private void setForeignKeyConstraintsEnabledBySql(SQLiteDatabase db, boolean enable) {
        db.execSQL(ENABLE_DB_FOREIGN_KEY);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setForeignKeyConstraintsEnabledByNative(SQLiteDatabase db, boolean enable) {
        db.setForeignKeyConstraintsEnabled(true);
    }

}
