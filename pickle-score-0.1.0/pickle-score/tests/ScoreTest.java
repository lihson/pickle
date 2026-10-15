import tw.pickle.score.*;
import java.util.*;
public class ScoreTest {
 static int checks=0;
 static void check(boolean b){checks++;if(!b)throw new AssertionError("Check "+checks);}
 public static void main(String[] args){
  var s=ScoreRules.initial(0);check(s.equals(new ScoreRules.State(0,0,0,2)));
  s=ScoreRules.rally(s,1);check(s.equals(new ScoreRules.State(0,0,1,1)));
  s=ScoreRules.rally(s,1);check(s.equals(new ScoreRules.State(0,1,1,1)));
  s=ScoreRules.rally(s,0);check(s.equals(new ScoreRules.State(0,1,1,2)));
  s=ScoreRules.rally(s,0);check(s.equals(new ScoreRules.State(0,1,0,1)));
  check(!new ScoreRules.State(11,10,0,1).finished());check(new ScoreRules.State(12,10,0,1).finished());
  var end=new ScoreRules.State(12,10,0,1);check(ScoreRules.rally(end,0).equals(end));
  for(int serving=0;serving<2;serving++)for(int server=1;server<=2;server++)for(int winner=0;winner<2;winner++){
   var before=new ScoreRules.State(4,3,serving,server);var after=ScoreRules.rally(before,winner);
   check(after.a()+after.b()==7+(winner==serving?1:0));
   check(after.serving()==(winner==serving||server==1?serving:1-serving));
   check(after.server()==(winner==serving?server:server==1?2:1));
  }
  Map<String,Object> m=new HashMap<>(Map.of("v",1,"type","state","session","game1","revision",2,"a",5,"b",3,"serving",0,"server",2));
  Snapshot a=Snapshot.parse(m);check(a.state().a()==5);check(a.supersedes(null));
  m.put("revision",1);Snapshot old=Snapshot.parse(m);check(a.supersedes(old));check(!old.supersedes(a));check(!a.supersedes(a));
  m.put("session","other");check(!Snapshot.parse(m).supersedes(a));
  for(Object invalid:List.of(-1,1.5,"5",Double.NaN)){m.put("a",invalid);try{Snapshot.parse(m);throw new AssertionError("accepted invalid");}catch(IllegalArgumentException expected){checks++;}}
  System.out.println("PASS "+checks+" checks (Java rules and Android wire validation; not Monkey C or BLE)");
 }
}
