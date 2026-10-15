package tw.pickle.score;
import java.util.Map;
/** Validated immutable wire state, also used by the real Android receiver. */
public record Snapshot(String session,int revision,ScoreRules.State state) {
 public static Snapshot parse(Map<?,?> m) {
  if(!"state".equals(m.get("type")) || number(m,"v")!=1) throw new IllegalArgumentException("Protocol");
  if(!(m.get("session") instanceof String id) || id.isEmpty() || id.length()>100) throw new IllegalArgumentException("Session");
  int r=number(m,"revision"),a=number(m,"a"),b=number(m,"b");
  if(r<0 || a>99 || b>99) throw new IllegalArgumentException("Range");
  return new Snapshot(id,r,new ScoreRules.State(a,b,number(m,"serving"),number(m,"server")));
 }
 static int number(Map<?,?> m,String key) {
  if(!(m.get(key) instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue()!=n.intValue()) throw new IllegalArgumentException(key);
  return n.intValue();
 }
 public boolean supersedes(Snapshot old){return old==null || (session.equals(old.session)&&revision>old.revision);}
}
