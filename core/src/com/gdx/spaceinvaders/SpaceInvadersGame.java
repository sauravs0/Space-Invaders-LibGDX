package com.gdx.spaceinvaders;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.Random;

public class SpaceInvadersGame extends Game {

	SpriteBatch batch;
	BitmapFont font;
	private Music backgroundMusic;

	public static Random random = new Random();

	@Override
	public void create() {
		batch = new SpriteBatch();
		font = new BitmapFont();

		backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background_music.wav"));
		backgroundMusic.setLooping(true);
		backgroundMusic.play();

		this.setScreen(new MainMenuScreen(this));
	}

	@Override
	public void dispose() {
		batch.dispose();
		font.dispose();
	}

	@Override
	public void render() {
		super.render();
	}

}
