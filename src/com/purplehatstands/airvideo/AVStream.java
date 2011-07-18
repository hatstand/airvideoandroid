package com.purplehatstands.airvideo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AVStream {
  private ByteBuffer buffer;
  
  public AVStream() {
    buffer = ByteBuffer.allocate(2048);  // 2KB should be enough for everyone.
    buffer.order(ByteOrder.BIG_ENDIAN);
  }
  
  public AVStream(byte[] data) {
    buffer = ByteBuffer.wrap(data);
    buffer.order(ByteOrder.BIG_ENDIAN);
  }
  
  public Object read() {
    byte b = buffer.get();
    switch (b) {
      case 'i':
        return buffer.getInt();
      case 'f':
        return buffer.getDouble();
      case 's':
        return readString();
      case 'n':
        return null;
      case 'o':
        return readMap();
      case 'a':
        return readList();
    }
    
    return null;
  }
  
  public AVMap readMap() {
    String name = readString();
    AVMap map = new AVMap(name);
    
    int version = buffer.getInt();
    int count = buffer.getInt();
    for (int i = 0; i < count; ++i) {
      int keyLength = buffer.getInt();
      byte[] utf8 = new byte[keyLength];
      buffer.get(utf8);
      String key = new String(utf8, Charset.forName("UTF8"));
      map.put(key, read());
    }
    return map;
  }
  
  public List<Object> readList() {
    int counter = buffer.getInt();
    int count = buffer.getInt();
    List<Object> list = new ArrayList<Object>();
    for (int i = 0; i < count; ++i) {
      list.add(read());
    }
    return list;
  }
  
  public String readString() {
    int counter = buffer.getInt();
    int size = buffer.getInt();
    byte[] utf8 = new byte[size];
    buffer.get(utf8);
    return new String(utf8, Charset.forName("UTF8"));
  }
  
  
  public byte[] finish() {
    return buffer.array();
  }
  
  public void write(int x) {
    write('i');
    buffer.putInt(x);
  }
  
  public void write(double x) {
    write('f');
    buffer.putDouble(x);
  }
  
  public void write(char c) {
    buffer.put((byte) (c & 0x00ff));
  }
  
  public void write(String x, int counter) {
    if (x == null) {
      write('n');
    } else {
      write('s');
      buffer.putInt(counter);
      buffer.putInt(x.length());
      byte[] utf8 = x.getBytes(Charset.forName("UTF8"));
      buffer.put(utf8);
    }
  }
  
  public void write(AVMap x, int counter) {
    int version = 1;
    if (x.getName().equals("air.video.ConversionRequest") ||
        x.getName().equals("air.video.BrowseRequest")) {
      version = 240;
    }
    
    write('o');
    buffer.putInt(counter);
    buffer.putInt(x.getName().length());
    buffer.put(x.getName().getBytes(Charset.forName("UTF8")));
    buffer.putInt(version);
    buffer.putInt(x.size());
    
    for (Map.Entry<String, Object> e: x.entrySet()) {
      buffer.putInt(e.getKey().length());
      buffer.put(e.getKey().getBytes(Charset.forName("UTF8")));
      Object o = e.getValue();
      write(o, counter + 1);
    }
  }
  
  public void write(Object o, int counter) {
    if (o instanceof String) {
      write((String)o, counter);
    } else if (o instanceof Integer){
      write(((Integer)o).intValue());
    } else if (o instanceof Double) {
      write(((Double)o).doubleValue());
    } else if (o instanceof AVMap){
      write((AVMap)o, counter);
    } else if (o instanceof List<?>) {
      write(((List<Object>)o), counter);
    } else {
      write('n');
    }
  }
  
  public void write(List<Object> x, int counter) {
    write('a');
    buffer.putInt(counter);
    buffer.putInt(x.size());
    
    for (Object o: x) {
      write(o, counter);
    }
  }
}
