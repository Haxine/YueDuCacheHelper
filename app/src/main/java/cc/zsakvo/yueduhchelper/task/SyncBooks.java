package cc.zsakvo.yueduhchelper.task;

import android.os.AsyncTask;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.zsakvo.yueduhchelper.bean.CacheBooks;
import cc.zsakvo.yueduhchelper.listener.SyncBooksListener;


@SuppressWarnings("ALL")
public class SyncBooks extends AsyncTask<String, Void, LinkedHashMap<String, CacheBooks>> {

    private SyncBooksListener sbl;

    public SyncBooks(SyncBooksListener sbl) {
        this.sbl = sbl;
    }

    private List<String> bookSourceList;

//    private LinkedHashMap<String, CacheBooks> books;

    private List<CacheBooks> bookList;

    private String cachePath;

    private long autoBackupTime = 0;
    private long backupTime = 0;
    private long cacheTime = 0;

    private int syncType = 0;

    @Override
    protected LinkedHashMap<String, CacheBooks> doInBackground(String... strings) {
        String autoBackupPath = strings[0]+"autoSave/myBookShelf.json";
        String backupPath = strings[0]+"myBookShelf.json";
        this.cachePath = strings[1];
        File autoBackupFile = new File(autoBackupPath);
        File backupFile = new File(backupPath);
        if (autoBackupFile.exists()) this.autoBackupTime = new File(autoBackupPath).lastModified();
        if (backupFile.exists()) this.backupTime = new File(backupPath).lastModified();
        File bookCache = new File(cachePath);

        if (!bookCache.exists()){
            return null;
        }else {

            if (bookCache.listFiles().length==0) return null;

            for (File cacheDir:bookCache.listFiles()){
                if (!cacheDir.getName().contains("-")) continue;
                if (cacheDir.lastModified()>cacheTime) cacheTime=cacheDir.lastModified();
            }

            if (autoBackupTime>backupTime&&autoBackupTime>cacheTime){
                syncType = 0;
                return readJson(autoBackupFile);
            }else if (backupTime>autoBackupTime&&backupTime>cacheTime){
                syncType = 1;
                return readJson(backupFile);
            }else if (cacheTime>autoBackupTime&&cacheTime>backupTime){
                syncType = 2;
                return syncFromCache();
            }else {
                return null;
            }
        }
    }

    private LinkedHashMap<String, CacheBooks> syncFromCache(){
        LinkedHashMap<String, CacheBooks> books = new LinkedHashMap<>();
        File bookCache = new File(cachePath);
        for (File cacheDir:bookCache.listFiles()){
            String cacheName = cacheDir.getName();
            if (!cacheName.contains("-")) continue;
            String bookName = cacheName.split("-")[0];
            int i = 0;
            for (File chapterCache:cacheDir.listFiles()){
                String chapterCacheName = chapterCache.getName();
                if (!chapterCacheName.substring(chapterCacheName.lastIndexOf("."),chapterCacheName.length()).equals(".nb")) continue;
//                String chapterName = chapterCacheName.replaceAll(".+?-|.nb","");
                i++;
            }
            CacheBooks cb = new CacheBooks();
            cb.setName(bookName);
            cb.setCacheNum(i);
            cb.setCachePath(cacheDir.getAbsolutePath());

            // 修正缓存来源网址
            cacheName = cacheName
                    .replace("http","http://")
                    .replace("https","https://")
                    .replace("www","www.")
                    .replace("api","api.")
                    .replace("com",".com")
                    .replace("cc",".cc")
                    .replace("net",".net")
                    .replace("org",".org")
                    .replace("cn",".cn")
                    .replace("la",".la");
            cb.setCacheInfo("缓存数量：" + i + "\n" + "来源：" + cacheName.split("-")[1]);

            if (books.get(bookName)!=null){
                if (books.get(bookName).getCacheNum()<i){
                    books.put(bookName,cb);
                }else {
                    continue;
                }
            }else {
                books.put(bookName,cb);
            }
        }
        return books;
    }

    private LinkedHashMap<String, CacheBooks> readJson(File jsonFile) {
        LinkedHashMap<String, CacheBooks> books = new LinkedHashMap<>();
        try {
            FileReader r = new FileReader(jsonFile);
            BufferedReader br = new BufferedReader(r);
            StringBuffer json = new StringBuffer();
            String s;
            while ((s = br.readLine()) != null) {
                json = json.append(s).append("\n");
            }
            br.close();
            JSONArray jsonArray = JSON.parseArray(json.toString());
            for (Object object:jsonArray){
                JSONObject jsonBook = (JSONObject) JSONObject.toJSON(object);
                JSONObject bookInfoBean = (JSONObject) JSONObject.toJSON(jsonBook.get("bookInfoBean"));
                CacheBooks cacheBook = new CacheBooks();
                String name = bookInfoBean.get("name").toString();
                String author = bookInfoBean.get("author").toString();
                cacheBook.setName(name);
                String origin = bookInfoBean.get("origin").toString();
                cacheBook.setCachePath(cachePath+name
                        +"-"
                        +bookInfoBean.get("tag")
                        .toString()
                        .replace(":" +
                                "//","")
                        .replace(".","")
                        +"/");
                cacheBook.setCacheInfo("作者：" + author + "\n" + "来源：" + origin);
                books.put(bookInfoBean.get("name")
                        +"-"+bookInfoBean.get("author"),cacheBook);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return books;
    }

    @Override
    protected void onPostExecute(LinkedHashMap<String, CacheBooks> books) {
        super.onPostExecute(books);
        sbl.showBooks(books,syncType);
    }
}


