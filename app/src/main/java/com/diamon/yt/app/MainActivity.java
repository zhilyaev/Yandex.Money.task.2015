package com.diamon.yt.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener {
    private SwipeRefreshLayout mSwipeRefresh;
    public static String strJson = "Данные еще не получены";
    DB database;
    ListView listView;
    SQLiteDatabase sdb;
    List<String> titles;
    List<Integer> ids;
    static Cat[] cats;
    static int WHERE;
    //static List<Integer> history; // Лень

    public void upList(int parent_id){
        WHERE = parent_id;
        titles = new ArrayList<String>();
        ids = new ArrayList<Integer>();
        Cursor cursor = sdb.rawQuery("SELECT * FROM cats WHERE parent_id="+String.valueOf(WHERE),null);
        while (cursor.moveToNext()) {
            titles.add(cursor.getString(cursor.getColumnIndex("title")));
            ids.add(cursor.getInt(cursor.getColumnIndex("id")));
        }
        cursor.close();

        if(WHERE == 0){
           setTitle(R.string.app_name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                MainActivity.this,
                android.R.layout.simple_list_item_1,
                titles );
        listView.setAdapter(adapter);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);
        database = new DB(this);
        sdb = database.getReadableDatabase();

        upList(WHERE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setTitle(String.valueOf(parent.getAdapter().getItem(position)));
                //history.add(position);
                upList(ids.get(position));
            }
        });

        mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        //Настраиваем выполнение OnRefreshListener для данной activity:
        mSwipeRefresh.setOnRefreshListener(this);
        //Настраиваем цветовую тему значка обновления, используя наши цвета:
        mSwipeRefresh.setColorSchemeResources(R.color.light_blue, R.color.middle_blue,R.color.deep_blue);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if(WHERE!=0){
            upList(0);
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            upList(0);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        new LJ().execute();
    }


    /* Получить json c сервера */
    public class LJ extends AsyncTask<Void, Void, String> {
        public String resultJson;

        @Override
        protected String doInBackground(Void... params) {
            try {
                URL url = new URL("https://money.yandex.ru/api/categories-list");

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Соединение установлено", Toast.LENGTH_SHORT).show();
                    }
                });
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {buffer.append(line);}


                resultJson = buffer.toString();


            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Соединение не установлено", Toast.LENGTH_SHORT).show();
                    }
                });
                return null;
            }
            return resultJson;
        }

        @Override
        protected void onPostExecute(String str_Json) {
            super.onPostExecute(str_Json);
            MainActivity.strJson = str_Json;

            Gson gson = new Gson();
            cats = gson.fromJson(MainActivity.strJson, Cat[].class);
            Toast.makeText(MainActivity.this, "Данные о категориях пропарсены", Toast.LENGTH_SHORT).show();

            // 3. ЗАпихать в БД
            sdb = database.getReadableDatabase();
            jsontobd(0,0,cats);
            Toast.makeText(MainActivity.this, "БД обвнолен", Toast.LENGTH_SHORT).show();
            mSwipeRefresh.setRefreshing(false);
            upList(0);
        }
    }

    public int jsontobd(int parent_id,int id, Cat[] catgs){
        for (Cat kitty : catgs){
            id++;
            ContentValues newValues = new ContentValues();
            newValues.put("id", id);
            newValues.put("title", kitty.title);
            newValues.put("parent_id", parent_id);
            sdb.insert("Cats",null,newValues);

            if(kitty.subs!=null){
                id = jsontobd(id,id,kitty.subs);
            }


        }
        return id;
    }


    public class DB extends SQLiteOpenHelper {

        public DB(Context context) {
            // конструктор суперкласса
            super(context, "TY_DB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // 1. Создать таблицу
            db.execSQL("create table Cats ("
                    + "id integer primary key autoincrement,"
                    //+ "id_yt integer,"
                    + "parent_id integer,"
                    + "title text" + ");");
            Toast.makeText(MainActivity.this,"БД Создана", Toast.LENGTH_SHORT).show();
            // 2. Подключиться к API
            new LJ().execute();
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

}
