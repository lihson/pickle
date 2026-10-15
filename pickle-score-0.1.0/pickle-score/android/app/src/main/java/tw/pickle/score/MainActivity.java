package tw.pickle.score;
import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import com.garmin.android.connectiq.*;
import java.util.*;
import org.json.JSONObject;

public class MainActivity extends Activity {
 static final String APP_ID="d4b261fc-19d3-4fe2-a24f-306d04a5b902";
 ConnectIQ iq; IQDevice device; IQApp app=new IQApp(APP_ID);
 Snapshot current; TextView score,call,status; LinearLayout root;
 final Handler handler=new Handler(Looper.getMainLooper());
 long lastReceived=0; boolean ready=false, choosingSession=false;
 final Runnable ticker=new Runnable(){public void run(){
  if(lastReceived>0 && SystemClock.elapsedRealtime()-lastReceived>10000) status.setText("未收到更新，顯示最後已知比分");
  handler.postDelayed(this,2000);
 }};
 @Override public void onCreate(Bundle saved){super.onCreate(saved);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,12,24,12);root.setGravity(Gravity.CENTER);root.setBackgroundColor(Color.rgb(10,22,30));setContentView(root);
  text("匹克球雙打｜G1 → S25",18);
  score=text("— : —",64);call=text("請開啟手錶計分 App",22);status=text("尚未連線",16);
  LinearLayout bar=new LinearLayout(this);root.addView(bar);
  button(bar,"連接 G1",v->connect());button(bar,"復原",v->command("undo",null));button(bar,"修正",v->correct());
  restore(); render();handler.post(ticker);
 }
 TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);root.addView(t);return t;}
 void button(LinearLayout row,String title,View.OnClickListener f){Button b=new Button(this);b.setText(title);row.addView(b);b.setOnClickListener(f);}
 void note(String s){runOnUiThread(()->status.setText(s));}
 void connect(){
  if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},7);return;}
  if(ready){chooseDevice();return;}
  iq=ConnectIQ.getInstance();
  try{iq.initialize(this,true,new ConnectIQ.ConnectIQListener(){
   public void onSdkReady(){ready=true;runOnUiThread(()->chooseDevice());}
   public void onSdkShutDown(){ready=false;note("Garmin 服務已停止");}
   public void onInitializeError(ConnectIQ.IQSdkErrorStatus s){note("初始化失敗："+s);}
  });}catch(Exception e){note("連線錯誤："+e.getMessage());}
 }
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] grants){super.onRequestPermissionsResult(r,p,grants);if(r==7&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)connect();}
 void chooseDevice(){try{
  List<IQDevice> list=iq.getKnownDevices();
  if(list==null||list.isEmpty()){note("請先在 Garmin Connect 配對 G1");return;}
  String[] names=new String[list.size()];for(int i=0;i<names.length;i++)names[i]=list.get(i).getFriendlyName();
  new AlertDialog.Builder(this).setTitle("選擇記分手錶").setItems(names,(d,n)->subscribe(list.get(n))).show();
 }catch(Exception e){note("無法列出裝置："+e.getMessage());}}
 void subscribe(IQDevice selected){try{
  if(device!=null){iq.unregisterAllForEvents();iq.unregisterForDeviceEvents(device);}
  device=selected;
  iq.registerForDeviceEvents(device,(d,s)->note("藍牙："+s+"；等候比分"));
  iq.registerForAppEvents(device,app,(d,a,data,s)->{
   if(s!=ConnectIQ.IQMessageStatus.SUCCESS){note("接收失敗："+s);return;}
   if(data!=null)for(Object obj:data)if(obj instanceof Map<?,?> map){
    try{Snapshot snap=Snapshot.parse(map);runOnUiThread(()->receive(snap));}catch(IllegalArgumentException e){note("忽略無效資料："+e.getMessage());}
   }
  });note("已訂閱；請在 G1 開啟 App 並按 START 同步");
 }catch(Exception e){note("訂閱失敗："+e.getMessage());}}
 void receive(Snapshot s){
  if(current!=null&&!current.session().equals(s.session())){
   if(choosingSession)return;choosingSession=true;
   new AlertDialog.Builder(this).setTitle("手錶已開始另一場比賽").setMessage("改為顯示手錶目前的比賽？")
    .setPositiveButton("切換",(d,n)->{choosingSession=false;accept(s);})
    .setNegativeButton("暫不切換",(d,n)->choosingSession=false)
    .setOnCancelListener(d->choosingSession=false).show();return;
  }
  if(current!=null&&s.revision()<current.revision())return;
  if(current!=null&&s.revision()==current.revision()&&!s.state().equals(current.state())){note("相同版本資料矛盾，請重新連線");return;}
  accept(s);
 }
 void accept(Snapshot s){
  current=s;lastReceived=SystemClock.elapsedRealtime();if(!persist()){note("本機保存失敗，未回覆同步確認");return;}render();
  HashMap<String,Object> ack=new HashMap<>();ack.put("v",1);ack.put("type","ack");ack.put("session",s.session());ack.put("revision",s.revision());send(ack,false);
 }
 void render(){if(current==null)return;var s=current.state();score.setText("A "+s.a()+" : "+s.b()+" B");
  int first=s.serving()==0?s.a():s.b(),second=s.serving()==0?s.b():s.a();
  call.setText((s.finished()?"本局結束｜":"")+(s.serving()==0?"A":"B")+" 隊發球 · 第 "+s.server()+" 發球員\n"+first+"－"+second+"－"+s.server());
  status.setText(lastReceived==0?"上次保存的比分；尚未收到即時資料":"已收到手錶比分 · 版本 "+current.revision());
 }
 void command(String action,Map<String,Object> fields){
  if(current==null||device==null||lastReceived==0||SystemClock.elapsedRealtime()-lastReceived>10000){note("請先連線並收到最新比分");return;}
  HashMap<String,Object> m=new HashMap<>();m.put("v",1);m.put("type","command");m.put("session",current.session());m.put("base",current.revision());m.put("action",action);if(fields!=null)m.putAll(fields);send(m,true);
 }
 void send(Map<String,Object> m,boolean isCommand){try{
  if(iq==null||device==null)return;
  iq.sendMessage(device,app,m,(d,a,s)->{
   if(s!=ConnectIQ.IQMessageStatus.SUCCESS)note("傳送失敗："+s);
   else if(isCommand)note("已送出修正；等候手錶確認（版本若已變動，請重試）");
  });
 }catch(Exception e){note("傳送錯誤："+e.getMessage());}}
 void correct(){if(current==null)return;Snapshot editing=current;var s=current.state();
  LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(30,10,30,10);
  EditText a=number(form,"A 隊分數",s.a()),b=number(form,"B 隊分數",s.b()),team=number(form,"發球隊：0=A、1=B",s.serving()),server=number(form,"第幾發球員：1 或 2",s.server());
  new AlertDialog.Builder(this).setTitle("修正比分與發球狀態").setView(form).setNegativeButton("取消",null).setPositiveButton("送到手錶",(d,n)->{
   try{if(!editing.equals(current)){note("比分已變動，請重新開啟修正");return;}int av=Integer.parseInt(a.getText().toString()),bv=Integer.parseInt(b.getText().toString()),tv=Integer.parseInt(team.getText().toString()),sv=Integer.parseInt(server.getText().toString());
    new ScoreRules.State(av,bv,tv,sv);if(av>99||bv>99)throw new IllegalArgumentException();
    Map<String,Object> fields=new HashMap<>();fields.put("a",av);fields.put("b",bv);fields.put("serving",tv);fields.put("server",sv);command("set",fields);
   }catch(Exception e){note("輸入無效，請重新修正");}
  }).show();
 }
 EditText number(LinearLayout form,String label,int v){TextView t=new TextView(this);t.setText(label);form.addView(t);EditText e=new EditText(this);e.setInputType(2);e.setText(""+v);form.addView(e);return e;}
 boolean persist(){try{JSONObject o=new JSONObject();o.put("v",1);o.put("type","state");o.put("session",current.session());o.put("revision",current.revision());o.put("a",current.state().a());o.put("b",current.state().b());o.put("serving",current.state().serving());o.put("server",current.state().server());return getPreferences(0).edit().putString("state",o.toString()).commit();}catch(Exception e){return false;}}
 void restore(){try{String raw=getPreferences(0).getString("state",null);if(raw==null)return;JSONObject o=new JSONObject(raw);Map<String,Object> m=new HashMap<>();Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next();m.put(k,o.get(k));}current=Snapshot.parse(m);}catch(Exception ignored){current=null;}}
 @Override protected void onDestroy(){handler.removeCallbacks(ticker);try{if(iq!=null)iq.shutdown(this);}catch(Exception ignored){}super.onDestroy();}
}
