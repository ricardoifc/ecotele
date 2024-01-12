package com.ricardoifc.ecotele;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class MainActivity extends Activity {
    private ExoPlayer player;
    private StyledPlayerView playerView;
    private List<Canal> canales;
    private int currentChannelIndex = 0;
    private Handler handler;
    private static final long CHANNEL_NAME_DISPLAY_DURATION = 4000;
    public interface ApiInterface {
        @GET("75dce7530c039accecab6ba6473777ea/raw")
        Call<List<Canal>> getCanales();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();
        // Inicializar el PlayerView desde el diseño XML
        playerView = findViewById(R.id.playerView);

        // Crear una instancia de ExoPlayer
        player = new SimpleExoPlayer.Builder(this).build();

        // Vincular el reproductor al PlayerView
        playerView.setPlayer(player);
        try {
            Field field = playerView.getClass().getDeclaredField("controller");
            field.setAccessible(true);
            StyledPlayerControlView controlView = (StyledPlayerControlView) field.get(playerView);
            controlView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_settings).setVisibility(View.GONE);
            controlView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_fullscreen).setVisibility(View.GONE);
            controlView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_subtitle).setVisibility(View.GONE);
            controlView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_progress).setVisibility(View.GONE);
            controlView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_position).setVisibility(View.GONE);
            View preparationView = controlView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_overlay);
            if (preparationView != null) {
                preparationView.setVisibility(View.GONE);
            }
           // controlView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_rew).setVisibility(View.GONE);
            //controlView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_ffwd).setVisibility(View.GONE);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace(); // Manejar la excepción según sea necesario
        }
        // Obtener la lista de canales desde el JSON a través de Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://gist.githubusercontent.com/ricardoifc/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiInterface apiInterface = retrofit.create(ApiInterface.class);
        Call<List<Canal>> call = apiInterface.getCanales();

        call.enqueue(new Callback<List<Canal>>() {
            @Override
            public void onResponse(Call<List<Canal>> call, Response<List<Canal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    canales = response.body();

                    // Reproducir el primer canal
                    playChannel(currentChannelIndex);
                } else {
                    // Manejar el caso de respuesta no exitosa
                }
            }

            @Override
            public void onFailure(Call<List<Canal>> call, Throwable t) {
                // Manejar el caso de error en la solicitud
            }
        });
    }

    private void playChannel(int channelIndex) {
        // Detener la reproducción actual
        player.stop();

        // Obtener el canal actual
        Canal canalActual = canales.get(channelIndex);

        // Crear una nueva fuente de medios para el nuevo canal
        MediaItem mediaItem = MediaItem.fromUri(canalActual.getUrl());
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);

        // Preparar el reproductor y comenzar la reproducción
        player.setMediaSource(hlsMediaSource);
        player.prepare();
        player.setPlayWhenReady(true);

        // Mostrar el nombre del canal en el TextView
        TextView channelNameTextView = findViewById(R.id.channelNameTextView);
        channelNameTextView.setText(canalActual.getNombre());

        // Asegurarnos de que el TextView esté visible
        channelNameTextView.setVisibility(View.VISIBLE);

        // Cancelar animaciones pendientes y restablecer la opacidad
        channelNameTextView.animate().cancel();
        channelNameTextView.setAlpha(1f);

        // Programar una tarea para desvanecer el TextView después de un tiempo
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Desvanecer el TextView
                channelNameTextView.animate()
                        .alpha(0f)
                        .setDuration(1000) // 1 segundo de duración del desvanecimiento
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                // Restablecer la opacidad del TextView después de que la animación haya terminado
                                channelNameTextView.setAlpha(1f);

                                // Ocultar el TextView después de restablecer la opacidad
                                channelNameTextView.setVisibility(View.INVISIBLE);
                            }
                        });
            }
        }, CHANNEL_NAME_DISPLAY_DURATION);
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Manejar eventos de teclas de flecha arriba y flecha abajo
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    // Cambiar al canal anterior
                    currentChannelIndex = (currentChannelIndex - 1 + canales.size()) % canales.size();
                    playChannel(currentChannelIndex);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    // Cambiar al canal siguiente
                    currentChannelIndex = (currentChannelIndex + 1) % canales.size();
                    playChannel(currentChannelIndex);
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
    @Override
    protected void onStop() {
        super.onStop();

            if (player != null) {
                player.release();
            }
            finish();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
        System.exit(0); // Forzar el cierre de la aplicación
    }


}
