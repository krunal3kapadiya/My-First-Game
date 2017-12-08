package com.krunal3kapadiya.myfirstgame;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Krunal on 11/10/2015.
 */
public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {
    public static final int WIDTH=856;
    public static final int HEIGHT=480;
    public static final int MOVESPEED= -5;
    private long SmokeStartTime;
    private long missileStartTime;
    private MainThread thread;
    private Background bg;
    private Player player;
    private ArrayList<Smokepuff> smoke;
    private ArrayList<Missile> missiles;
    private ArrayList<TopBorder> topBorders;
    private ArrayList<BottomBorder> bottomBorders;
    private Random rand=new Random();
    private int maxBorderHeight;
    private int minBorderHeight;
    private boolean topDown=true;
    private boolean botDown=true;
    private boolean newGamecreated;

    private Explosion explosion;
    private long startReset;
    private boolean reset;
    private boolean disappear;
    private boolean started;
    private int best;

    //increase to slow down difficulty progression,decrease to slow up difficulty progression
    private int progressDenom=20;

    public GamePanel(Context context){
        super(context);
        //Add the callback to the surfaceviewholder to intercept the event
        getHolder().addCallback(this);
        setFocusable(true);
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){

    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        boolean retry=true;
        int counter=0;
        while (retry && counter<1000){
            try {
            thread.setRunning(false);
                counter++;
                thread.join();
                retry=false;
                thread=null;
            }catch (Exception e){e.printStackTrace();}

        }

    }
    @Override
    public void surfaceCreated(SurfaceHolder holder){
        bg=new Background(BitmapFactory.decodeResource(getResources(),R.drawable.grassbg1));
        player=new Player(BitmapFactory.decodeResource(getResources(),R.drawable.helicopter),65,25,3);
        smoke=new ArrayList<Smokepuff>();
        missiles=new ArrayList<Missile>();
        topBorders=new ArrayList<TopBorder>();
        bottomBorders=new ArrayList<BottomBorder>();
        missileStartTime= System.nanoTime();
        SmokeStartTime= System.nanoTime();
        thread=new MainThread(getHolder(),this);
        //we can safely start the game loop
        thread.setRunning(true);
        thread.start();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(event.getAction()== MotionEvent.ACTION_DOWN){
            if(!player.getPlaying() && newGamecreated && reset){
                player.setPlaying(true);
                player.setUp(true);
            }
            if(player.getPlaying()) {
                if(!started)started=true;
                reset=false;
                player.setUp(true);
            }
            return true;
        }
        if(event.getAction()== MotionEvent.ACTION_UP){
            player.setUp(false);
            return true;
        }
        return super.onTouchEvent(event);
    }
    public void update(){
        if(player.getPlaying()) {
            if(bottomBorders.isEmpty()){
                player.setPlaying(false);
                return;
            }
            if(topBorders.isEmpty()){
                player.setPlaying(false);
                return;
            }

            bg.update();
            player.update();
            //calculate the thresold of height the border can have based on score
            //max and min border heart are updated, and the border switched direction when either max or min is met
            maxBorderHeight=30+ player.getScore()/progressDenom;
            //cap max border height so that borders can only take up a total of 1/2 the screen
            if(maxBorderHeight>HEIGHT/4)maxBorderHeight=HEIGHT/4;
            minBorderHeight=5+player.getScore()/progressDenom;

            //check bottom border collision
            for(int i=0;i<bottomBorders.size();i++){
                if(collision(bottomBorders.get(i),player)){
                    player.setPlaying(false);
                }
            }
            //check top border collision
            for(int i=0;i<topBorders.size();i++){
                if(collision(topBorders.get(i),player)){
                    player.setPlaying(false);
                }
            }

            //update top border
            this.updateTopborder();
            //create bottom border
            this.updateBottomborder();
            //add missiles ontimer
            long missilesElapsed= (System.nanoTime()-missileStartTime)/1000000;
            if(missilesElapsed>(2000-player.getScore()/4)){
             //first missile always be on middle
                if(missiles.size()==0){
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(),R.drawable.
                            missile),WIDTH+10,HEIGHT/2,45,15,player.getScore(),13));
                }
                else {
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(),R.drawable.missile),
                            WIDTH+10,(int)(rand.nextDouble()*(HEIGHT-(maxBorderHeight*2)+maxBorderHeight)),45,15,player.getScore(),13));
                }
                //reset Timer
                missileStartTime= System.nanoTime();
            }
            //loop through Every Missile and check collision and check
            for(int i=0;i<missiles.size();i++){
                //update missiles
                missiles.get(i).update();
                if(collision(missiles.get(i),player)){
                    missiles.remove(i);
                    player.setPlaying(false);
                    break;
                }
                //remove missile if it is out of the screen
                if(missiles.get(i).getX()<-100){
                    missiles.remove(i);
                    break;
                }

            }

            //add smoke puff ontimer
            long elapsed=(System.nanoTime()-SmokeStartTime)/1000000;
            if(elapsed>120){
                smoke.add(new Smokepuff(player.getX(),player.getY()+10));
                SmokeStartTime= System.nanoTime();
            }
            for (int i=0;i<smoke.size();i++){
                smoke.get(i).update();
                if(smoke.get(i).getX()<-10){
                    smoke.remove(i);
                }
            }
        }
        else {
            player.resetDY();
            if(!reset){
                newGamecreated=false;
                startReset= System.nanoTime();
                reset=true;
                disappear=true;
                explosion=new Explosion(BitmapFactory.decodeResource(getResources(),R.drawable.explosion),player.getX(),player.getY()-30,100,100,25);
            }
            explosion.update();
            long resetElapsed=(System.nanoTime()-startReset)/1000000;
            if(resetElapsed<2500 && !newGamecreated){
                newGame();
            }
        }
    }

     public void updateBottomborder() {
         //every 40 points, insert randomly places bottom blocks that break pattern
         if(player.getScore()%40==0){
             bottomBorders.add(new BottomBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                     bottomBorders.get(bottomBorders.size()-1).getX()+20,(int)((rand.nextDouble()*(maxBorderHeight))+(HEIGHT-maxBorderHeight))));
         }
         //update bottom border
         for (int i=0;i<bottomBorders.size();i++){
             bottomBorders.get(i).update();
             //if border is moving off screen, remove it and add a corresponding new one
             if(bottomBorders.get(i).getX()<-20){
                 bottomBorders.remove(i);
                 //determine if border is moving down or up
                 if(bottomBorders.get(bottomBorders.size()-1).getY()<=HEIGHT-maxBorderHeight){
                     botDown=true;
                 }
                 if(bottomBorders.get(bottomBorders.size()-1).getY()>=HEIGHT-minBorderHeight){
                    botDown=false;
                 }
         }
            botDown=true;
         }
         if(botDown){
             bottomBorders.add(new BottomBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),bottomBorders.get(bottomBorders.size()-1).getX()+20,bottomBorders.get(bottomBorders.size()-1).getY()+1));
         }
         else {
             bottomBorders.add(new BottomBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick), bottomBorders.get(bottomBorders.size() - 1).getX() + 20, bottomBorders.get(bottomBorders.size() - 1).getY() - 1));
         }
    }

    public void updateTopborder() {
        //every 50 points, insert randomly places top blocks that break pattern
        if(player.getScore()%50==0){
            topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                    topBorders.get(topBorders.size()-1).getX()+20,0,(int)((rand.nextDouble()*(maxBorderHeight))+1)));
        }
        for(int i=0;i<topBorders.size();i++){
            topBorders.get(i).update();
            if(topBorders.get(i).getX()<-20){
                topBorders.remove(i);
                //remove element of arraylist, replace it by adding a new one
                //calculate topdown which determines the direction the border is moving (up or downs)
                if(topBorders.get(topBorders.size()-1).getHeight()>=maxBorderHeight){
                    topDown=false;
                }
                if(topBorders.get(topBorders.size()-1).getHeight()<=minBorderHeight){
                    topDown=true;
                }
                //new border added will have larger height
                if(topDown){
                    topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                            topBorders.get(topBorders.size()-1).getX()+20,0,topBorders.get(topBorders.size()-1).getHeight()+1));
                }
                //new Border added will have smaller height
                else {
                    topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                            topBorders.get(topBorders.size()-1).getX()+20,0,topBorders.get(topBorders.size()-1).getHeight()-1));
                }
            }
        }
    }

    public boolean collision(GameObject a, GameObject b){
        if(Rect.intersects(a.getRectangle(),b.getRectangle())){
                return true;
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas){
        final float scaleFactorX=getWidth()/(WIDTH*1.f);
        final float scaleFactorY=getHeight()/(HEIGHT*1.f);
        if(canvas!=null) {
            final int savedState=canvas.save();
            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);
            if(!disappear){
            player.draw(canvas);
            }
            //draw smokepuff
            for (Smokepuff sp : smoke){
                sp.draw(canvas);
            }
            //draw missiles
            for (Missile m: missiles){
                m.draw(canvas);
            }


            //draw top border
            for (TopBorder tb : topBorders){
                tb.draw(canvas);
            }
            //draw bottom border
            for (BottomBorder bb : bottomBorders){
                bb.draw(canvas);
            }
            //draw Exploison
            if(started){
                explosion.draw(canvas);
            }
            drawText(canvas);
            canvas.restoreToCount(savedState);
        }

    }

    public void drawText(Canvas canvas) {
        Paint paint=new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("DISTANCE: "+ (player.getScore()*3),10,HEIGHT-10,paint);
        canvas.drawText("BEST: "+best,WIDTH-215,HEIGHT-10,paint);

        if(!player.getPlaying()&&newGamecreated&&reset){
            Paint paint1=new Paint();
            paint1.setTextSize(40);
            paint1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("PRESS TO START", WIDTH / 2 - 50, HEIGHT / 2, paint1);
            paint1.setTextSize(20);
            canvas.drawText("PRESS AND HOLD TO GO UP",WIDTH/2-50,HEIGHT/2+20,paint1);
            canvas.drawText("RELEASE TO GO DOWN",WIDTH/2-50,HEIGHT/2+40,paint1);

        }

    }

    public void newGame(){
        disappear=false;
        bottomBorders.clear();
        topBorders.clear();
        missiles.clear();
        smoke.clear();

        minBorderHeight=5;
        maxBorderHeight=30;

        player.resetScore();
        player.setY(HEIGHT / 2);
        player.resetDY();

        if(player.getScore()>best){
            best=player.getScore();
        }

        //create initial border
        //initial top border
        for (int i=0;i*20<WIDTH+40;i++){
            //first top border create
            if(i==0){
                topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),i*20,0,10));
            }
            else {
                topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),i*20,0,topBorders.get(i-1).getHeight()+1));
            }
        }
        //initial bottom border
        for (int i=0;i*20<WIDTH+40;i++){
            //first border ever crated
            if(i==0){
                bottomBorders.add(new BottomBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),i*20,HEIGHT-minBorderHeight));
            }
            //adding borders until the initial screen is filled
            else {
                bottomBorders.add(new BottomBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),i*20,bottomBorders.get(i-1).getY()-1));
            }
        }
        newGamecreated=true;
    }
}
