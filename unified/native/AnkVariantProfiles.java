/*
 * AnkHack native variant selection and per-server/per-world learning.
 * Original AnkHack code. No bundled client engines.
 *
 * A stable airborne interval is weak positive evidence, not a guarantee that
 * a server accepts a movement technique. Position corrections are negative
 * evidence. Never claim to know an untested server's best choice.
 */
package meteordevelopment.meteorclient;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AnkVariantProfiles {
    private static final Path FILE=FabricLoader.getInstance().getConfigDir()
        .resolve("ankhack").resolve("server-variant-scores.properties");
    private static final String[] VARIANTS={"Abilities","Velocity","WurstMotion"};
    private static final Properties storage=new Properties();
    private static boolean initialized;
    private static final Map<String,Integer> sessionCorrections=new ConcurrentHashMap<>();
    private static final Map<String,Integer> sessionStable=new ConcurrentHashMap<>();

    private AnkVariantProfiles() {}

    private static synchronized void init() {
        if(initialized)return;
        initialized=true;
        if(Files.exists(FILE)){
            try(InputStream in=Files.newInputStream(FILE)){storage.load(in);}
            catch(IOException ex){System.err.println("[AnkHack] Cannot read local variant profiles: "+ex.getClass().getSimpleName());}
        }
    }
    private static String sha(String text){
        try{
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();
            for(int i=0;i<12;i++)out.append(String.format(Locale.ROOT,"%02x",digest[i]&0xff));
            return out.toString();
        }catch(Exception e){throw new IllegalStateException("SHA256 unavailable",e);}
    }
    public static String serverKey(MinecraftClient mc){
        if(mc==null)return "unknown";
        try{
            if(mc.isInSingleplayer() && mc.getServer()!=null)
                return sha("singleplayer:"+mc.getServer().getSaveProperties().getLevelName());
            ServerInfo s=mc.getCurrentServerEntry();
            if(s!=null&&s.address!=null&&!s.address.isBlank())return sha("multiplayer:"+s.address);
            if(mc.getNetworkHandler()!=null&&mc.getNetworkHandler().getConnection()!=null){
                var addr=mc.getNetworkHandler().getConnection().getAddress();
                if(addr!=null)return sha("connection:"+addr.toString());
            }
        }catch(RuntimeException ignored){}
        return "unknown";  // Never persist unknown worlds as if they were one server.
    }
    private static String key(String server,String module,String variant,String stat){
        return server+"."+module+"."+variant+"."+stat;
    }
    private static int get(String k){
        init();try{return Math.max(0,Integer.parseInt(storage.getProperty(k,"0")));}catch(NumberFormatException ex){return 0;}
    }
    private static synchronized void increment(String server,String module,String variant,String stat){
        if("unknown".equals(server))return;
        String k=key(server,module,variant,stat);
        storage.setProperty(k,Integer.toString(Math.min(100000,get(k)+1)));
        try{
            Files.createDirectories(FILE.getParent());
            Path temp=FILE.resolveSibling("server-variant-scores.tmp");
            try(OutputStream out=Files.newOutputStream(temp)){storage.store(out,"AnkHack local variant feedback; server identities are hashed");}
            try{Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);}
        }catch(IOException ex){System.err.println("[AnkHack] Cannot persist local variant feedback: "+ex.getClass().getSimpleName());}
    }
    public static void stable(String server,String module,String variant){
        increment(server,module,variant,"stable");
        sessionStable.merge(server+"."+module+"."+variant,1,Integer::sum);
    }
    public static void correction(String server,String module,String variant){
        increment(server,module,variant,"correction");
        sessionCorrections.merge(server+"."+module+"."+variant,1,Integer::sum);
    }
    public static int correctionsThisSession(String server,String module,String variant){
        return sessionCorrections.getOrDefault(server+"."+module+"."+variant,0);
    }
    public static void resetSession(String server,String module){
        for(String v:VARIANTS){
            sessionCorrections.remove(server+"."+module+"."+v);
            sessionStable.remove(server+"."+module+"."+v);
        }
    }
    public static boolean hasEvidence(String server,String module,String variant){
        return get(key(server,module,variant,"stable"))+get(key(server,module,variant,"correction"))>0;
    }
    public static int score(String server,String module,String variant){
        int stable=get(key(server,module,variant,"stable"));
        int setbacks=get(key(server,module,variant,"correction"));
        return stable*3-setbacks*8;
    }
    public static String choose(String server,String module){
        // No evidence -> safest expected creative-like choice. Not a fabricated benchmark.
        String choice="Abilities";int max=Integer.MIN_VALUE;
        for(String variant:VARIANTS) {
            if(!hasEvidence(server,module,variant))continue;
            int score=score(server,module,variant);
            if(score>max){choice=variant;max=score;}
        }
        return choice;
    }
    public static String nextUntested(String server,String module,String current){
        for(String v:VARIANTS){
            if(v.equals(current))continue;
            if(!hasEvidence(server,module,v))return v;
        }
        return choose(server,module);
    }
}
