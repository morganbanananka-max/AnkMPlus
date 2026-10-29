package se.ankclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.*;

/**
 * AnkHack UI, original dark-and-gold frontend. Invisible vanilla click targets,
 * fully custom drawn surfaces. No shader, no unsafe overlay mixin.
 */
public final class ClickGuiScreen extends Screen {
    private static final int BG=0xFF090D13, PANEL=0xFF121924, LAYER=0xFF1B2532,
        HOVER=0xFF283447, GOLD=0xFFFFCD42, GOLD_DIM=0xFF725E2A,
        WHITE=0xFFF4F6FB, MUTED=0xFF91A0B4, GREEN=0xFF69E0AC, RED=0xFFFF8290;
    private static final String[] CATS={"ALL","COMBAT","MOVEMENT","RENDER","WORLD","AUTOMATION","UTILITY","HUD"};
    private static final String[] SYMBOLS={"*","!","~","+","#",">","=","@"};
    private final String category, query, selectedKey;
    private final int page;
    private TextFieldWidget search;
    private int x,y,w,h,rail,listX,listY,listW,visibleRows,detailsX,detailsW,rowH;
    private List<BackendRegistry.Group> shown=List.of();
    private List<BackendRegistry.Group> matched=List.of();
    private BackendRegistry.Group selected;
    private boolean compact;

    public ClickGuiScreen(String cat,String q,int p,String selected,int unused){
        super(Text.literal("AnkHack"));
        category=cat==null?"ALL":cat;
        query=q==null?"":q;
        page=Math.max(0,p);
        selectedKey=selected;
    }
    private void open(String cat,String term,int p,String key){
        client.setScreen(new ClickGuiScreen(cat,term,p,key,0));
    }
    private void hit(String id,int bx,int by,int bw,int bh,Runnable task){
        if(bw<=0 || bh<=0)return;
        ButtonWidget b=ButtonWidget.builder(Text.literal(""),btn->task.run())
            .dimensions(bx,by,bw,bh).build();
        b.setAlpha(0f);
        addDrawableChild(b);
    }
    private List<BackendRegistry.Group> filter(String term){
        String q=term.toLowerCase(Locale.ROOT).trim();
        ArrayList<BackendRegistry.Group> result=new ArrayList<>();
        for(var g:BackendRegistry.groups()){
            if(!category.equals("ALL")&&!category.equals(g.category()))continue;
            if(!q.isEmpty()&&!g.name().toLowerCase(Locale.ROOT).contains(q)
               &&!g.key().contains(q)&&!g.description().toLowerCase(Locale.ROOT).contains(q))continue;
            result.add(g);
        }
        return result;
    }
    private String term(){return search==null?query:search.getText();}
    private String shorten(String s,int chars){
        if(s==null)return "";
        return s.length()>chars?s.substring(0,Math.max(0,chars-2))+"..":s;
    }
    @Override protected void init(){
        w=Math.min(width-18,1050);
        h=Math.min(height-18,660);
        x=(width-w)/2; y=(height-h)/2;
        compact=w<700;
        rail=compact?84:142;
        detailsW=compact?0:Math.min(280,Math.max(210,w/3));
        listX=x+rail+14;
        listW=w-rail-detailsW-34;
        rowH=46;
        listY=y+144;
        visibleRows=Math.max(2,(h-213)/rowH);
        detailsX=listX+listW+12;
        matched=filter(query);
        int pg=Math.min(page,Math.max(0,(matched.size()-1)/visibleRows));
        int from=Math.min(pg*visibleRows,matched.size());
        shown=matched.subList(from,Math.min(from+visibleRows,matched.size()));
        for(var g:matched)if(g.key().equals(selectedKey)){selected=g;break;}
        if(selected==null&&!shown.isEmpty())selected=shown.get(0);

        search=new TextFieldWidget(textRenderer,listX+11,y+92,Math.max(60,listW-94),23,Text.literal("Search modules"));
        search.setMaxLength(80); search.setText(query);search.setSuggestion("Find modules...");
        addDrawableChild(search);
        hit("find",listX+listW-70,y+92,60,23,()->open(category,term(),0,null));
        int navTop=y+132;
        for(int i=0;i<CATS.length;i++){
            String cat=CATS[i];int yy=navTop+i*36;
            if(yy+32>=y+h-79)break;
            hit("category",x+12,yy,rail-20,30,()->open(cat,term(),0,null));
        }
        for(int i=0;i<shown.size();i++){
            var g=shown.get(i);
            int yy=listY+i*rowH;
            hit("feature",listX+7,yy+3,Math.max(25,listW-87),38,
                ()->open(category,term(),pg,g.key()));
            hit("toggle",listX+listW-77,yy+10,67,25,()->{
                BackendRegistry.toggle(g);
                open(category,term(),pg,g.key());
            });
        }
        int bottom=y+h-48;
        hit("previous",listX+12,bottom+3,33,29,()->open(category,term(),Math.max(0,pg-1),selectedKey));
        hit("next",listX+51,bottom+3,33,29,()->open(category,term(),pg+1,selectedKey));
        hit("close",x+w-42,y+16,27,27,this::close);
        if(!compact){
            int cy=y+210;
            if(selected!=null){
                for(int i=0;i<Math.min(3,selected.variants().size());i++){
                    var v=selected.variants().get(i);
                    int yy=cy+i*53;
                    hit("engine",detailsX+9,yy,detailsW-25,46,
                        ()->{BackendRegistry.select(selected,v.provider());open(category,term(),pg,selected.key());});
                    hit("worked",detailsX+detailsW-75,yy+23,24,19,()->{
                        ServerProfiles.feedback(selected.key(),v.provider(),true);open(category,term(),pg,selected.key());
                    });
                    hit("failed",detailsX+detailsW-45,yy+23,24,19,()->{
                        ServerProfiles.feedback(selected.key(),v.provider(),false);open(category,term(),pg,selected.key());
                    });
                }
                hit("auto",detailsX+10,y+380,detailsW-27,26,()->{
                    ServerProfiles.clearSelection(selected.key());open(category,term(),pg,selected.key());
                });
                int sy=y+440;
                WurstBridge.Entry wurst=selected.variants().stream()
                    .map(BackendRegistry.Variant::wurst).filter(Objects::nonNull).findFirst().orElse(null);
                if(wurst!=null){
                    var options=WurstBridge.options(wurst);
                    for(int i=0;i<Math.min(3,options.size());i++){
                        var opt=options.get(i);int yy=sy+26+i*29;
                        if(yy>bottom-21)break;
                        hit("setting_down",detailsX+detailsW-69,yy,23,22,
                            ()->{WurstBridge.adjust(opt,-1);open(category,term(),pg,selected.key());});
                        hit("setting_up",detailsX+detailsW-41,yy,23,22,
                            ()->{WurstBridge.adjust(opt,1);open(category,term(),pg,selected.key());});
                    }
                }
            }
            hit("logging",detailsX+10,bottom,detailsW-24,27,()->{
                AnkLog.setEnabled(!AnkLog.enabled());open(category,term(),pg,selectedKey);
            });
        }
        int foot=y+h-27;
        hit("profile_pvp",x+16,foot-46,Math.min(rail-25,105),21,()->{
            ProfileManager.load("PvP");open(category,term(),pg,selectedKey);
        });
        hit("profile_save",x+16,foot-22,Math.min(rail-25,105),20,()->{
            ProfileManager.save("PvP");open(category,term(),pg,selectedKey);
        });
    }
    private void label(DrawContext d,String s,int px,int py,int color){
        d.drawTextWithShadow(textRenderer,Text.literal(s),px,py,color);
    }
    private void rect(DrawContext d,int a,int b,int wd,int ht,int color){
        d.fill(a,b,a+Math.max(0,wd),b+Math.max(0,ht),color);
    }
    private void border(DrawContext d,int a,int b,int wd,int ht,int c){
        rect(d,a,b,wd,1,c);rect(d,a,b+ht-1,wd,1,c);
        rect(d,a,b,1,ht,c);rect(d,a+wd-1,b,1,ht,c);
    }
    @Override public void render(DrawContext d,int mx,int my,float delta){
        d.fill(0,0,width,height,0xE6090B10);
        rect(d,x+8,y+10,w,h,0x99000000);
        rect(d,x,y,w,h,BG);
        border(d,x,y,w,h,0xFF354053);
        rect(d,x,y,5,h,GOLD);
        rect(d,x+5,y,w-5,63,PANEL);
        rect(d,x+5,y+62,w-5,2,GOLD_DIM);
        rect(d,x+20,y+17,28,27,GOLD);
        label(d,"A",x+30,y+26,BG);
        label(d,"ANKHACK",x+57,y+17,WHITE);
        label(d,"CONTROL CENTER",x+57,y+35,GOLD);
        label(d,"v0.3.2  •  FABRIC 1.21.11",x+170,y+24,MUTED);
        label(d,"FPS "+PerformanceMonitor.latest(),x+w-164,y+25,GREEN);
        rect(d,x+w-42,y+16,27,27,LAYER);
        label(d,"X",x+w-33,y+25,WHITE);

        rect(d,x+11,y+76,rail-16,h-90,PANEL);
        label(d,"SECTIONS",x+21,y+96,GOLD);
        int navTop=y+132;
        for(int i=0;i<CATS.length;i++){
            String cat=CATS[i];int cy=navTop+i*36;
            if(cy+32>=y+h-79)break;
            boolean selectedCat=cat.equals(category);
            boolean hover=mx>=x+12&&mx<x+rail-8&&my>=cy&&my<cy+30;
            if(selectedCat||hover)rect(d,x+13,cy,rail-20,30,selectedCat?LAYER:HOVER);
            if(selectedCat)rect(d,x+13,cy,3,30,GOLD);
            label(d,SYMBOLS[i],x+23,cy+10,selectedCat?GOLD:MUTED);
            label(d,compact?cat.substring(0,Math.min(5,cat.length())):cat,
                x+(compact?38:44),cy+10,selectedCat?WHITE:MUTED);
        }
        label(d,"PROFILES",x+23,y+h-106,GOLD);
        rect(d,x+16,y+h-73,rail-25,21,LAYER);
        label(d,"LOAD PVP",x+24,y+h-66,WHITE);
        rect(d,x+16,y+h-49,rail-25,20,LAYER);
        label(d,"SAVE PVP",x+24,y+h-42,WHITE);

        rect(d,listX,y+76,listW,h-90,PANEL);
        label(d,"MODULE LIBRARY",listX+11,y+81,WHITE);
        label(d,matched.size()+" unique",listX+116,y+82,GOLD);
        rect(d,listX+listW-70,y+92,60,23,GOLD);
        label(d,"SEARCH",listX+listW-62,y+100,BG);
        label(d,"FEATURE",listX+12,y+127,MUTED);
        label(d,"STATE",listX+listW-64,y+127,MUTED);

        for(int i=0;i<shown.size();i++){
            var f=shown.get(i);
            int yy=listY+i*rowH;
            boolean enabled=BackendRegistry.isOn(f);
            boolean picked=selected!=null && f.key().equals(selected.key());
            boolean hover=mx>=listX+5 && mx<=listX+listW-80&&my>=yy&&my<=yy+43;
            rect(d,listX+6,yy+2,listW-12,42,picked?HOVER:(hover?0xFF263345:LAYER));
            if(picked)rect(d,listX+6,yy+2,3,42,GOLD);
            label(d,shorten(f.name(),Math.max(8,(listW-100)/6)),listX+17,yy+9,WHITE);
            String marker=f.variants().size()>1?"MULTI ENGINE":"1 ENGINE";
            label(d,shorten(f.category()+"  /  "+marker,32),listX+17,yy+25,MUTED);
            int sw=listX+listW-77;
            rect(d,sw,yy+10,67,25,enabled?0xFF1C4738:0xFF2A3340);
            border(d,sw,yy+10,67,25,enabled?GREEN:0xFF495568);
            label(d,enabled?"ON":"OFF",sw+25,yy+19,enabled?GREEN:MUTED);
        }
        int bot=y+h-48;
        rect(d,listX+12,bot+3,33,29,LAYER);
        rect(d,listX+51,bot+3,33,29,LAYER);
        label(d,"<",listX+24,bot+12,WHITE);
        label(d,">",listX+64,bot+12,WHITE);
        int pages=Math.max(1,(matched.size()+visibleRows-1)/visibleRows);
        label(d,"PAGE "+Math.min(page+1,pages)+" / "+pages,listX+96,bot+13,MUTED);

        if(!compact){
            rect(d,detailsX,y+76,detailsW-2,h-90,PANEL);
            label(d,"MODULE DETAILS",detailsX+11,y+83,GOLD);
            if(selected!=null){
                label(d,shorten(selected.name(),30),detailsX+11,y+115,WHITE);
                label(d,shorten(selected.description(),36),detailsX+11,y+134,MUTED);
                label(d,"ENGINE SELECTION",detailsX+11,y+179,GOLD);
                int cy=y+210;
                for(int i=0;i<Math.min(3,selected.variants().size());i++){
                    var v=selected.variants().get(i);
                    int yy=cy+i*53;
                    boolean pref=BackendRegistry.preferred(selected)==v;
                    rect(d,detailsX+9,yy,detailsW-25,46,pref?0xFF2D342B:LAYER);
                    if(pref)rect(d,detailsX+9,yy,3,46,GOLD);
                    label(d,shorten(v.provider(),14),detailsX+18,yy+7,pref?GOLD:WHITE);
                    label(d,(pref?"SELECTED  •  ":"")+"SCORE "+ServerProfiles.rating(selected.key(),v.provider()),
                        detailsX+18,yy+26,MUTED);
                    label(d,"+",detailsX+detailsW-67,yy+28,GREEN);
                    label(d,"-",detailsX+detailsW-37,yy+28,RED);
                }
                rect(d,detailsX+10,y+380,detailsW-27,26,LAYER);
                label(d,"AUTO SELECT ENGINE",detailsX+19,y+388,GOLD);
                var optEntry=selected.variants().stream().map(BackendRegistry.Variant::wurst)
                    .filter(Objects::nonNull).findFirst().orElse(null);
                if(optEntry!=null){
                    label(d,"WURST SETTINGS",detailsX+11,y+426,GOLD);
                    var opts=WurstBridge.options(optEntry);
                    for(int i=0;i<Math.min(3,opts.size());i++){
                        var opt=opts.get(i);int yy=y+466+i*29;
                        if(yy>bot-21)break;
                        label(d,shorten(opt.name(),14),detailsX+11,yy+3,WHITE);
                        label(d,shorten(opt.value(),11),detailsX+98,yy+3,MUTED);
                        rect(d,detailsX+detailsW-69,yy-4,23,22,LAYER);
                        rect(d,detailsX+detailsW-41,yy-4,23,22,LAYER);
                        label(d,"-",detailsX+detailsW-61,yy+3,WHITE);
                        label(d,"+",detailsX+detailsW-33,yy+3,WHITE);
                    }
                } else {
                    label(d,"ENGINE NOTES",detailsX+11,y+426,GOLD);
                    label(d,"Pick one provider per module.",detailsX+11,y+450,MUTED);
                    label(d,"Scores are based on your tests.",detailsX+11,y+465,MUTED);
                }
            } else label(d,"Choose a module to configure",detailsX+11,y+120,MUTED);
            rect(d,detailsX+10,bot,detailsW-24,27,LAYER);
            label(d,AnkLog.enabled()?"LOGGING  •  ON":"LOGGING  •  OFF",detailsX+20,bot+9,AnkLog.enabled()?GREEN:MUTED);
        }
        super.render(d,mx,my,delta);
    }
    @Override public boolean shouldPause(){return false;}
}
