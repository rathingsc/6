package com.italiano2774.nativeapp;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

/**
 * Reusable audio helper. v2.9 adds a small local-audio preload buffer and a
 * single user speed preference shared by word/example playback and TTS.
 */
public class AudioPlayer {
    private final Context context;private final ProgressStore progress;private MediaPlayer player,preloaded;private TextToSpeech tts;private int preloadedWordId=-1;private boolean preloadReady=false;
    public AudioPlayer(Context c,ProgressStore p){
        context=c.getApplicationContext();progress=p;
        tts=new TextToSpeech(context,status->{if(status==TextToSpeech.SUCCESS&&tts!=null){tts.setLanguage(Locale.ITALIAN);tts.setSpeechRate(progress.audioSpeed());}});
    }
    public void play(Word w){play(w,progress.audioSpeed());}
    /** Plays a word at an explicit rate while still preferring the bundled/original recording. */
    public void play(Word w,float rate){if(w==null)return;stopActive();float safe=Math.max(0.55f,Math.min(1.25f,rate));if(progress.preferOriginalAudio()&&w.duoAudio!=null&&!w.duoAudio.isEmpty())playRemote(w,safe);else playLocal(w,safe);}
    public void speak(String text){speak(text,progress.audioSpeed());}
    public void speak(String text,float rate){stopActive();if(tts!=null&&text!=null&&!text.trim().isEmpty()){tts.setSpeechRate(Math.max(0.55f,Math.min(1.25f,rate)));tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"sentence");}}

    /** Prepares the next bundled MP3 without touching the UI thread. */
    public void preload(Word w){
        if(w==null||progress.preferOriginalAudio()||w.localAudio==null||w.localAudio.isEmpty())return;
        if(preloaded!=null&&preloadedWordId==w.id)return;releasePreloaded();
        try{
            AssetFileDescriptor afd=context.getAssets().openFd("audio/"+w.localAudio);MediaPlayer mp=new MediaPlayer();mp.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());afd.close();
            preloaded=mp;preloadedWordId=w.id;preloadReady=false;mp.setOnPreparedListener(x->{applySpeed(x,progress.audioSpeed());if(preloaded==x)preloadReady=true;});mp.setOnErrorListener((x,a,b)->{if(preloaded==x)releasePreloaded();return true;});mp.prepareAsync();
        }catch(Exception ignored){releasePreloaded();}
    }

    private void playRemote(Word w,float rate){
        try{player=new MediaPlayer();player.setDataSource(w.duoAudio);player.setOnPreparedListener(mp->{applySpeed(mp,rate);mp.start();});player.setOnErrorListener((mp,a,b)->{releasePlayer();playLocal(w,rate);return true;});player.prepareAsync();}
        catch(Exception e){playLocal(w,rate);}
    }
    private void playLocal(Word w,float rate){
        if(preloaded!=null&&preloadedWordId==w.id){
            player=preloaded;boolean ready=preloadReady;preloaded=null;preloadedWordId=-1;preloadReady=false;
            player.setOnErrorListener((mp,a,b)->{releasePlayer();fallbackTts(w,rate);return true;});
            if(ready){try{applySpeed(player,rate);player.start();}catch(Exception e){releasePlayer();fallbackTts(w,rate);}}
            else player.setOnPreparedListener(mp->{applySpeed(mp,rate);mp.start();});
            return;
        }
        try{AssetFileDescriptor afd=context.getAssets().openFd("audio/"+w.localAudio);player=new MediaPlayer();player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());afd.close();player.setOnPreparedListener(mp->{applySpeed(mp,rate);mp.start();});player.setOnErrorListener((mp,a,b)->{releasePlayer();fallbackTts(w,rate);return true;});player.prepareAsync();}
        catch(Exception e){fallbackTts(w,rate);}
    }
    private void fallbackTts(Word w,float rate){if(tts!=null&&w!=null){tts.setSpeechRate(rate);tts.speak(w.word,TextToSpeech.QUEUE_FLUSH,null,"word");}}
    private void applySpeed(MediaPlayer mp,float rate){try{PlaybackParams params=mp.getPlaybackParams();params.setSpeed(Math.max(0.55f,Math.min(1.25f,rate)));mp.setPlaybackParams(params);}catch(Exception ignored){}}
    private void releasePlayer(){if(player!=null){try{player.release();}catch(Exception ignored){}player=null;}}
    private void releasePreloaded(){if(preloaded!=null){try{preloaded.release();}catch(Exception ignored){}preloaded=null;}preloadedWordId=-1;preloadReady=false;}
    private void stopActive(){if(player!=null){try{player.stop();}catch(Exception ignored){}releasePlayer();}if(tts!=null)tts.stop();}
    public void stop(){stopActive();}
    public void release(){stopActive();releasePreloaded();if(tts!=null){tts.shutdown();tts=null;}}
}
