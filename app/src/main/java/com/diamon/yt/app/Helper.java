package com.diamon.yt.app;

import com.google.gson.Gson;

import java.io.*;
import java.net.URL;

public class Helper {

    private static final String path = "categories-list.json";

    public static void my() throws IOException {
        URL url = new URL("https://money.yandex.ru/api/categories-list");
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(),"UTF-8"));
        String str = in.readLine();
        System.out.println(str);

        /* Корявое обновление на диске */
        File old =  new File(path);
        if(old.exists()){
            PrintWriter out = new PrintWriter("tmp.json");
            out.write(str);
            out.close();
            File news = new File("tmp.json");
            if(news.equals(old)){
                FileWriter writer = new FileWriter(path, false);
                writer.write(str);
                writer.close();
                System.out.println("Файл отличался и был перезаписан более новым");
            }
            news.delete();
        }else{
            FileWriter writer = new FileWriter(path, false);
            writer.write(str);
            System.out.println("Файл создан");
        }

        Gson gson = new Gson();
        Cat[] cats = gson.fromJson(str, Cat[].class);

        catchKitty(cats,"");



    }

    // Ловим котов
    public static void catchKitty (Cat[] cats,String s){
        for (Cat kitty : cats){
            if(kitty.id!=null){
                System.out.print(s+"ID="+ kitty.id+" ");
            }
            System.out.print(kitty.title);

            if(kitty.subs!=null){
                System.out.println(":");
                s += "-";
                catchKitty(kitty.subs, s);
                s = s.substring(0,s.length()-1);
            }else{
                System.out.println();
            }

        }
    }
}
