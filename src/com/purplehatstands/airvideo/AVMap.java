package com.purplehatstands.airvideo;

import java.util.HashMap;

public class AVMap extends HashMap<String, Object> {
  private static final long serialVersionUID = -8477695414350350419L;

  private String name;
  public AVMap(String name) {
    this.name = name;
  }
  
  public String getName() {
    return name;
  }
}
