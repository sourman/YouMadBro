package com.example.jimmmers.youmadbro;

import com.example.jimmmers.youmadbro.Jae1.src.main.java.appDev;
import com.example.jimmmers.youmadbro.util.SystemUiHider;
import com.ibm.watson.developer_cloud.alchemy.v1.AlchemyLanguage;
import com.ibm.watson.developer_cloud.alchemy.v1.model.DocumentSentiment;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;



public class MainActivity extends Activity {

    private static final boolean AUTO_HIDE = true;

    String[] emotions = {"Sadness","Fear","Anger","Disgust","Joy"};

    private SpeechRecognizer mSpeechRecognizer;
    private Intent mSpeechRecognizerIntent;

    private boolean mIslistening;

    private static final int AUTO_HIDE_DELAY_MILLIS = 1000;

    private Intent i;

    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    private static final String TAG = "M";
    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private String resultText;
    private boolean ready;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ready = false;

        //This is some of the code for the splash screen that needs to be finished
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.splash);
        ImageView splash = (ImageView)findViewById(R.id.splash);
        Resources res2 = getResources();
        Drawable mad = res2.getDrawable(R.drawable.umadbro);
        splash.setImageDrawable(mad);

        setContentView(R.layout.activity_main);
        final View contentView = findViewById(R.id.fullscreen_content);


        //This code sets up the google speech prompt
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());


        //SpeechRecognitionListener listener = new SpeechRecognitionListener();
        //mSpeechRecognizer.setRecognitionListener(listener);

        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {

                            if (mControlsHeight == 0) {
                                //mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }

                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(0);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    public void promptSpeechInput(){

        i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");


        try {
            startActivityForResult(i,100);

        }
        catch(ActivityNotFoundException e){
            Toast.makeText(MainActivity.this, "Sorry, no", Toast.LENGTH_SHORT).show();
        }
    }


    public void onActivityResult(int request_code, int result_code, Intent i){
        super.onActivityResult(request_code,result_code,i);
        switch(request_code){
            case 100: if(result_code==RESULT_OK&&i !=null){
                ArrayList<String> result = i.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                //resultText.setText(result.get(0));

                resultText = result.get(0);
                TextView text = (TextView) findViewById(R.id.fullscreen_content);
                text.setText(resultText);

                MyThread thread = new MyThread(resultText);
                thread.start();
                try {
                    thread.join();
                }catch(InterruptedException j){
                    j.printStackTrace();
                }

                String toneSen = thread.getTheTone().toString();
                String[] split = toneSen.split("\\s");
                float[] numberArray = new float[5];
                String[] emotions = {"Anger","Disgust","Fear", "Joy", "Sadness"};
                int count2=0;
                for(int i2=0;i2<split.length;i2++){
                    if(count2 == 5){
                        break;
                    }
                    split[i2].trim();
                    if(split[i2].length()!=0&&split[i2].charAt(0)=='0'){
                        numberArray[count2++] =  Float.valueOf(split[i2]);
                    }
                }
                float max =numberArray[0];
                int index =0;
                for(int i2=0;i2<numberArray.length;i2++){
                    if(max<=numberArray[i2]){
                        max = numberArray[i2];
                        index =i2;
                    }

                }
                setEmotion(emotions[index]);
            }
                break;
        }
    }

    private void setEmotion(String emotion){
        ImageView emoji = (ImageView)findViewById(R.id.emoji);
        Resources res = getResources();

        switch(emotion){
            case "Joy":
                Drawable happy = res.getDrawable(R.drawable.happy);
                emoji.setImageDrawable(happy);
                break;

            case "Sadness":
                Drawable sad = res.getDrawable(R.drawable.sad);
                emoji.setImageDrawable(sad);
                break;

            case "Anger":
                Drawable angry = res.getDrawable(R.drawable.angry);
                emoji.setImageDrawable(angry);
                break;

            case "Fear":
                Drawable fear = res.getDrawable(R.drawable.fear);
                emoji.setImageDrawable(fear);
                break;
            case "Disgust":
                Drawable disgusted = res.getDrawable(R.drawable.disgust);
                emoji.setImageDrawable(disgusted);
                break;
            case "Neutral":
                Drawable neutral = res.getDrawable(R.drawable.neutral);
                emoji.setImageDrawable(neutral);
                break;
        }
    }


    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    
    //The network operation must be rum on a seperate thread
    public static class MyThread extends Thread {
        String text;
        ToneAnalysis tone;
        public MyThread(String text){
            this.text = text;

        }

        public void run(){
            ToneAnalyzer service = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_05_19);
            service.setUsernameAndPassword("1079029d-f17e-4bd0-800d-a7340ead306b", "wrM841ZlRaHA");

            try {
                tone = service.getTone(text, null).execute();
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        public ToneAnalysis getTheTone(){
            return tone;
        }
    }

}
