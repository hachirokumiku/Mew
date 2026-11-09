package com.sharkie.wobblekit;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.view.*;import android.widget.*;import java.io.*;
public class MainActivity extends Activity {
 WobbleView view;WobbleMotion motion;Handler handler=new Handler();Runnable animation;TextView status;Button modeButton;static final String PREF="wobble";static final String IMAGE_FILE="wallpaper-image";
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().setStatusBarColor(0xff171326);getWindow().setNavigationBarColor(0xff171326);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xff171326);
  scroll.setClipToPadding(false);
  scroll.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(14),dp(18),dp(18));scroll.addView(root);
  TextView eyebrow=new TextView(this);eyebrow.setText("YOUR LIVE WALLPAPER STUDIO");eyebrow.setTextColor(0xffbd9dff);eyebrow.setTextSize(11);eyebrow.setLetterSpacing(.12f);root.addView(eyebrow);
  TextView title=new TextView(this);title.setText("WobbleKit");title.setTextColor(0xffffffff);title.setTextSize(28);title.setTypeface(null,android.graphics.Typeface.BOLD);root.addView(title);
  status=new TextView(this);status.setTextColor(0xffd0c8e3);status.setTextSize(13);status.setPadding(0,dp(3),0,dp(12));root.addView(status);
  view=new WobbleView(this);view.setContentDescription("Wallpaper preview and zone editor");view.loadPhysics(getPreferences(0));view.loadFrame(getPreferences(0));motion=new WobbleMotion(this,view);
  android.graphics.Point screen=new android.graphics.Point();getWindowManager().getDefaultDisplay().getRealSize(screen);
  if(screen.x>0&&screen.y>0)view.setPreviewAspect((float)screen.x/screen.y);
  int previewHeight=Math.max(dp(240),Math.round(screen.y*.48f));
  root.addView(view,new LinearLayout.LayoutParams(-1,previewHeight));
  TextView hint=new TextView(this);hint.setText("YOUR TOOLS  •  Scroll for more");hint.setTextColor(0xffbd9dff);hint.setTextSize(11);hint.setLetterSpacing(.08f);hint.setPadding(0,dp(18),0,dp(8));root.addView(hint);
  addRow(root,new String[]{"Choose image","Crop & zoom"},new int[]{0,7});
  addRow(root,new String[]{"+ Add zone","Preview"},new int[]{1,2});
  addRow(root,new String[]{"Zone shape","Delete zone"},new int[]{3,4});
  addRow(root,new String[]{"Tune motion","Set wallpaper"},new int[]{5,6});
  setContentView(scroll);scroll.requestApplyInsets();loadSaved();updateMode();animation=()->{view.tick();handler.postDelayed(animation,16);};
 }
 int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
 void addRow(LinearLayout root,String[] labels,int[] actions){
  LinearLayout row=new LinearLayout(this);root.addView(row);
  for(int i=0;i<labels.length;i++){final int action=actions[i];Button button=new Button(this);button.setText(labels[i]);button.setTextSize(14);button.setAllCaps(false);button.setTextColor(0xffffffff);button.setElevation(0);
   android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();background.setColor(action==6?0xff7650d6:action==4?0xff61384e:0xff312940);background.setCornerRadius(dp(14));button.setBackground(background);
   LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(52),1);params.setMargins(i==0?0:dp(6),0,i==0?dp(6):0,dp(10));row.addView(button,params);button.setOnClickListener(v->action(action));if(action==2)modeButton=button;
  }
 }
 void updateMode(){modeButton.setText(view.editing?"Preview":"Edit");status.setText(view.editing?"Edit: tap a zone, drag to move, pinch or drag its dot to resize":"Preview: tap or drag a zone, tilt or gently move your phone");}
 void action(int n){if(n==0){openImagePicker();}else if(n==1){if(view.picture==null){Toast.makeText(this,"Pick an image first.",Toast.LENGTH_SHORT).show();return;}view.editing=true;motion.stop();view.addZone();saveZones();updateMode();}else if(n==2){view.editing=!view.editing;if(view.editing)motion.stop();else if(!motion.start())Toast.makeText(this,"Accelerometer unavailable; touch wobble still works.",Toast.LENGTH_LONG).show();saveZones();updateMode();}else if(n==3){showShapeDialog();}else if(n==4){if(!view.editing||view.activeZone()==null){Toast.makeText(this,"In edit mode, tap a zone first.",Toast.LENGTH_LONG).show();return;}new AlertDialog.Builder(this).setTitle("Delete selected zone?").setNegativeButton("Cancel",null).setPositiveButton("Delete",(dialog,which)->{view.removeActiveZone();saveZones();}).show();}else if(n==5){showPhysicsDialog();}else if(n==7){showFrameDialog();}else{saveZones();Intent i=new Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);i.putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,WobbleWallpaper.class));try{startActivity(i);}catch(ActivityNotFoundException|SecurityException ex){try{startActivity(new Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));Toast.makeText(this,"Select WobbleKit Wallpaper in the chooser.",Toast.LENGTH_LONG).show();}catch(ActivityNotFoundException|SecurityException fallback){Toast.makeText(this,"No live wallpaper picker is available on this device.",Toast.LENGTH_LONG).show();}}}}
 void showFrameDialog(){
  if(view.picture==null){Toast.makeText(this,"Pick an image first.",Toast.LENGTH_SHORT).show();return;}
  SharedPreferences prefs=getPreferences(0);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(12),dp(20),0);
  TextView hint=new TextView(this);hint.setText("Crop fills the phone screen. The original image and zones are kept.");content.addView(hint);
  CheckBox crop=new CheckBox(this);crop.setText("Fill screen (crop)");crop.setChecked(prefs.getBoolean("frameCrop",false));content.addView(crop);
  SeekBar zoom=addSlider(content,"Zoom",100,300,prefs.getInt("frameZoom",100),"%");
  SeekBar x=addSlider(content,"Horizontal position",0,100,prefs.getInt("frameX",50),"%");
  SeekBar y=addSlider(content,"Vertical position",0,100,prefs.getInt("frameY",50),"%");
  Runnable preview=()->view.setFrame(crop.isChecked(),zoom.getProgress()+100,x.getProgress(),y.getProgress());
  crop.setOnCheckedChangeListener((button,checked)->preview.run());
  liveFrameSlider(zoom,"Zoom",100,"%",preview);liveFrameSlider(x,"Horizontal position",0,"%",preview);liveFrameSlider(y,"Vertical position",0,"%",preview);
  ScrollView scroller=new ScrollView(this);scroller.addView(content);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Crop & zoom").setView(scroller).setNegativeButton("Cancel",null).setNeutralButton("Reset",null).setPositiveButton("Apply",null).create();
  dialog.setOnShowListener(ignored->{
   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{prefs.edit().putBoolean("frameCrop",crop.isChecked()).putInt("frameZoom",zoom.getProgress()+100).putInt("frameX",x.getProgress()).putInt("frameY",y.getProgress()).apply();dialog.dismiss();});
   dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{crop.setChecked(false);zoom.setProgress(0);x.setProgress(50);y.setProgress(50);preview.run();});
  });
  dialog.setOnDismissListener(ignored->view.loadFrame(prefs));dialog.show();
 }
 void liveFrameSlider(SeekBar slider,String title,int min,String suffix,Runnable preview){
  LinearLayout parent=(LinearLayout)slider.getParent();TextView label=(TextView)parent.getChildAt(parent.indexOfChild(slider)-1);
  slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){label.setText(title+": "+(progress+min)+suffix);preview.run();}public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}});
 }
 void showShapeDialog(){JellyZone zone=view.activeZone();if(!view.editing||zone==null){Toast.makeText(this,"In edit mode, tap a zone first.",Toast.LENGTH_LONG).show();return;}LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);int padding=(int)(20*getResources().getDisplayMetrics().density);content.setPadding(padding,padding,padding,0);SeekBar width=addSlider(content,"Width",4,50,Math.round(zone.rx*100),"%");SeekBar height=addSlider(content,"Height",4,50,Math.round(zone.ry*100),"%");SeekBar rotation=addSlider(content,"Rotation",-180,180,Math.round(zone.angle)," degrees");Spinner shape=new Spinner(this);shape.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Smooth","Broad","Pointed"}));shape.setSelection(zone.shape);content.addView(shape);new AlertDialog.Builder(this).setTitle("Zone shape").setView(content).setNegativeButton("Cancel",null).setNeutralButton("Delete",(dialog,which)->{view.removeActiveZone();saveZones();}).setPositiveButton("Apply",(dialog,which)->{zone.rx=(width.getProgress()+4)/100f;zone.ry=(height.getProgress()+4)/100f;zone.angle=rotation.getProgress()-180;zone.shape=shape.getSelectedItemPosition();view.invalidate();saveZones();}).show();}
 void showPhysicsDialog(){SharedPreferences prefs=getPreferences(0);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);int padding=(int)(20*getResources().getDisplayMetrics().density);content.setPadding(padding,padding,padding,0);TextView hint=new TextView(this);hint.setText("Applies to all zones in preview and wallpaper.");content.addView(hint);SeekBar strength=addSlider(content,"Strength",0,300,prefs.getInt("strength",100),"%");SeekBar spring=addSlider(content,"Springiness",20,180,prefs.getInt("spring",85),"");SeekBar damping=addSlider(content,"Damping",2,30,prefs.getInt("damping",12),"");new AlertDialog.Builder(this).setTitle("Tune wobble").setView(content).setNegativeButton("Cancel",null).setNeutralButton("Reset",(dialog,which)->{prefs.edit().putInt("strength",100).putInt("spring",85).putInt("damping",12).apply();view.loadPhysics(prefs);}).setPositiveButton("Apply",(dialog,which)->{prefs.edit().putInt("strength",strength.getProgress()).putInt("spring",spring.getProgress()+20).putInt("damping",damping.getProgress()+2).apply();view.loadPhysics(prefs);}).show();}
 SeekBar addSlider(LinearLayout content,String title,int min,int max,int value,String suffix){TextView label=new TextView(this);content.addView(label);SeekBar slider=new SeekBar(this);slider.setMax(max-min);slider.setProgress(Math.max(0,Math.min(max-min,value-min)));label.setText(title+": "+(slider.getProgress()+min)+suffix);slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){label.setText(title+": "+(progress+min)+suffix);}public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}});content.addView(slider);return slider;}
 void openImagePicker(){Intent document=new Intent(Intent.ACTION_OPEN_DOCUMENT);document.setType("image/*");document.addCategory(Intent.CATEGORY_OPENABLE);document.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);try{startActivityForResult(document,1);}catch(android.content.ActivityNotFoundException ex){Intent fallback=new Intent(Intent.ACTION_GET_CONTENT);fallback.setType("image/*");fallback.addCategory(Intent.CATEGORY_OPENABLE);fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivityForResult(fallback,1);}catch(android.content.ActivityNotFoundException fallbackEx){Toast.makeText(this,"No image picker is available on this device.",Toast.LENGTH_LONG).show();}}}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==1&&result==RESULT_OK&&data!=null){try{android.net.Uri uri=data.getData();if(uri==null)throw new IOException("The image picker returned no image.");try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=openFileOutput(IMAGE_FILE,MODE_PRIVATE)){if(in==null)throw new IOException("Unable to read the selected image.");byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);}Bitmap image=ImageLoader.load(new File(getFilesDir(),IMAGE_FILE));if(image==null)throw new IOException("The selected file is not a readable image.");view.load(image);getPreferences(0).edit().putBoolean("hasImage",true).apply();saveZones();}catch(IOException|SecurityException ex){Toast.makeText(this,"Couldn't load image: "+ex.getMessage(),Toast.LENGTH_LONG).show();}}}
 void loadSaved(){try{if(getPreferences(0).getBoolean("hasImage",false)){Bitmap b=ImageLoader.load(new File(getFilesDir(),IMAGE_FILE));if(b==null)throw new IOException("Saved image is unreadable.");view.load(b);String z=getPreferences(0).getString("zones","");for(String item:z.split(";")){if(!item.isEmpty())view.zones.add(JellyZone.parse(item));}}}catch(IOException|IllegalArgumentException ex){Toast.makeText(this,"Couldn't restore saved image or zones: "+ex.getMessage(),Toast.LENGTH_LONG).show();}}
 void saveZones(){StringBuilder b=new StringBuilder();for(JellyZone z:view.zones)b.append(z.serialize()).append(';');getPreferences(0).edit().putString("zones",b.toString()).apply();}
 @Override protected void onResume(){super.onResume();view.loadPhysics(getPreferences(0));view.resetClock();handler.post(animation);if(!view.editing&&!motion.start())Toast.makeText(this,"Accelerometer unavailable; touch wobble still works.",Toast.LENGTH_LONG).show();}
 @Override protected void onPause(){motion.stop();handler.removeCallbacks(animation);saveZones();super.onPause();}
 @Override protected void onDestroy(){motion.stop();handler.removeCallbacks(animation);super.onDestroy();}
}
