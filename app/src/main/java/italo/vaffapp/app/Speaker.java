package italo.vaffapp.app;

import android.speech.tts.TextToSpeech;
import android.content.Context;
import java.util.Locale;
/**
 * Created by iarmenti on 8/7/14.
 */
public class Speaker {
    private static TextToSpeech speaker;

    public Speaker(Context context){
        speaker = new TextToSpeech(context,
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if(status != TextToSpeech.ERROR){
                            speaker.setLanguage(Locale.ITALY);
                        }
                    }
                }
        );
    }

    public void speakInsult(String insult_){
        if ( speaker != null )
            speaker.speak(insult_, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void speakEnglish(String desc_){
        if ( speaker != null ) {
            speaker.setLanguage(Locale.ENGLISH);
            speaker.speak(desc_, TextToSpeech.QUEUE_FLUSH, null);
            speaker.setLanguage(Locale.ITALY);
        }
    }

    public static void onPause(){
        if(speaker != null){
            speaker.stop();
            speaker.shutdown();
        }
    }
}
