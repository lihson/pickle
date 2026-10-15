package tw.pickle.score;
/** Portable reference model; watch is the live authority. */
public final class ScoreRules {
 public record State(int a,int b,int serving,int server) {
  public State { if(a<0||b<0||serving<0||serving>1||server<1||server>2) throw new IllegalArgumentException(); }
  public boolean finished(){return Math.max(a,b)>=11 && Math.abs(a-b)>=2;}
 }
 public static State initial(int serving){return new State(0,0,serving,2);}
 public static State rally(State s,int winner){
  if(winner<0||winner>1) throw new IllegalArgumentException();
  if(s.finished()) return s;
  if(winner==s.serving()) return new State(s.a()+(winner==0?1:0),s.b()+(winner==1?1:0),s.serving(),s.server());
  return s.server()==1?new State(s.a(),s.b(),s.serving(),2):new State(s.a(),s.b(),1-s.serving(),1);
 }
}
