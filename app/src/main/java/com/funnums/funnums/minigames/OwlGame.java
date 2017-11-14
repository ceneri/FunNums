package com.funnums.funnums.minigames;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.funnums.funnums.classes.DraggableTile;
import com.funnums.funnums.classes.ExpressionEvaluator;
import com.funnums.funnums.uihelpers.GameFinishedMenu;
import com.funnums.funnums.uihelpers.UIButton;

import java.util.ArrayList;

public class OwlGame extends MiniGame {
    public String TAG = "Owl Game"; //for debugging

    /**
     * Private TitlePlaceHolder class holds the coordinates for a tile to be placed
     * and a reference to the tile that holds that position
     * */
    class TilePlaceHolder{
        float x;
        float y;
        DraggableTile t;

        TilePlaceHolder(float x, float y){
            this.x = x;
            this.y = y;
            t = null;
        }

        boolean isOccupied(){
            return (t != null);
        }

        DraggableTile getTile(){
            return t;
        }

        void setTile(DraggableTile t){
            this.t = t;
        }
    }

    final int TILE_LIMIT = 10;
    final int EXPR_LIMIT = 7;

    //Ratios based on screen size
    double TILE_LENGTH_RATIO = .10;     /*10% of the screen width*/
    double T_BUFFER_RATIO = .20;        /*20% of the screen length*/
    double E_BUFFER_RATIO = .15;        /*15% of the screen length*/

    //Dimensions of the screen
    private int screenX;
    private int screenY;

    //This is the amount of space at the top of the screen used for the tiles
    private float tileBuffer;
    private float exprBuffer;

    //Tile coordinates
    private ArrayList<TilePlaceHolder> tileSpaces = new ArrayList<>();
    //Expression coordinates
    private ArrayList<TilePlaceHolder> exprSpaces = new ArrayList<>();

    // List of all the touchable tiles on screen
    private ArrayList<DraggableTile> tileList;

    // Used to hold touch events so that drawing thread and onTouch thread don't result in concurrent access
    // not likely that these threads would interact, but if they do the game will crash!! which is why
    //we keep events in a separate list to be processed in the game loop
    private ArrayList<MotionEvent> events = new ArrayList<>();

    //TODO initialize Derek's target generator
    // The target generator
    // ExpressionGenerator expGenerator = new ExpressionGenerator();
    //For now we use dummy espression
    String [] dummy = {"1", "+", "2", "*", "3", "4", "-", "10", "+", "8"};

    // The Target evaluator
    ExpressionEvaluator evaluator;

    // Target player is trying to sum to
    private int target;

    //Optimal tile length/width radius
    private float tLength;

    //Counter of tiles
    int numberOfTiles;
    int numberOfExprSpaces;

    //Counter or tile spaces in use
    int numberOfTileSpacesUsed;
    int numberOfExprSpacesUsed;

    //game over menu
    private GameFinishedMenu gameFinishedMenu;

    //Initializer
    public void init() {

        //Game only finished when owl has died :P
        isFinished = false;

        //Initialize ArrayList of Tiles
        tileList = new ArrayList<>();

        //TODO Initialize Derek's generator
        //Initialize Expression generator Object
        //expGenerator = new ExpressionGenerator();

        //Initialize Expression Evaluator Object
        evaluator = new ExpressionEvaluator();

        //TODO get a target from the target generator
        //target = targetGen.nextTarget();
        //!!For now refer use dummy
        target = 10;

        //TODO set values according to the target generated
        numberOfTiles = TILE_LIMIT;
        numberOfExprSpaces = EXPR_LIMIT;

        //No tiles are present currently
        numberOfExprSpacesUsed = 0;
        numberOfTileSpacesUsed = 0;

        //Get x and Y values of the Screen
        screenX = com.funnums.funnums.maingame.GameActivity.screenX;
        screenY = com.funnums.funnums.maingame.GameActivity.screenY;

        //Set appropriate sizes based on screen
        tLength = (float) (screenX * TILE_LENGTH_RATIO);
        tileBuffer = (float) (screenY * T_BUFFER_RATIO);
        exprBuffer = (float) (screenY * E_BUFFER_RATIO);

        //Generate tile coordinates
        generateTileSpaceHolders();
        generateExprSpaceHolders();

        //Generate tiles
        generateTiles();

        /**Even tough pause button is not being used it has to be declared,
         * because minigame class forces you to have one :P
         */
        int offset = 100;
        Bitmap pauseImgDown = com.funnums.funnums.maingame.GameActivity.gameView.loadBitmap("pause_down.png", true);
        Bitmap pauseImg = com.funnums.funnums.maingame.GameActivity.gameView.loadBitmap("pause.png", true);
        pauseButton = new UIButton(screenX *3/4, 0, screenX, offset, pauseImg, pauseImgDown);

        Log.d(TAG, "init pauseButton: " + pauseButton);

        Bitmap resumeDown = com.funnums.funnums.maingame.GameView.loadBitmap("button_resume_down.png", true);
        Bitmap resume = com.funnums.funnums.maingame.GameView.loadBitmap("button_resume.png", true);
        UIButton resumeButton = new UIButton(0,0,0,0, resume, resumeDown);

        Bitmap menuDown = com.funnums.funnums.maingame.GameView.loadBitmap("button_quit_down.png", true);
        Bitmap menu = com.funnums.funnums.maingame.GameView.loadBitmap("button_quit.png", true);
        UIButton menuButton = new UIButton(0,0,0,0, menu, menuDown);

    }

    //Update method to be called by game loop
    public void update(long delta) {
        if (isPaused)
            return;

        processEvents();
    }

    //Draw method
    public void draw(SurfaceHolder ourHolder, Canvas canvas, Paint paint) {

        if (ourHolder.getSurface().isValid()) {
            //First we lock the area of memory we will be drawing to
            canvas = ourHolder.lockCanvas();

            // Rub out the last frame
            canvas.drawColor(Color.argb(255, 0, 0, 0));

            //draw tile buffer
            paint.setColor(Color.argb(255, 100, 150, 155));
            canvas.drawRect( (float)0, (float)(screenY-tileBuffer - exprBuffer), (float)screenX,
                    (float)screenY - exprBuffer, paint);

            //draw expr buffer
            paint.setColor(Color.argb(255, 150, 150, 155));
            canvas.drawRect( (float)0, (float)(screenY - exprBuffer), (float)screenX,
                    (float)screenY, paint);

            //Draw all the tiles
            for(DraggableTile num : tileList)
                num.draw(canvas, paint);

            /*
            if(pauseButton != null)
                pauseButton.render(canvas, paint);

            //draw pause menu, if paused
            if(isPaused)
                com.funnums.funnums.maingame.GameActivity.gameView.pauseScreen.draw(canvas, paint);
            //game finished stuff
            if(isFinished)
                com.funnums.funnums.maingame.GameActivity.gameView.gameFinishedMenu.draw(canvas, paint);
            */

            ourHolder.unlockCanvasAndPost(canvas);
        }


    }

    //Process the touch events
    private void processEvents() {

        for(MotionEvent e : events) {
            float  x = e.getX();
            float  y = e.getY();

            checkTouchedTile(x, y);
        }

        events.clear();

    }

    //Touch handler
    public synchronized boolean onTouch(MotionEvent e) {
        //add touch event to eventsQueue rather than processing it immediately. This is because
        //onTouchEvent is run in a separate thread by Android and if we touch and delete a number
        //in this touch UI thread while our game thread is accessing that same number, the game crashes
        //because two threads are accessing same memory being removed. We could do mutex but this setup
        //is pretty standard I believe.
        events.add(e);
        Log.d(TAG, "Touch event added");
        return true;
    }

    //TODO
    private void makeNewTarget() {}

    //TODO
    private void resetGame() { }

    // Generates TileSpaceHolders to be used by the tiles, initially no actual tiles
    // are being held inside the place holders
    private void generateTileSpaceHolders(){
        double SPACING_TOP_PERCENTAGE = .15;
        double SPACING_LEFT_PERCENTAGE = .05;
        double SPACING_MIDDLE_PERCENTAGE = .45;
        double SPACING_BETWEEN_PERCENTAGE = .1;


        float x, y;
        TilePlaceHolder space;

        //Y value starts at the top of the tileBuffer + 15% of the overall length of tileBuffer
        y = screenY - tileBuffer - exprBuffer + (int)(SPACING_TOP_PERCENTAGE * tileBuffer);
        //X leaves 5% spacing
        x = (int) (SPACING_LEFT_PERCENTAGE * screenX);

        space = new TilePlaceHolder (x, y);
        tileSpaces.add(space);

        for(int i = 1; i < 5; i++){

            x += (int) (SPACING_BETWEEN_PERCENTAGE * screenX) + tLength;

            space = new TilePlaceHolder (x, y);
            tileSpaces.add(space);
        }

        //Create second row

        //Y now adds 30% for the tile space and 15% for extra space
        y +=  (int)(SPACING_MIDDLE_PERCENTAGE * tileBuffer);
        //Reset X
        x = (int) (SPACING_LEFT_PERCENTAGE * screenX);

        space = new TilePlaceHolder (x, y);
        tileSpaces.add(space);

        for(int i = 6; i < 10; i++){
            x += (int) (SPACING_BETWEEN_PERCENTAGE * screenX) + tLength;

            space = new TilePlaceHolder (x, y);
            tileSpaces.add(space);
        }
    }

    // Generates TileSpaceHolders to be used by the tiles in an expression
    private void generateExprSpaceHolders(){
        double SPACING_TOP_PERCENTAGE = .25;
        double SPACING_LEFT_PERCENTAGE = .05;

        float x, y;
        TilePlaceHolder space;

        //Y starts at top of exprBuffer + 20% spacing
        y = screenY - exprBuffer + (int)(SPACING_TOP_PERCENTAGE * exprBuffer);
        x = (int) (SPACING_LEFT_PERCENTAGE * screenX);

        space = new TilePlaceHolder (x, y);
        exprSpaces.add(space);

        for(int i = 1; i < 10; i++){

            x += tLength;

            space = new TilePlaceHolder (x, y);
            exprSpaces.add(space);
        }


    }

    // Generates a draggable tiles on screen
    private void generateTiles() {

        float x, y;
        String value;
        TilePlaceHolder space;
        DraggableTile til;

        for (int i = 0; i < numberOfTiles; i++){
            space = tileSpaces.get(i);
            x = space.x;
            y = space.y;

            //TODO change from dummy to actual new expression
            value = dummy[i];

            til = new DraggableTile (x, y, tLength, value);
            tileList.add(til);

            space.setTile(til);
        }

        //TODO make sure is based on generator
        numberOfTileSpacesUsed = numberOfTiles;

    }

    //Check if there is a tile in the touch coordinates, and if so,
    //move tile to corresponding space
    private void checkTouchedTile(float x, float y) {
        //TODO find out the source of the mysterious power that this number holds
        int HOLY_MAGIC_NUMBER = 60;

        boolean touchInXRange, touchInYRange;

        for (DraggableTile t : tileList) {
            //TODO fix touch sensitivity
            //Boolean check of touch
            touchInXRange = ( x >= t.getX() && x <= (t.getX() + tLength) );
            touchInYRange = ( y >= (t.getY()+HOLY_MAGIC_NUMBER) && y <= (t.getY() + tLength)+HOLY_MAGIC_NUMBER);

            // If there is a hit
            if (touchInXRange && touchInYRange) {

                    if (t.isUsed()){
                        moveToTiles(t);
                    } else {
                        moveToExpr(t);
                    }
                    break;
            }
        }

    }
    
    //If there is a slot available in the expression
    // 1) Free your current spot
    // 2) Find the next open available space in the expression
    private void moveToExpr(DraggableTile tile) {
        float x, y;
        int index = 0;

        //If there is space in the expression
        if (numberOfExprSpacesUsed < numberOfExprSpaces){


            //Free your spot
            for (TilePlaceHolder p : tileSpaces) {

                if (p.getTile() == tile) {
                    p.setTile(null);
                    break;
                }

            }

            //Find an open spot in the expression
            for (TilePlaceHolder p : exprSpaces) {

                if (p.getTile() == null ){
                    x = p.x;
                    y = p.y;
                    tile.setXY(x, y);

                    tile.setUsed(true);
                    p.setTile(tile);

                    //Insert token to evaluate
                    insertTokenToEval(tile.getValue(), index);

                    break;
                }

                index++;
            }

            //Update values accordingly
            numberOfExprSpacesUsed++;
            numberOfTileSpacesUsed--;
        }

    }


    // 1) Free your current spot in the expression
    // 2) Find the next open available space in the overall tile space
    private void moveToTiles(DraggableTile tile){
        float x, y;
        int index = 0;

        //Free your spot in the expression
        for (TilePlaceHolder p : exprSpaces) {

            if (p.getTile() == tile) {
                p.setTile(null);

                //Insert token in evaluator
                deleteTokenToEval(index);
                break;
            }

            index++;

        }

        //Find an open spot in the overall tile space
        for (TilePlaceHolder p : tileSpaces) {

            if (p.getTile() == null ){
                x = p.x;
                y = p.y;
                tile.setXY(x, y);

                tile.setUsed(false);
                p.setTile(tile);

                break;
            }

        }

        //Update values accordingly
        numberOfExprSpacesUsed--;
        numberOfTileSpacesUsed++;

    }

    //Inserts a new token to be evaluated by ExpressionEvaluator object
    public void insertTokenToEval(String t, int index){
        evaluator.slots.insert(t, index);
    }

    //Delete a token from the ExpressionEvaluator object
    public void deleteTokenToEval(int index){
        evaluator.slots.delete(index);
    }
}
