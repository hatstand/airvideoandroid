/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.hellojni;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import com.purplehatstands.airvideo.AVMap;
import com.purplehatstands.airvideo.AVStream;

import uk.co.halfninja.videokit.UpdateNotifier;
import uk.co.halfninja.videokit.Videokit;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


public class HelloJni extends Activity implements UpdateNotifier
{
    private Videokit vk;
    private Bitmap frame;
    private ImageView image_view;
    private AndroidHttpClient httpClient;
    private ListView listView;
    private ArrayAdapter<String> listAdapter;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        vk = new Videokit(this);
        vk.initialise();
        
        frame = Bitmap.createBitmap(192, 144, Config.RGB_565);

        /*
        image_view = new ImageView(this);
        image_view.setImageBitmap(frame);
        setContentView(image_view);
        */
        setContentView(R.layout.file_list);
        listView = (ListView)findViewById(R.id.file_list_view);
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(listAdapter);
        
        httpClient = AndroidHttpClient.newInstance("AirVideoAndroid");
    }

    @Override
    public void onResume() {
      super.onResume();
      /*
      ByteBuffer buffer = vk.doStuff();
      if (buffer != null) {
        Log.d("HelloJni", "Got buffer from jni:" + buffer.capacity());
        Bitmap frame = Bitmap.createBitmap(192, 144, Config.ARGB_8888);
        frame.copyPixelsFromBuffer(buffer);
        ImageView iv = new ImageView(this);
        iv.setImageBitmap(frame);
        setContentView(iv);
      }
      */
      
      /*
      new AsyncTask<Object, Object, Object>() {
        @Override
        protected Object doInBackground(Object... params) {
          vk.doStuff(frame);
          return null;
        }
      }.execute();
      */
      
      new AsyncTask<Object, Object, Object>() {
        @Override
        protected Object doInBackground(Object... ps) {
          HttpPost post = new HttpPost("http://192.168.0.187:45631/service");
          
          AVMap avMap = new AVMap("air.connect.Request");
          avMap.put("requestURL", "http://192.168.0.187:45631/service");
          avMap.put("clientVersion", 240);
          avMap.put("serviceName", "browseService");
          avMap.put("methodName", "getItems");
          avMap.put("clientIdentifier", "89eae483355719f119d698e8d11e8b356525ecfb");
          
          AVMap params = new AVMap("air.video.BrowseRequest");
          params.put("folderId", "5ADB62B1BB62E0190E4B9C07159265C48F322D84");
          params.put("preloadDetails", 0);
          List<Object> list = new ArrayList<Object>();
          list.add(params);
          avMap.put("parameters", list);
          
          AVStream avStream = new AVStream();
          avStream.write(avMap, 0);
          
          post.setEntity(new ByteArrayEntity(avStream.finish()));
          
          try {
            HttpResponse response = httpClient.execute(post);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            response.getEntity().writeTo(outputStream);
            AVStream result = new AVStream(outputStream.toByteArray());
            AVMap foo = (AVMap)result.read();
            Log.d("AirVideo", foo.getName());
            Log.d("AirVideo", foo.toString());
            
            AVMap results = (AVMap)foo.get("result");
            final List<Object> items = (List<Object>) results.get("items");
            runOnUiThread(new Runnable() {
              
              public void run() {
                // TODO Auto-generated method stub
                for (Object o: items) {
                  AVMap item = (AVMap)o;
                  listAdapter.add((String)item.get("name"));
                }
              }
            });
            
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          
          return null;
        }
      }.execute();
    }
    
    @Override
    public void onPause() {
      super.onPause();
      vk.stop();
    }
    
    public void update() {
      runOnUiThread(new Runnable() {
        
        public void run() {
          image_view.invalidate();
        }
      });
    }
    

    /* this is used to load the 'hello-jni' library on application
     * startup. The library has already been unpacked into
     * /data/data/com.example.HelloJni/lib/libhello-jni.so at
     * installation time by the package manager.
     */
    static {
        System.loadLibrary("videokit");  
    }
}
