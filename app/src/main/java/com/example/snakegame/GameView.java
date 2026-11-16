package com.example.snakegame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
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
    private final Paint paintBg = new Paint();
    private final Paint paintGrid = new Paint();
    private final Paint paintBorder = new Paint();
    private final Paint paintText = new Paint();
    private final Paint paintDim = new Paint();
    private final Paint paintGold = new Paint();
    private final Paint paintBlue = new Paint();
    private final Paint paintHat = new Paint();
    private final Paint paintStar = new Paint();
    private final Paint paintFace = new Paint();
    private final Paint paintHair = new Paint();
    private final Paint paintEyeWhite = new Paint();
    private final Paint paintPupil = new Paint();
    private final Paint paintMouth = new Paint();
    private final Paint paintBrow = new Paint();

    private Chiptune chiptune;
    private boolean specialFood = false;

    private float touchStartX, touchStartY;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);

        paintSnake.setColor(Color.rgb(76, 175, 80));
        paintBg.setColor(Color.rgb(18, 18, 22));
        paintGrid.setColor(Color.rgb(30, 30, 36));
        paintBorder.setColor(Color.rgb(74, 74, 90));
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(6f);
        paintDim.setColor(Color.argb(180, 0, 0, 0));
        paintText.setColor(Color.WHITE);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintGold.setColor(Color.rgb(255, 213, 79));
        paintGold.setStrokeCap(Paint.Cap.ROUND);
        paintBlue.setColor(Color.rgb(26, 63, 196));
        paintBlue.setStrokeCap(Paint.Cap.ROUND);
        paintHat.setColor(Color.rgb(107, 107, 120));
        paintStar.setColor(Color.rgb(211, 47, 47));
        paintFace.setColor(Color.rgb(247, 178, 106));
        paintHair.setColor(Color.rgb(245, 215, 110));
        paintEyeWhite.setColor(Color.WHITE);
        paintPupil.setColor(Color.rgb(26, 42, 90));
        paintMouth.setColor(Color.rgb(179, 84, 31));
        paintMouth.setStyle(Paint.Style.STROKE);
        paintMouth.setStrokeWidth(3f);
        paintBrow.setColor(Color.rgb(245, 215, 110));
        paintBrow.setStyle(Paint.Style.STROKE);
        paintBrow.setStrokeWidth(4f);
        paintBrow.setStrokeCap(Paint.Cap.ROUND);

        chiptune = new Chiptune();

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
        specialFood = random.nextInt(100) < 15;
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
            score += specialFood ? 3 : 1;
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

                drawHammerSickle(canvas, offsetX + food.x * cellSize + cellSize / 2f,
                        offsetY + food.y * cellSize + cellSize / 2f, cellSize / 2.3f);

                for (int i = snake.size() - 1; i >= 0; i--) {
                    Point p = snake.get(i);
                    if (i == 0) {
                        drawTrumpHead(canvas, offsetX + p.x * cellSize + cellSize / 2f,
                                offsetY + p.y * cellSize + cellSize / 2f, cellSize / 2.1f);
                    } else {
                        float pad = cellSize * 0.08f;
                        canvas.drawRoundRect(offsetX + p.x * cellSize + pad,
                                offsetY + p.y * cellSize + pad,
                                offsetX + (p.x + 1) * cellSize - pad,
                                offsetY + (p.y + 1) * cellSize - pad,
                                cellSize * 0.2f, cellSize * 0.2f, paintSnake);
                    }
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

    private void drawHammerSickle(Canvas canvas, float cx, float cy, float r) {
        Paint gold = specialFood ? paintStar : paintGold;
        Paint tool = specialFood ? paintBlue : paintGold;
        Paint strokeGold = new Paint(gold);
        strokeGold.setStyle(Paint.Style.STROKE);
        strokeGold.setStrokeWidth(r * 0.28f);
        Paint strokeTool = new Paint(tool);
        strokeTool.setStyle(Paint.Style.STROKE);
        strokeTool.setStrokeWidth(r * 0.28f);
        canvas.drawArc(cx - r * 0.8f, cy - r * 0.85f, cx + r * 0.6f, cy + r * 0.55f,
                -6f, 154f, false, strokeGold);
        canvas.drawLine(cx + r * 0.45f, cy + r * 0.4f, cx + r * 0.05f, cy + r * 0.85f, strokeGold);
        canvas.drawLine(cx - r * 0.7f, cy + r * 0.6f, cx - r * 0.1f, cy - r * 0.1f, strokeTool);
        canvas.save();
        canvas.translate(cx - r * 0.15f, cy - r * 0.25f);
        canvas.rotate(-45f);
        canvas.drawRect(-r * 0.5f, -r * 0.18f, r * 0.5f, r * 0.18f, tool);
        canvas.restore();
    }

    private void drawTrumpHead(Canvas canvas, float cx, float cy, float r) {
        canvas.drawCircle(cx, cy, r, paintFace);
        canvas.save();
        canvas.rotate(-8f, cx, cy);
        canvas.drawOval(cx - r * 1.05f, cy - r * 1.05f, cx + r * 1.05f, cy - r * 0.1f, paintHair);
        canvas.restore();
        canvas.drawOval(cx - r * 1.05f, cy - r * 0.95f, cx + r * 0.9f, cy - r * 0.35f, paintHair);
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.05f, r * 0.22f, paintEyeWhite);
        canvas.drawCircle(cx + r * 0.35f, cy - r * 0.05f, r * 0.22f, paintEyeWhite);
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.05f, r * 0.1f, paintPupil);
        canvas.drawCircle(cx + r * 0.35f, cy - r * 0.05f, r * 0.1f, paintPupil);
        canvas.drawLine(cx - r * 0.6f, cy - r * 0.45f, cx - r * 0.12f, cy - r * 0.3f, paintBrow);
        canvas.drawLine(cx + r * 0.12f, cy - r * 0.3f, cx + r * 0.6f, cy - r * 0.45f, paintBrow);
        canvas.drawArc(cx - r * 0.4f, cy + r * 0.1f, cx + r * 0.4f, cy + r * 0.7f,
                20f, 140f, false, paintMouth);
        canvas.drawRoundRect(cx - r * 1.05f, cy - r * 0.95f, cx + r * 1.05f, cy - r * 0.4f,
                r * 0.25f, r * 0.25f, paintHat);
        canvas.drawRect(cx - r * 1.05f, cy - r * 0.6f, cx - r * 0.75f, cy + r * 0.15f, paintHat);
        canvas.drawRect(cx + r * 0.75f, cy - r * 0.6f, cx + r * 1.05f, cy + r * 0.15f, paintHat);
        Path star = new Path();
        float scx = cx, scy = cy - r * 0.62f, R = r * 0.22f;
        for (int i = 0; i < 10; i++) {
            double ang = -Math.PI / 2 + i * Math.PI / 5;
            float rad = (i % 2 == 0) ? R : R * 0.45f;
            float px = (float) (scx + rad * Math.cos(ang));
            float py = (float) (scy + rad * Math.sin(ang));
            if (i == 0) star.moveTo(px, py); else star.lineTo(px, py);
        }
        star.close();
        canvas.drawPath(star, paintStar);
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
                {196f, .5f}, {261.63f, 1}, {261.63f, .5f}, {261.63f, .5f}, {293.66f, .5f}, {329.63f, 1},
                {349.23f, .75f}, {329.63f, .25f}, {293.66f, 1}, {261.63f, .75f}, {246.94f, .25f}, {261.63f, 1},
                {293.66f, 1}, {329.63f, 2}, {0f, .5f},
                {349.23f, .5f}, {349.23f, .5f}, {329.63f, .5f}, {293.66f, .5f}, {261.63f, 1},
                {293.66f, .5f}, {329.63f, .5f}, {349.23f, .5f}, {392f, 1},
                {329.63f, .5f}, {261.63f, .5f}, {293.66f, 2}, {0f, .5f},
                {196f, .5f}, {261.63f, 1}, {261.63f, .5f}, {261.63f, .5f}, {293.66f, .5f}, {329.63f, 1},
                {349.23f, .75f}, {329.63f, .25f}, {293.66f, 1}, {261.63f, .75f}, {246.94f, .25f}, {261.63f, 1},
                {261.63f, 2}, {0f, 1}
        };
        private static final float BEAT_SEC = 0.38f;

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
