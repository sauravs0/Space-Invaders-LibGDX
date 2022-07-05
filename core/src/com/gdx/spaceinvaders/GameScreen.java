package com.gdx.spaceinvaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Locale;

class GameScreen implements Screen {
    SpaceInvadersGame game;

    // Screen
    private Camera camera;
    private Viewport viewport;

    //Graphics
    private SpriteBatch batch;
    private TextureAtlas textureAtlas;
    private Texture explosionTexture;

    private TextureRegion[] backgrounds;
    private float backgroundHeight;

    private TextureRegion playerShipTextureRegion, playerShieldTextureRegion,
            enemyShipTextureRegion, enemyShieldTextureRegion,
            playerLaserTextureRegion, enemyLaserTextureRegion;

    //Timing
    private float[] backgroundOffsets = {0, 0, 0, 0};
    private float backgroundMaxScrollingSpeed;
    private float timeBetweenEnemySpawns = 1f;
    private float enemySpawnTimer = 0;

    //world parameters
    private final float WORLD_WIDTH = 72;
    private final float WORLD_HEIGHT = 128;
    private final float TOUCH_MOVEMENT_THRESHOLD = 0.5f;

    //Game Objects
    private PlayerShip playerShip;
    private LinkedList<EnemyShip> enemyShipList;
    private LinkedList<Laser> playerLaserList;
    private LinkedList<Laser> enemyLaserList;
    private LinkedList<Explosion> explosionList;

    private int score = 0;

    //Heads-Up Display
    BitmapFont font;
    float hudVerticalMargin, hudLeftX, hudRightX, hudCentreX, hudRow1Y, hudRow2Y, hudSectionWidth;

    //Sound Effects
    private Sound collisionSound;

    FileHandle file;
    private int HighScore;

    GameScreen(SpaceInvadersGame game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new StretchViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);

        //Set up the texture atlas
        textureAtlas = new TextureAtlas("images.atlas");

        backgrounds = new TextureRegion[4];
        backgrounds[0] = textureAtlas.findRegion("Starscape00");
        backgrounds[1] = textureAtlas.findRegion("Starscape01");
        backgrounds[2] = textureAtlas.findRegion("Starscape02");
        backgrounds[3] = textureAtlas.findRegion("Starscape03");

        backgroundHeight = WORLD_HEIGHT * 2;
        backgroundMaxScrollingSpeed = (float)(WORLD_HEIGHT) / 4;

        //Initialize Texture regions
        playerShipTextureRegion = textureAtlas.findRegion("playerShip2_blue");
        enemyShipTextureRegion = textureAtlas.findRegion("enemyRed3");
        playerShieldTextureRegion = textureAtlas.findRegion("shield2");
        enemyShieldTextureRegion = textureAtlas.findRegion("shield1");
        enemyShieldTextureRegion.flip(false, true);

        playerLaserTextureRegion = textureAtlas.findRegion("laserBlue03");
        enemyLaserTextureRegion = textureAtlas.findRegion("laserRed03");

        explosionTexture  = new Texture("explosion.png");

        collisionSound = Gdx.audio.newSound(Gdx.files.internal("collision_sound.wav"));

        file = Gdx.files.local("HighScore.txt");

        //Set up Game Objects
        playerShip = new PlayerShip(WORLD_WIDTH / 2, WORLD_HEIGHT / 4,
                10, 10,
                48, 3,
                0.4f, 4,
                45, 0.5f,
                playerShipTextureRegion, playerShieldTextureRegion, playerLaserTextureRegion);

        enemyShipList = new LinkedList<>();

        playerLaserList = new LinkedList<>();
        enemyLaserList = new LinkedList<>();

        explosionList = new LinkedList<>();

        batch = new SpriteBatch();

        prepareHUD();

    }

    private void prepareHUD(){
        //Create a BitmapFont from our font file
        FreeTypeFontGenerator fontGenerator = new FreeTypeFontGenerator(
                Gdx.files.internal("EdgeOfTheGalaxyRegular-OVEa6.otf"));
        FreeTypeFontGenerator.FreeTypeFontParameter fontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        fontParameter.size = 72;
        fontParameter.borderWidth = 3.6f;
        fontParameter.color = new Color(1, 1, 1, 0.3f);
        fontParameter.borderColor = new Color(0, 0, 0, 0.3f);

        font = fontGenerator.generateFont(fontParameter);

        //Scale the font to fit world
        font.getData().setScale(0.08f);

        //Calculate hud margins, etc.
        hudVerticalMargin = font.getCapHeight() / 2;
        hudLeftX = hudVerticalMargin;
        hudRightX = WORLD_WIDTH * 2 / 3 - hudLeftX;
        hudCentreX = WORLD_WIDTH / 3;
        hudRow1Y = WORLD_HEIGHT - hudVerticalMargin;
        hudRow2Y = hudRow1Y - hudVerticalMargin - font.getCapHeight();
        hudSectionWidth = WORLD_WIDTH / 3;

    }

    @Override
    public void render(float deltaTime) {
        batch.begin();

        //Scrolling background
        renderBackground(deltaTime);

        detectInput(deltaTime);
        playerShip.update(deltaTime);

        spawnEnemyShips(deltaTime);

        ListIterator<EnemyShip> enemyShipListIterator = enemyShipList.listIterator();
        while(enemyShipListIterator.hasNext()){
            EnemyShip enemyShip = enemyShipListIterator.next();
            moveEnemy(enemyShip, deltaTime);
            enemyShip.update(deltaTime);
            enemyShip.draw(batch);
        }

        //Player Ship
        playerShip.draw(batch);

        //Lasers
        renderLasers(deltaTime);

        //Detect collisions between lasers and ships
        detectCollisions();

        //Explosions
        updateAndRenderExplosions(deltaTime);

        //HUD Rendering
        updateAndRenderHUD();

        batch.end();
    }

    private void updateAndRenderHUD(){
        //Render top row labels
        font.draw(batch, "Score", hudLeftX, hudRow1Y, hudSectionWidth, Align.left, false);
        font.draw(batch, "Shield", hudCentreX, hudRow1Y, hudSectionWidth, Align.center, false);
        font.draw(batch, "Lives", hudRightX, hudRow1Y, hudSectionWidth, Align.right, false);

        //Render second row values
        font.draw(batch, String.format(Locale.getDefault(), "%06d", score),
                hudLeftX, hudRow2Y, hudSectionWidth, Align.left, false);
        font.draw(batch, String.format(Locale.getDefault(), "%02d", playerShip.shield),
                hudCentreX, hudRow2Y, hudSectionWidth, Align.center, false);
        font.draw(batch, String.format(Locale.getDefault(), "%02d", playerShip.lives),
                hudRightX, hudRow2Y, hudSectionWidth, Align.right, false);

    }

    private void moveEnemy(EnemyShip enemyShip, float deltaTime){
        float leftLimit, rightLimit, upLimit, downLimit;
        leftLimit = -enemyShip.boundingBox.x;
        downLimit = (float)WORLD_HEIGHT / 2 - enemyShip.boundingBox.y;
        rightLimit = WORLD_WIDTH - enemyShip.boundingBox.x - enemyShip.boundingBox.width;
        upLimit = WORLD_HEIGHT - enemyShip.boundingBox.y - enemyShip.boundingBox.height;

        float xMove = enemyShip.getDirectionVector().x * enemyShip.movementSpeed * deltaTime;
        float yMove = enemyShip.getDirectionVector().y * enemyShip.movementSpeed * deltaTime;

        if(xMove > 0) xMove = Math.min(xMove, rightLimit);
        else xMove = Math.max(xMove, leftLimit);

        if(yMove > 0) yMove = Math.min(yMove, upLimit);
        else yMove = Math.max(yMove, downLimit);

        enemyShip.translate(xMove, yMove);
    }

    private void spawnEnemyShips(float deltaTime){
        enemySpawnTimer += deltaTime;
        if(enemySpawnTimer > timeBetweenEnemySpawns) {
            enemyShipList.add(new EnemyShip(
                    SpaceInvadersGame.random.nextFloat() * (WORLD_WIDTH - 10) + 5, WORLD_HEIGHT - 5,
                    10, 10,
                    48, 1,
                    0.3f, 5,
                    50, 0.8f,
                    enemyShipTextureRegion, enemyShieldTextureRegion, enemyLaserTextureRegion));
            enemySpawnTimer -= timeBetweenEnemySpawns;
        }

    }

    private void detectInput(float deltaTime){
        //Keyboard input
        float leftLimit, rightLimit, upLimit, downLimit;
        leftLimit = -playerShip.boundingBox.x;
        downLimit = -playerShip.boundingBox.y;
        rightLimit = WORLD_WIDTH - playerShip.boundingBox.x - playerShip.boundingBox.width;
        upLimit = (float)WORLD_HEIGHT / 2 - playerShip.boundingBox.y - playerShip.boundingBox.height;

        if(Gdx.input.isKeyPressed(Input.Keys.RIGHT) && rightLimit > 0) {
            playerShip.translate(Math.min(playerShip.movementSpeed * deltaTime, rightLimit), 0f);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.UP) && upLimit > 0) {
            playerShip.translate(0f, Math.min(playerShip.movementSpeed * deltaTime, upLimit));
        }
        if(Gdx.input.isKeyPressed(Input.Keys.LEFT) && leftLimit < 0) {
            playerShip.translate(Math.max(-playerShip.movementSpeed * deltaTime, leftLimit), 0f);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.DOWN) && downLimit < 0) {
            playerShip.translate(0f, Math.max(-playerShip.movementSpeed * deltaTime, downLimit));
        }

        //Touch(mouse) input
        if(Gdx.input.isTouched()) {
            float xTouchPixels = Gdx.input.getX();
            float yTouchPixels = Gdx.input.getY();

            Vector2 touchPoint = new Vector2(xTouchPixels, yTouchPixels);
            touchPoint = viewport.unproject(touchPoint);

            Vector2 playerShipCentre = new Vector2(
                    playerShip.boundingBox.x + playerShip.boundingBox.width / 2,
                    playerShip.boundingBox.y + playerShip.boundingBox.height / 2);

            float touchDistance = touchPoint.dst(playerShipCentre);

            if(touchDistance > TOUCH_MOVEMENT_THRESHOLD){
                float xTouchDifference = touchPoint.x - playerShipCentre.x;
                float yTouchDifference = touchPoint.y - playerShipCentre.y;

                float xMove = xTouchDifference / touchDistance * playerShip.movementSpeed * deltaTime;
                float yMove = yTouchDifference / touchDistance * playerShip.movementSpeed * deltaTime;

                if(xMove > 0) xMove = Math.min(xMove, rightLimit);
                else xMove = Math.max(xMove, leftLimit);

                if(yMove > 0) yMove = Math.min(yMove, upLimit);
                else yMove = Math.max(yMove, downLimit);

                playerShip.translate(xMove, yMove);
            }
        }
    }

    private void detectCollisions(){

        ListIterator<Laser> laserListIterator = playerLaserList.listIterator();
        while(laserListIterator.hasNext()) {
            Laser laser = laserListIterator.next();
            ListIterator<EnemyShip> enemyShipListIterator = enemyShipList.listIterator();
            while(enemyShipListIterator.hasNext()){
                EnemyShip enemyShip = enemyShipListIterator.next();
                if(enemyShip.intersects(laser.boundingBox)){
                    if(enemyShip.hitAndCheckDestroyed(laser)){
                        enemyShipListIterator.remove();
                        explosionList.add(
                                new Explosion(explosionTexture,
                                        new Rectangle(enemyShip.boundingBox),
                                        0.7f));
                        score += 100;
                        collisionSound.play();
                    }
                    laserListIterator.remove();
                    break;
                }
            }
        }

        laserListIterator = enemyLaserList.listIterator();
        while(laserListIterator.hasNext()) {
            Laser laser = laserListIterator.next();
            if(playerShip.intersects(laser.boundingBox)){
                if(playerShip.hitAndCheckDestroyed(laser)){
                    explosionList.add(
                            new Explosion(explosionTexture,
                                    new Rectangle(playerShip.boundingBox),
                                    1.6f));
                    playerShip.shield = 3;
                    playerShip.lives--;
                    collisionSound.play();
                    if(playerShip.lives <= 0){
                        try {
                            String text = file.readString();
                            HighScore = Integer.parseInt(text);
                            if (score > HighScore) {
                                text = Integer.toString(score);
                                file.writeString(text, false);
                            }
                        }catch (GdxRuntimeException e){
                            e.printStackTrace();
                        }
                        game.setScreen(new GameOverScreen(game));
                        dispose();
                    }
                }
                laserListIterator.remove();
            }
        }
    }

    private void updateAndRenderExplosions(float deltaTime){
        ListIterator<Explosion> explosionListIterator = explosionList.listIterator();
        while(explosionListIterator.hasNext()){
            Explosion explosion = explosionListIterator.next();
            explosion.update(deltaTime);
            if(explosion.isFinished()){
                explosionListIterator.remove();
            }
            else{
                explosion.draw(batch);
            }
        }
    }

    private void renderLasers(float deltaTime){

        //Create new Lasers
        //Player lasers
        if(playerShip.canFireLaser()){
            Laser[] lasers = playerShip.fireLasers();
            for(Laser laser: lasers){
                playerLaserList.add(laser);
            }
        }

        //Enemy lasers
        ListIterator<EnemyShip> enemyShipListIterator = enemyShipList.listIterator();
        while(enemyShipListIterator.hasNext()){
            EnemyShip enemyShip = enemyShipListIterator.next();
            if(enemyShip.canFireLaser()){
                Laser[] lasers = enemyShip.fireLasers();
                enemyLaserList.addAll(Arrays.asList(lasers));
            }
        }

        //Draw Lasers
        //Remove old Lasers
        ListIterator<Laser> iterator = playerLaserList.listIterator();
        while(iterator.hasNext()){
            Laser laser = iterator.next();
            laser.draw(batch);
            laser.boundingBox.y += laser.movementSpeed * deltaTime;
            if(laser.boundingBox.y > WORLD_HEIGHT){
                iterator.remove();
            }
        }

        iterator = enemyLaserList.listIterator();
        while(iterator.hasNext()){
            Laser laser = iterator.next();
            laser.draw(batch);
            laser.boundingBox.y -= laser.movementSpeed * deltaTime;
            if(laser.boundingBox.y + laser.boundingBox.height < 0){
                iterator.remove();
            }
        }
    }

    private void renderBackground(float deltaTime){

        backgroundOffsets[0] += deltaTime * backgroundMaxScrollingSpeed / 8;
        backgroundOffsets[1] += deltaTime * backgroundMaxScrollingSpeed / 4;
        backgroundOffsets[2] += deltaTime * backgroundMaxScrollingSpeed / 2;
        backgroundOffsets[3] += deltaTime * backgroundMaxScrollingSpeed;

        for(int layer = 0; layer < backgroundOffsets.length; layer++){
            if(backgroundOffsets[layer] > WORLD_HEIGHT){
                backgroundOffsets[layer] = 0;
            }
            batch.draw(backgrounds[layer], 0, -backgroundOffsets[layer], WORLD_WIDTH, backgroundHeight);

        }

    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        batch.setProjectionMatrix(camera.combined);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void show() {

    }

    @Override
    public void dispose() {

    }
}
