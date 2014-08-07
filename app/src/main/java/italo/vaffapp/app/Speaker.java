package italo.vaffapp.app;

import android.speech.tts.TextToSpeech;
import android.content.Context;
import java.util.Locale;
/**
 * Created by iarmenti on 8/7/14.
 */
public class Speaker {
    private static TextToSpeech insult;
    private static TextToSpeech desc;

    public Speaker(Context context){
        insult = new TextToSpeech(context,
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if(status != TextToSpeech.ERROR){
                            insult.setLanguage(Locale.ITALY);
                        }
                        else
                            System.out.println("insult init failed");
                    }
                }
        );

        desc = new TextToSpeech(context,
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if(status != TextToSpeech.ERROR){
                            desc.setLanguage(Locale.ITALY);
                        }
                        else
                            System.out.println("desc init failed");
                    }
                }
        );
    }

    public static void speakInsult(String insult_){
        if ( insult != null )
            insult.speak(insult_, TextToSpeech.QUEUE_FLUSH, null);
    }

    public static void speakDesc(String desc_){
        if ( desc != null )
            desc.speak(desc_, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void onPause(){
        if(insult != null){
            insult.stop();
            insult.shutdown();
        }

        if(desc != null){
            desc.stop();
            desc.shutdown();
        }
    }
}
