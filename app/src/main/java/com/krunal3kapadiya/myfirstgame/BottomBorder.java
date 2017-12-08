package com.krunal3kapadiya.myfirstgame;

import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * Created by Krunal on 11/11/2015.
 */
public class BottomBorder extends GameObject {
    Bitmap image;
    public BottomBorder(Bitmap res, int x, int y){
        height=200;
        width=20;

        this.x=x;
        this.y=y;
        dx=GamePanel.MOVESPEED;
        image= Bitmap.createBitmap(res,0,0,width,height);
}
    public void update(){
        x+=dx;
    }
    public void draw(Canvas canvas){
        canvas.drawBitmap(image,x,y,null);
    }
}
