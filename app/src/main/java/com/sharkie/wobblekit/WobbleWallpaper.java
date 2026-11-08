package com.sharkie.wobblekit;
import android.service.wallpaper.WallpaperService;import android.view.*;import android.os.*;import android.graphics.*;import android.content.*;import java.io.*;
public class WobbleWallpaper extends WallpaperService {
 @Override public Engine onCreateEngine(){return new WEngine();}
 class WEngine extends Engine{Handler handler=new Handler();boolean visible;Bitmap bitmap;WobbleView view;final Runnable draw=new Runnable(){public void run(){render();}};
 WEngine(){view=new WobbleView(WobbleWallpaper.this);view.editing=false;try{String u=getSharedPreferences("MainActivity",0).getString("uri",null);if(u!=null){InputStream in=getContentResolver().openInputStream(android.net.Uri.parse(u));bitmap=BitmapFactory.decodeStream(in);in.close();view.load(bitmap);String z=getSharedPreferences("MainActivity",0).getString("zones","");for(String item:z.split(";")){String[] p=item.split(",");if(p.length==3)view.zones.add(new WobbleView.Zone(Float.parseFloat(p[0]),Float.parseFloat(p[1]),Float.parseFloat(p[2])));}}}catch(Exception ignored){}setTouchEventsEnabled(true);}
 void render(){handler.removeCallbacks(draw);if(!visible)return;Canvas c=null;try{c=getSurfaceHolder().lockCanvas();if(c!=null){view.layout(0,0,c.getWidth(),c.getHeight());view.tick();view.draw(c);}}catch(Exception ignored){}finally{if(c!=null)getSurfaceHolder().unlockCanvasAndPost(c);}handler.postDelayed(draw,33);}
 @Override public void onVisibilityChanged(boolean v){visible=v;if(v)render();else handler.removeCallbacks(draw);} @Override public void onDestroy(){visible=false;handler.removeCallbacks(draw);super.onDestroy();}
 @Override public void onTouchEvent(android.view.MotionEvent event){view.onTouchEvent(event);}
 }
}
