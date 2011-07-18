package uk.co.halfninja.videokit;

import android.graphics.Bitmap;
import android.util.Log;

public class Videokit {
  private UpdateNotifier listener;
  
  public Videokit(UpdateNotifier listener) {
    this.listener = listener;
  }
  
  public native void initialise();
  public native void doStuff(Bitmap bitmap);
  public native void stop();
  
  private void update() {
    Log.d("Videokit", "Should update bitmap");
    listener.update();
  }
}
