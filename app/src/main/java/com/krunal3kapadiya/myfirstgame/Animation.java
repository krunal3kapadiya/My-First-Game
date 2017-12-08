package com.krunal3kapadiya.myfirstgame;

import android.graphics.Bitmap;

/**
 * Created by Krunal on 11/11/2015.
 */
public class Animation {
    private Bitmap[] frames;
    private int currentFrames;
    private long startTimes;
    private long delay;
    private boolean playedOnce;
    public void setFrames(Bitmap[] frames){
        this.frames=frames;
        currentFrames=0;
        startTimes= System.nanoTime();
    }
    public void setDelay(long d){delay=d;}
    public void setFrame(int i){currentFrames=i;}
    public void update(){
        long elapsed=(System.nanoTime()-startTimes)/1000000;
        if(elapsed>delay){
            currentFrames++;
            startTimes= System.nanoTime();
        }
        if (currentFrames==frames.length){
            currentFrames=0;
            playedOnce=true;
        }
    }
    public Bitmap getImage(){
        return frames[currentFrames];
    }
    public int getFrame(){return currentFrames;}
    public boolean playedOnce(){return playedOnce;}
}
