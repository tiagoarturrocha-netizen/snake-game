package com.example.snakegame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final int COLS = 20;
    private static final int ROWS = 20;
    private static final long TICK_MS = 120;
    private static final int SAMPLE_RATE = 22050;

    private Thread thread;
    private volatile boolean running;
    private final Object lock = new Object();

    private final ArrayList<Point> snake = new ArrayList<>();
    private Point dir = new Point(1, 0);
    private Point pendingDir = new Point(1, 0);
    private Point food = new Point();
    private final Random random = new Random();

    private int score = 0;
    private int best = 0;
    private boolean gameOver = false;
    private boolean started = false;

    private int cellSize;
    private int offsetX;
    private int offsetY;

    private final Paint paintSnake = new Paint();
    private final Paint paintSnakeHead = new Paint();
    private final Paint paintBg = new Paint();
    private final Paint paintGrid = new Paint();
    private final Paint paintBorder = new Paint();
    private final Paint paintText = new Paint();
    private final Paint paintDim = new Paint();
    private final Paint paintApple = new Paint();
    private final Paint paintShine = new Paint();
    private final Paint paintStem = new Paint();
    private final Paint paintLeaf = new Paint();

    private Chiptune chiptune;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    private float touchStartX, touchStartY;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);

        paintSnake.setColor(Color.rgb(76, 175, 80));
        paintSnakeHead.setColor(Color.rgb(139, 195, 74));
        paintBg.setColor(Color.rgb(18, 18, 22));
        paintGrid.setColor(Color.rgb(30, 30, 36));
        paintBorder.setColor(Color.rgb(74, 74, 90));
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(6f);
        paintDim.setColor(Color.argb(180, 0, 0, 0));
        paintText.setColor(Color.WHITE);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintShine.setColor(Color.argb(140, 255, 255, 255));
        paintStem.setColor(Color.rgb(91, 58, 30));
        paintStem.setStrokeWidth(4f);
        paintStem.setStyle(Paint.Style.STROKE);
        paintStem.setStrokeCap(Paint.Cap.ROUND);
        paintLeaf.setColor(Color.rgb(76, 158, 47));

        chiptune = new Chiptune();

        tts = new TextToSpeech(context, status -> ttsReady = status == TextToSpeech.SUCCESS);
        tts.setLanguage(Locale.US);

        resetGame();
    }

    private void resetGame() {
        snake.clear();
        int midY = ROWS / 2;
        snake.add(new Point(5, midY));
        snake.add(new Point(4, midY));
        snake.add(new Point(3, midY));
        dir = new Point(1, 0);
        pendingDir = new Point(1, 0);
        score = 0;
        gameOver = false;
        started = false;
        spawnFood();
    }

    private void spawnFood() {
        Point p;
        do {
            p = new Point(random.nextInt(COLS), random.nextInt(ROWS));
        } while (snake.contains(p));
        food = p;
    }

    private void computeLayout(int width, int height) {
        cellSize = Math.min((width - 12) / COLS, (height - 12) / ROWS);
        offsetX = (width - cellSize * COLS) / 2;
        offsetY = (height - cellSize * ROWS) / 2;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        computeLayout(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        chiptune.stop();
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        long last = System.currentTimeMillis();
        while (running) {
            long now = System.currentTimeMillis();
            if (started && !gameOver && now - last >= TICK_MS) {
                last = now;
                synchronized (lock) {
                    update();
                }
            }
            draw();
            try {
                Thread.sleep(16);
            } catch (InterruptedException ignored) {
                return;
            }
        }
    }

    private void update() {
        dir = pendingDir;
        Point head = snake.get(0);
        int nx = head.x + dir.x;
        int ny = head.y + dir.y;

        if (nx < 0 || nx >= COLS || ny < 0 || ny >= ROWS) {
            endGame();
            return;
        }
        for (Point p : snake) {
            if (p.x == nx && p.y == ny) {
                endGame();
                return;
            }
        }

        snake.add(0, new Point(nx, ny));
        if (nx == food.x && ny == food.y) {
            score++;
            if (score % 5 == 0) saySixSeven();
            spawnFood();
        } else {
            snake.remove(snake.size() - 1);
        }
    }

    private void endGame() {
        gameOver = true;
        chiptune.stop();
        if (score > best) best = score;
    }

    private void saySixSeven() {
        if (ttsReady) {
            tts.speak("six seven", TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void draw() {
        SurfaceHolder holder = getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;
        try {
            synchronized (lock) {
                int w = canvas.getWidth();
                int h = canvas.getHeight();
                canvas.drawColor(paintBg.getColor());

                for (int x = 0; x < COLS; x++) {
                    for (int y = 0; y < ROWS; y++) {
                        if ((x + y) % 2 == 0) {
                            canvas.drawRect(offsetX + x * cellSize, offsetY + y * cellSize,
                                    offsetX + (x + 1) * cellSize, offsetY + (y + 1) * cellSize,
                                    paintGrid);
                        }
                    }
                }

                canvas.drawRect(offsetX - 9f, offsetY - 9f,
                        offsetX + COLS * cellSize + 9f, offsetY + ROWS * cellSize + 9f,
                        paintBorder);

                drawApple(canvas, offsetX + food.x * cellSize + cellSize / 2f,
                        offsetY + food.y * cellSize + cellSize / 2f, cellSize / 2.5f);

                for (int i = snake.size() - 1; i >= 0; i--) {
                    Point p = snake.get(i);
                    Paint paint = (i == 0) ? paintSnakeHead : paintSnake;
                    float pad = cellSize * 0.08f;
                    canvas.drawRoundRect(offsetX + p.x * cellSize + pad,
                            offsetY + p.y * cellSize + pad,
                            offsetX + (p.x + 1) * cellSize - pad,
                            offsetY + (p.y + 1) * cellSize - pad,
                            cellSize * 0.2f, cellSize * 0.2f, paint);
                }

                paintText.setTextSize(w / 18f);
                canvas.drawText(String.valueOf(score), w / 2f, offsetY * 0.5f, paintText);
                paintText.setTextSize(w / 40f);
                canvas.drawText("Recorde: " + best, w / 2f, offsetY * 0.5f + w / 22f, paintText);

                if (!started) {
                    canvas.drawRect(0, 0, w, h, paintDim);
                    paintText.setTextSize(w / 15f);
                    canvas.drawText("SNAKE", w / 2f, h / 2f - w / 20f, paintText);
                    paintText.setTextSize(w / 28f);
                    canvas.drawText("Deslize para mover · Toque para começar", w / 2f, h / 2f + w / 15f, paintText);
                } else if (gameOver) {
                    canvas.drawRect(0, 0, w, h, paintDim);
                    paintText.setTextSize(w / 18f);
                    canvas.drawText("Perdeu miserável!", w / 2f, h / 2f - w / 20f, paintText);
                    paintText.setTextSize(w / 28f);
                    canvas.drawText("Pontos: " + score, w / 2f, h / 2f + w / 30f, paintText);
                    canvas.drawText("Toque para jogar de novo", w / 2f, h / 2f + w / 12f, paintText);
                }
            }
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawApple(Canvas canvas, float cx, float cy, float r) {
        paintApple.setShader(new RadialGradient(cx - r * 0.35f, cy - r * 0.4f, r * 1.15f,
                new int[]{Color.rgb(255, 123, 107), Color.rgb(232, 56, 44),
                        Color.rgb(165, 30, 20), Color.rgb(110, 15, 8)},
                new float[]{0f, 0.35f, 0.8f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, paintApple);
        canvas.drawOval(cx - r * 0.55f, cy - r * 0.55f,
                cx - r * 0.1f, cy - r * 0.3f, paintShine);
        canvas.drawLine(cx, cy - r * 0.85f, cx + r * 0.05f, cy - r * 1.45f, paintStem);
        canvas.save();
        canvas.rotate(30f, cx, cy);
        canvas.drawOval(cx + r * 0.05f, cy - r * 1.3f, cx + r * 0.75f, cy - r * 0.95f, paintLeaf);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                if (!started || gameOver) {
                    if (gameOver) resetGame();
                    started = true;
                    chiptune.start();
                    return true;
                }
                float dx = event.getX() - touchStartX;
                float dy = event.getY() - touchStartY;
                if (Math.abs(dx) < 20 && Math.abs(dy) < 20) return true;
                Point nd;
                if (Math.abs(dx) > Math.abs(dy)) {
                    nd = new Point(dx > 0 ? 1 : -1, 0);
                } else {
                    nd = new Point(0, dy > 0 ? 1 : -1);
                }
                if (nd.x != -dir.x || nd.y != -dir.y) {
                    pendingDir = nd;
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private static class Chiptune implements Runnable {

        private static final float[][] MELODY = {
                {659.25f, 1}, {493.88f, .5f}, {523.25f, .5f}, {587.33f, 1}, {523.25f, .5f}, {493.88f, .5f},
                {440f, 1}, {440f, .5f}, {523.25f, .5f}, {659.25f, 1}, {587.33f, .5f}, {523.25f, .5f},
                {493.88f, 1.5f}, {523.25f, .5f}, {587.33f, 1}, {659.25f, 1}, {523.25f, 1}, {440f, 1}, {440f, 2},
                {0f, .5f}, {587.33f, 1}, {698.46f, .5f}, {880f, 1}, {783.99f, .5f}, {698.46f, .5f},
                {659.25f, 1.5f}, {523.25f, .5f}, {659.25f, 1}, {587.33f, .5f}, {523.25f, .5f},
                {493.88f, 1}, {493.88f, .5f}, {523.25f, .5f}, {587.33f, 1}, {659.25f, 1},
                {523.25f, 1}, {440f, 1}, {440f, 2}, {0f, 1}
        };
        private static final float BEAT_SEC = 0.32f;

        private AudioTrack track;
        private Thread thread;
        private volatile boolean playing;

        void start() {
            if (playing) return;
            playing = true;
            thread = new Thread(this);
            thread.start();
        }

        void stop() {
            playing = false;
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
            if (track != null) {
                track.stop();
                track.release();
                track = null;
            }
        }

        @Override
        public void run() {
            int minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(minBuf, 8192), AudioTrack.MODE_STREAM);
            track.play();
            int idx = 0;
            while (playing) {
                float[] note = MELODY[idx % MELODY.length];
                idx++;
                int samples = (int) (note[1] * BEAT_SEC * SAMPLE_RATE);
                short[] buf = new short[samples];
                if (note[0] > 0) {
                    int period = Math.round(SAMPLE_RATE / note[0]);
                    for (int i = 0; i < samples; i++) {
                        boolean on = (i % period) < (period / 2);
                        double env = 1.0 - (double) i / samples * 0.6;
                        buf[i] = (short) (on ? 2600 * env : -2600 * env);
                    }
                }
                if (track != null) track.write(buf, 0, samples);
            }
        }
    }
}
