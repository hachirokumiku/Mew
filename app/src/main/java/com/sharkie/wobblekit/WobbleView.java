package com.sharkie.wobblekit;
import android.content.*;import android.graphics.*;import android.view.*;import java.util.*;
public class WobbleView extends View {
 public static class Zone {float x,y,r,dx,dy,vx,vy;Zone(float a,float b,float c){x=a;y=b;r=c;}}
 public Bitmap picture; public final ArrayList<Zone> zones=new ArrayList<>(); public boolean editing=true;
 private final Paint paint=new Paint(3);private final Paint overlay=new Paint(3);private long last=System.nanoTime();private float previousX,previousY;private Zone selected;
 public WobbleView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
 public void load(Bitmap b){picture=b;zones.clear();invalidate();}
 public void addZone(){zones.add(new Zone(.5f,.5f,.17f));invalidate();}
 public void tick(){long now=System.nanoTime();float dt=Math.min(.04f,(now-last)/1e9f);last=now;for(Zone z:zones){float k=85f,d=12f;z.vx+=(-k*z.dx-d*z.vx)*dt;z.vy+=(-k*z.dy-d*z.vy)*dt;z.dx+=z.vx*dt;z.dy+=z.vy*dt;}invalidate();}
 @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Color.rgb(18,19,26));if(picture==null){paint.setColor(-1);paint.setTextSize(42);c.drawText("Pick an image :3",25,getHeight()/2f,paint);return;}float w=getWidth(),h=getHeight();float s=Math.min(w/picture.getWidth(),h/picture.getHeight());float iw=picture.getWidth()*s,ih=picture.getHeight()*s,ox=(w-iw)/2,oy=(h-ih)/2;
 // Render a tiled mesh: local displacement falls off smoothly inside each wobble zone.
 int nx=40,ny=60;float[] vertices=new float[(nx+1)*(ny+1)*2];int index=0;for(int y=0;y<=ny;y++)for(int x=0;x<=nx;x++){float u=x/(float)nx,v=y/(float)ny;float px=u*iw,py=v*ih;for(Zone z:zones){float rx=(u-z.x),ry=(v-z.y)*ih/iw;float radius=z.r;float dist=(float)Math.sqrt(rx*rx+ry*ry);if(dist<radius){float f=(float)Math.pow(1-dist/radius,2);px+=z.dx*iw*f;py+=z.dy*ih*f;}}vertices[index++]=px;vertices[index++]=py;}
 c.save();c.translate(ox,oy);paint.setColor(Color.WHITE);c.drawBitmapMesh(picture,nx,ny,vertices,0,null,0,paint);if(editing){overlay.setStyle(Paint.Style.STROKE);overlay.setStrokeWidth(3);overlay.setColor(0xff73e7ff);for(Zone z:zones)c.drawCircle(z.x*iw,z.y*ih,z.r*iw,overlay);overlay.setStyle(Paint.Style.FILL);}c.restore();}
 @Override public boolean onTouchEvent(android.view.MotionEvent e){if(picture==null)return true;float w=getWidth(),h=getHeight();float s=Math.min(w/picture.getWidth(),h/picture.getHeight()),iw=picture.getWidth()*s,ih=picture.getHeight()*s,ox=(w-iw)/2,oy=(h-ih)/2;float u=(e.getX()-ox)/iw,v=(e.getY()-oy)/ih;switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:selected=null;float best=Float.MAX_VALUE;for(Zone z:zones){float d=(u-z.x)*(u-z.x)+(v-z.y)*(v-z.y);if(d<z.r*z.r&&d<best){best=d;selected=z;}}previousX=u;previousY=v;return true;case MotionEvent.ACTION_MOVE:if(selected!=null){float dx=u-previousX,dy=v-previousY;if(editing){selected.x=Math.max(0,Math.min(1,selected.x+dx));selected.y=Math.max(0,Math.min(1,selected.y+dy));}else{selected.vx+=dx*75;selected.vy+=dy*75;}}previousX=u;previousY=v;invalidate();return true;case MotionEvent.ACTION_UP:selected=null;return true;}return true;}
}
