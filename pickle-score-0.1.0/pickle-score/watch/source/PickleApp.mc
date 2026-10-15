using Toybox.Application;
using Toybox.WatchUi;
using Toybox.Graphics;
using Toybox.Communications;
using Toybox.Timer;
using Toybox.Time;
using Toybox.System;
using Toybox.Lang;

class PickleApp extends Application.AppBase {
 var state; var history = []; var revision = 0; var session;
 var synced = false; var busy = false; var timer; var sentAt=0; var ackAt=0;
 function initialize() { AppBase.initialize(); }
 function onStart(saved) {
  var record = Application.Storage.getValue("match");
  if (record instanceof Lang.Dictionary) {
   state = record["state"]; history = record["history"];
   revision = record["revision"]; session = record["session"];
  } else { newMatch(0); }
  Communications.registerForPhoneAppMessages(method(:receive));
  timer = new Timer.Timer(); timer.start(method(:send), 3000, true);
 }
 function onStop(saved) { persist(); if(timer != null) {timer.stop();} }
 function getInitialView() { return [new ScoreView(self), new ScoreInput(self)]; }
 function persist() {
  Application.Storage.setValue("match", {"state"=>state,"history"=>history,"revision"=>revision,"session"=>session});
 }
 function newMatch(team) {
  session = Time.now().value().toString()+"-"+System.getTimer().toString();
  state = {"a"=>0,"b"=>0,"serving"=>team,"server"=>2}; history=[]; revision=0;
  synced=false; persist();
 }
 function finished() { return (state["a"]>=11 || state["b"]>=11) && (state["a"]-state["b"]>=2 || state["b"]-state["a"]>=2); }
 function remember() {
  history.add(state);
  if(history.size()>20) { history=history.slice(1,history.size()); }
  state={"a"=>state["a"],"b"=>state["b"],"serving"=>state["serving"],"server"=>state["server"]};
 }
 function changed() { revision+=1; synced=false; persist(); WatchUi.requestUpdate(); send(); }
 function rally(team) {
  if(finished()) {return;}
  remember();
  if(team==state["serving"]) {
   if(team==0) {state["a"]+=1;} else {state["b"]+=1;}
  } else if(state["server"]==1) {state["server"]=2;}
  else {state["serving"]=1-state["serving"];state["server"]=1;}
  changed();
 }
 function undo() {
  if(history.size()==0) {return;}
  state=history[history.size()-1]; history=history.slice(0,history.size()-1); changed();
 }
 function send() as Void {
  if(synced && System.getTimer()-ackAt>10000) {synced=false;WatchUi.requestUpdate();}
  if(busy && System.getTimer()-sentAt<10000) {return;} busy=true;sentAt=System.getTimer();
  try {
   Communications.transmit({"v"=>1,"type"=>"state","session"=>session,"revision"=>revision,
    "a"=>state["a"],"b"=>state["b"],"serving"=>state["serving"],"server"=>state["server"]},null,new SendListener(self));
  } catch(e) {busy=false;synced=false;WatchUi.requestUpdate();}
 }
 function receive(msg as Communications.PhoneAppMessage) as Void {
  var d=msg.data;
  if(!(d instanceof Lang.Dictionary) || d["v"]!=1 || d["session"]!=session) {return;}
  if(d["type"]=="ack" && d["revision"]==revision) {synced=true;ackAt=System.getTimer();WatchUi.requestUpdate();return;}
  if(d["type"]!="command" || d["base"]!=revision) {return;}
  if(d["action"]=="undo") {undo();}
  else if(d["action"]=="set") {
   var a=d["a"];var b=d["b"];var t=d["serving"];var s=d["server"];
   if(!(a instanceof Lang.Number) || !(b instanceof Lang.Number) || !(t instanceof Lang.Number) || !(s instanceof Lang.Number)) {return;}
   if(a<0 || a>99 || b<0 || b>99 || t<0 || t>1 || s<1 || s>2) {return;}
   remember();state={"a"=>a,"b"=>b,"serving"=>t,"server"=>s};changed();
  }
 }
}
class SendListener extends Communications.ConnectionListener {
 var app;
 function initialize(a) {ConnectionListener.initialize();app=a;}
 function onComplete() {app.busy=false;}
 function onError() {app.busy=false;app.synced=false;WatchUi.requestUpdate();}
}
class ScoreView extends WatchUi.View {
 var app;
 function initialize(a) {View.initialize();app=a;}
 function onUpdate(dc) {
  dc.setColor(Graphics.COLOR_BLACK,Graphics.COLOR_WHITE);dc.clear();var x=dc.getWidth()/2;
  dc.drawText(x,8,Graphics.FONT_XTINY,"A       B",Graphics.TEXT_JUSTIFY_CENTER);
  dc.drawText(x,35,Graphics.FONT_LARGE,app.state["a"].toString()+" : "+app.state["b"].toString(),Graphics.TEXT_JUSTIFY_CENTER);
  var team=app.state["serving"]==0?"A":"B";
  dc.drawText(x,83,Graphics.FONT_SMALL,app.finished()?"GAME":team+" SERVE "+app.state["server"].toString(),Graphics.TEXT_JUSTIFY_CENTER);
  dc.drawText(x,112,Graphics.FONT_XTINY,"UP:A  DOWN:B",Graphics.TEXT_JUSTIFY_CENTER);
  dc.drawText(x,136,Graphics.FONT_XTINY,app.synced?"SYNC OK":"NOT CONFIRMED",Graphics.TEXT_JUSTIFY_CENTER);
 }
}
class ScoreInput extends WatchUi.BehaviorDelegate {
 var app; var last=-1000;
 function initialize(a) {BehaviorDelegate.initialize();app=a;}
 function allowed() {var n=System.getTimer();if(n-last<450){return false;}last=n;return true;}
 function onPreviousPage() {if(allowed()){app.rally(0);}return true;}
 function onNextPage() {if(allowed()){app.rally(1);}return true;}
 function onBack() {if(allowed()){app.undo();}return true;}
 function onSelect() {app.send();return true;}
 function onMenu() {
  var m=new WatchUi.Menu2({:title=>"New game?"});
  m.addItem(new WatchUi.MenuItem("A serves",null,:a,{}));
  m.addItem(new WatchUi.MenuItem("B serves",null,:b,{}));
  m.addItem(new WatchUi.MenuItem("Exit app",null,:exit,{}));
  WatchUi.pushView(m,new SetupInput(app),WatchUi.SLIDE_UP);return true;
 }
}
class SetupInput extends WatchUi.Menu2InputDelegate {
 var app;
 function initialize(a) {Menu2InputDelegate.initialize();app=a;}
 function onSelect(item) {
  if(item.getId()==:exit) {System.exit();return;}
  app.newMatch(item.getId()==:a?0:1);WatchUi.popView(WatchUi.SLIDE_DOWN);WatchUi.requestUpdate();app.send();
 }
}
