package com.example.semana9;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    // Secciones
    private LinearLayout sectionVideo, sectionMusic, sectionImage, sectionAnim;

    // VIDEO
    private VideoView videoView;
    private Button btnPause;
    private boolean videoPrepared = false;

    // MÚSICA
    private MediaPlayer player;

    // IMAGEN
    private ImageView imgPreview;

    // ANIMACIÓN
    private ImageView imgAnim;
    private ObjectAnimator animLoop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        // Secciones
        sectionVideo = findViewById(R.id.sectionVideo);
        sectionMusic = findViewById(R.id.sectionMusic);
        sectionImage = findViewById(R.id.sectionImage);
        sectionAnim  = findViewById(R.id.sectionAnim);

        // Barra inferior
        Button bVideo = findViewById(R.id.btnVideo);
        Button bMusic = findViewById(R.id.btnMusic);
        Button bImage = findViewById(R.id.btnImage);
        Button bAnim  = findViewById(R.id.btnAnim);

        // ===== VIDEO (solo pausa/play) =====
        videoView = findViewById(R.id.videoView);
        btnPause  = findViewById(R.id.btnPause);
        setVideoSource();

        btnPause.setOnClickListener(v -> {
            if (!videoPrepared) return;
            if (videoView.isPlaying()) videoView.pause();
            else videoView.start();
        });

        // ===== MÚSICA =====
        Button btnPlay = findViewById(R.id.btnPlay);
        Button btnStop = findViewById(R.id.btnStop);
        Button btnRestart = findViewById(R.id.btnRestart);

        btnPlay.setOnClickListener(v -> {
            stopVideo(); // al reproducir música, apaga el video
            try {
                if (player == null) player = MediaPlayer.create(this, R.raw.musica);
                if (player != null && !player.isPlaying()) player.start();
            } catch (Exception e) {
                Toast.makeText(this, "Play error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        btnStop.setOnClickListener(v -> stopMusic());

        btnRestart.setOnClickListener(v -> {
            try {
                if (player != null) { player.seekTo(0); player.start(); }
                else { player = MediaPlayer.create(this, R.raw.musica); if (player != null) player.start(); }
            } catch (Exception e) {
                Toast.makeText(this, "Restart error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // ===== IMAGEN =====
        imgPreview = findViewById(R.id.imgPreview);
        Button btnDownload = findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(v -> saveImageLegacy());

        // ===== ANIMACIÓN =====
        imgAnim = findViewById(R.id.imgAnim);

        // Navegación por secciones
        bVideo.setOnClickListener(v -> {
            stopMusic();
            stopAnim();
            showSection("video");
        });
        bMusic.setOnClickListener(v -> {
            stopVideo();
            stopAnim();
            showSection("music");
        });
        bImage.setOnClickListener(v -> {
            stopVideo();
            stopMusic();
            stopAnim();
            showSection("image");
        });
        bAnim.setOnClickListener(v -> {
            stopVideo();
            stopMusic();
            showSection("anim");
            startAnim(); // inicia el bucle infinito
        });

        // Sección por defecto
        showSection("video");
    }

    /** VIDEO */
    private void setVideoSource() {
        btnPause.setEnabled(false);
        videoPrepared = false;

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video);
        videoView.setVideoURI(videoUri);
        videoView.requestFocus();

        videoView.setOnPreparedListener(mp -> {
            videoPrepared = true;
            btnPause.setEnabled(true);
            mp.setLooping(false);
        });

        videoView.setOnCompletionListener(mp -> {
            videoView.seekTo(0);
            videoView.pause();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, "No se pudo reproducir el video (" + what + "," + extra + ")", Toast.LENGTH_SHORT).show();
            // Repreparar por si queda en estado inválido
            setVideoSource();
            return true;
        });
    }

    private void stopVideo() {
        try {
            if (videoView != null) {
                if (videoView.isPlaying()) videoView.stopPlayback();
                setVideoSource();
            }
        } catch (Exception ignored) {}
    }

    /** MÚSICA */
    private void stopMusic() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
                player = null;
            }
        } catch (Exception ignored) {}
    }

    /** ANIMACIÓN: loop infinito suave */
    private void startAnim() {
        if (imgAnim == null) return;
        stopAnim(); // por si ya había uno corriendo

        animLoop = ObjectAnimator.ofFloat(imgAnim, "translationY", -20f, 20f);
        animLoop.setDuration(700);
        animLoop.setRepeatMode(ValueAnimator.REVERSE);
        animLoop.setRepeatCount(ValueAnimator.INFINITE);
        animLoop.start();
    }

    private void stopAnim() {
        if (animLoop != null) { animLoop.cancel(); animLoop = null; }
        if (imgAnim != null) { imgAnim.setTranslationY(0f); }
    }

    /** Mostrar solo una sección */
    private void showSection(String which) {
        sectionVideo.setVisibility(View.GONE);
        sectionMusic.setVisibility(View.GONE);
        sectionImage.setVisibility(View.GONE);
        sectionAnim.setVisibility(View.GONE);

        switch (which) {
            case "video": sectionVideo.setVisibility(View.VISIBLE); break;
            case "music": sectionMusic.setVisibility(View.VISIBLE); break;
            case "image": sectionImage.setVisibility(View.VISIBLE); break;
            case "anim":  sectionAnim.setVisibility(View.VISIBLE);  break;
        }
    }

    // ====== Permisos (Android 13+ y legacy) ======
    private void checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            boolean img = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
            boolean aud = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
            boolean vid = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    == PackageManager.PERMISSION_GRANTED;

            if (!img || !aud || !vid) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_AUDIO,
                                Manifest.permission.READ_MEDIA_VIDEO
                        },
                        PERMISSION_REQUEST_CODE
                );
            }
        } else {
            boolean read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
            boolean write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;

            if (!read || !write) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            Toast.makeText(this, granted ? "Permisos otorgados" : "Permisos denegados", Toast.LENGTH_SHORT).show();
        }
    }

    /** Guardar imagen (Android 8) */
    private void saveImageLegacy() {
        try {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.gato);
            String name = "gato_" + System.currentTimeMillis() + ".png";
            File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (!pictures.exists()) pictures.mkdirs();
            File file = new File(pictures, name);
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush(); fos.close();

            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, new String[]{"image/png"}, null);
            Toast.makeText(this, "Imagen guardada en Pictures", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Apaga todo al salir/minimizar
    @Override protected void onPause()  { super.onPause();  stopVideo(); stopMusic(); stopAnim(); }
    @Override protected void onStop()   { super.onStop();   stopVideo(); stopMusic(); stopAnim(); }
    @Override protected void onDestroy(){ super.onDestroy();stopVideo(); stopMusic(); stopAnim(); }
}
