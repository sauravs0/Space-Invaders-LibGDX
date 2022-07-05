package com.gdx.spaceinvaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;

public class GameOverScreen implements Screen {
    SpaceInvadersGame game;
    OrthographicCamera camera;

    private int Score;
    FileHandle file;

    public GameOverScreen(SpaceInvadersGame game){
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 400);

        FreeTypeFontGenerator fontGenerator = new FreeTypeFontGenerator(
                Gdx.files.internal("EdgeOfTheGalaxyRegular-OVEa6.otf"));
        FreeTypeFontGenerator.FreeTypeFontParameter fontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        fontParameter.size = 72;
        fontParameter.borderWidth = 3.6f;
        fontParameter.color = new Color(1, 1, 1, 0.3f);
        fontParameter.borderColor = new Color(0, 0, 0, 0.3f);

        game.font = fontGenerator.generateFont(fontParameter);

        game.font.getData().setScale(0.7f);

        file = Gdx.files.local("HighScore.txt");

        try {
            String text = file.readString();
            Score = Integer.parseInt(text);
        }catch (GdxRuntimeException e){
            e.printStackTrace();
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0.2f, 1);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        game.font.draw(game.batch, "Game Over ", 250, 320);
        game.font.draw(game.batch, "High Score: " + Score, 200, 250);
        game.font.draw(game.batch, "Press R to Retry", 200, 180);
        game.font.draw(game.batch, "Press Q to Quit", 220, 110);
        game.batch.end();

        if(Gdx.input.isKeyPressed(Input.Keys.R)){
            game.setScreen(new GameScreen(game));
            dispose();
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.Q)){
            Gdx.app.exit();
        }
    }

    @Override
    public void show() {

    }

    @Override
    public void resize(int width, int height) {

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
    public void dispose() {

    }
}
