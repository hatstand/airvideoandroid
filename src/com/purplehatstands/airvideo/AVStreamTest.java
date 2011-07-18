package com.purplehatstands.airvideo;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AVStreamTest {
  private AVStream stream;
  @Before
  public void setUp() throws Exception {
    stream = new AVStream();
  }
  
  @Test
  public void writesString() {
    stream.write("foobar", 1);
    byte[] result = stream.finish();
    Assert.assertNotNull(result);
    Assert.assertEquals('s', result[0]);
    
    Assert.assertEquals(0x0, result[1]);
    Assert.assertEquals(0x0, result[2]);
    Assert.assertEquals(0x0, result[3]);
    Assert.assertEquals(0x1, result[4]);  // Big-endian counter
    
    Assert.assertEquals(0x06, result[8]);  // String length
    
    Assert.assertEquals('f', result[9]);
    Assert.assertEquals('o', result[10]);
  }
  
  @Test
  public void writesInteger() {
    stream.write(42, 0);
    byte[] result = stream.finish();
    
    Assert.assertNotNull(result);
    Assert.assertEquals('i', result[0]);
    Assert.assertEquals(42, result[4]);
  }
  
  @Test
  public void writesMap() {
    AVMap foo = new AVMap("air.video.ConversionRequest");
    foo.put("foo", 42);
    foo.put("bar", "baz");
    
    stream.write(foo, 0);
    byte[] result = stream.finish();
    
    Assert.assertEquals('o', result[0]);
    Assert.assertEquals("air.video.ConversionRequest".length(), result[8]);
    Assert.assertEquals('a', result[9]);
    Assert.assertEquals('i', result[10]);
    
    Assert.assertEquals(-16, result[12 + "air.video.ConversionRequest".length()]);  // 240 but signed byte wraparound
    
    Assert.assertEquals(2, result[16 + "air.video.ConversionRequest".length()]);
  }
  
  @Test
  public void writesList() {
    List<Object> foo = new ArrayList<Object>();
    foo.add("foo");
    foo.add(42);
    
    stream.write(foo, 0);
    byte[] result = stream.finish();
    
    Assert.assertEquals('a', result[0]);
    Assert.assertEquals(2, result[8]);
  }
  
  @Test
  public void readsAndWritesString() {
    stream.write("foobar", 0);
    byte[] result = stream.finish();
    
    AVStream reader = new AVStream(result);
    Assert.assertEquals("foobar", reader.read());
  }
  
  @Test
  public void readsAndWritesMap() {
    AVMap foo = new AVMap("air.video.ConversionRequest");
    foo.put("foo", 42);
    foo.put("bar", "baz");
    
    stream.write(foo, 0);
    byte[] result = stream.finish();
    
    AVStream reader = new AVStream(result);
    Object ret = reader.read();
    AVMap map = (AVMap)ret;
    Assert.assertNotNull(map);
    
    Assert.assertEquals(2, map.size());
    Assert.assertEquals(42, map.get("foo"));
    Assert.assertEquals("baz", map.get("bar"));
  }
  
  @Test
  public void readsAndWritesList() {
    List<Object> foo = new ArrayList<Object>();
    foo.add("foo");
    foo.add(42);
    
    stream.write(foo, 0);
    byte[] result = stream.finish();
    
    AVStream reader = new AVStream(result);
    Object ret = reader.read();
    List<Object> list = (List<Object>)ret;
    Assert.assertNotNull(list);
    Assert.assertEquals(2, list.size());
    Assert.assertEquals("foo", list.get(0));
    Assert.assertEquals(42, list.get(1));
  }
  
  @Test
  public void readsAndWritesNestedMaps() {
    AVMap foo = new AVMap("air.video.ConversionRequest");
    foo.put("foo", 42);
    AVMap bar = new AVMap("foobar");
    bar.put("baz", "wibble");
    foo.put("wobble", bar);
    
    stream.write(foo, 0);
    byte[] result = stream.finish();
    
    AVStream reader = new AVStream(result);
    Object ret = reader.read();
    AVMap map = (AVMap)ret;
    Assert.assertNotNull(map);
    Assert.assertEquals(2, foo.size());
    Assert.assertEquals(42, foo.get("foo"));
    Object o = foo.get("wobble");
    AVMap nested = (AVMap)o;
    Assert.assertNotNull(nested);
    Assert.assertEquals(1, nested.size());
    Assert.assertEquals("wibble", nested.get("baz"));
  }

}
